package client;

import static java.lang.System.out;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import client.JsonObjects.AntProperty;
import client.JsonObjects.Error;
import client.JsonObjects.FileRequest;
import client.JsonObjects.JobRequest;
import client.JsonObjects.Property;

public class JobService
{
	private WebTarget target;
	private int fileID;

	public JobService(WebTarget target)
	{
		this.target = target;
	}

	public Response pollJob(String id)
	{
		return target.path("job").queryParam("id", id).request().get();
	}

	public Response cancelJob(String id)
	{
		return target.path("job").queryParam("id", id).request().delete();
	}

	public String postJob(ArrayList<AntProperty> genProperties, ArrayList<Property> userProperties,
			String gitHubUser, String repoName)
	{
		FormDataMultiPart multipart = new FormDataMultiPart();
		JobRequest jobRequest = new JobRequest();
		jobRequest.githubOwner = gitHubUser;
		jobRequest.repoName = repoName;
		fileID = 0;

		// Keep track of file streams so that they can be properly closed
		ArrayList<FileInputStream> fileStreams = new ArrayList<FileInputStream>();

		try
		{
			for (int i = 0; i < userProperties.size(); ++i)
			{
				File propertyFile = new File(userProperties.get(i).value);
				if (propertyFile.exists())
				{
					if (propertyFile.isDirectory())
					{
						addAllFiles(propertyFile, multipart, jobRequest, fileStreams);

						jobRequest.properties.add(new Property(userProperties.get(i).name, propertyFile.getName()));
					}
					else
					{
						String fieldName = nextID();

						jobRequest.files.add(new FileRequest(fieldName,
								userProperties.get(i).name, propertyFile.getName()));

						FileInputStream stream = new FileInputStream(propertyFile);
						fileStreams.add(stream);
						multipart.field(fieldName, stream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
					}
				}
				else
				{
					jobRequest.properties.add(userProperties.get(i));
				}
			}

			multipart.field("manifest", jobRequest, MediaType.APPLICATION_JSON_TYPE);
			Response response = target.path("job").request(MediaType.TEXT_PLAIN_TYPE)
					.post(Entity.entity(multipart, multipart.getMediaType()));

			switch (response.getStatus())
			{
			case 202:
				return response.readEntity(String.class);

			case 400:
			case 500:
				error(response);
				break;

			default:
				out.println("Failed: HTTP error " + response.getStatus());
				break;
			}

			return null;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			for (FileInputStream stream : fileStreams)
			{
				IOUtils.closeQuietly(stream);
			}
		}
	}

	public void error(Response response)
	{
		Error error = response.readEntity(Error.class);
		out.print(error.message);
		out.println(".");
	}

	public void addAllFiles(File directory, FormDataMultiPart multipart,
			JobRequest request, ArrayList<FileInputStream> fileStreams) throws FileNotFoundException
	{
		Iterator<File> it = FileUtils.iterateFiles(directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

		while (it.hasNext())
		{
			File file = it.next();
			String fieldName = nextID();

			request.files.add(new FileRequest(fieldName, null,
					relativePath(directory.getParent(), file.getAbsolutePath())));

			FileInputStream stream = new FileInputStream(file);
			fileStreams.add(stream);
			multipart.field(fieldName, stream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
		}
	}

	private String relativePath(String basePath, String filePath)
	{
		return Paths.get(basePath).relativize(Paths.get(filePath)).toString();
	}

	private String nextID()
	{
		return "file" + fileID++;
	}
}
