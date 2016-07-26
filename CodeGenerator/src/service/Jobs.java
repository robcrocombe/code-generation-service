package service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.fasterxml.jackson.databind.ObjectMapper;

import exceptions.BadRequestException;
import exceptions.JobNotFoundException;
import generator.AntBuild;
import generator.AntBuild.Generator;
import generator.AntWrapper.FileRequest;
import generator.AntWrapper.JobRequest;
import generator.AntWrapper.Property;
import generator.FileManager;
import generator.GeneratorManager;
import generator.GitManager;
import generator.JobManager;
import generator.JobState;

@Path("job")
public class Jobs
{
	// Get the status of a current generation job
	@GET
	public Response get(@QueryParam("id") String id)
	{
		if (isNullOrEmpty(id))
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		else
		{
			try
			{
				switch (JobManager.inst.getJobState(id))
				{
				case Waiting:
					return Response.status(202).type(MediaType.TEXT_PLAIN)
							.entity("Job waiting in queue").build();
				case Running:
					return Response.status(202).type(MediaType.TEXT_PLAIN)
							.entity("Generator running").build();
				case Compressing:
					return Response.status(202).type(MediaType.TEXT_PLAIN)
							.entity("Compressing output").build();
				case Error:
					return new Error(JobManager.inst.getJobError(id)).build(500);
				case Cancelled:
					return Response.status(499).type(MediaType.TEXT_PLAIN)
							.entity("Job cancelled by user").build();
				case Success:
					File generatedZip = JobManager.inst.getGeneratedZip(id);

					return Response.ok(generatedZip)
							.type("application/zip")
							.header("Content-Disposition",
									"attachment; filename=\"" + generatedZip.getName() + "\"")
							.build();
				}
			}
			catch (JobNotFoundException e)
			{
				return new Error(e).build(Status.NOT_FOUND);
			}
		}
		return Response.status(500).type(MediaType.TEXT_PLAIN).build();
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response post(FormDataMultiPart form)
	{
		try
		{
			// Get JSON manifest, parse it to a Job Request
			FormDataBodyPart manifestEntity = form.getField("manifest");
			if (manifestEntity == null)
			{
				return new Error("The manifest JSON was not found in the request").build(Status.BAD_REQUEST);
			}

			ObjectMapper mapper = new ObjectMapper();
			JobRequest manifest = mapper.readValue(getEntityString(manifestEntity), JobRequest.class);

			// Initialise the job folder
			final String jobID = Identifier.inst.next();
			FileManager fileManager = new FileManager(jobID, manifest.githubOwner, manifest.repoName);
			fileManager.init();

			// Update generator information
			boolean modified = GitManager.updateRepo(manifest.githubOwner, manifest.repoName);
			AntBuild antBuild = new AntBuild();
			Generator generator = antBuild.getInformation(manifest.githubOwner, manifest.repoName);
			if (modified)
			{
				GeneratorManager.inst.addGenerator(generator.name, generator.description);
			}

			// Get files and properties from the request
			ArrayList<Property> properties = new ArrayList<Property>();
			if (manifest.properties != null) properties.addAll(manifest.properties);
			if (manifest.files == null || manifest.files.size() == 0)
			{
				return new Error("The request must include at least one file").build(Status.BAD_REQUEST);
			}

			for (FileRequest file : manifest.files)
			{
				FormDataBodyPart fileEntity = form.getField(file.fieldName);
				if (fileEntity == null)
				{
					return new Error("The file \"" + file.fieldName
							+ "\" was not found in the request").build(Status.BAD_REQUEST);
				}

				try (InputStream fileStream = getInputStream(fileEntity))
				{
					String modelPath = fileManager.uploadModel(fileStream, file.filePath);

					if (file.propertyName != null && !file.propertyName.isEmpty())
					{
						Property p = new Property();
						p.name = file.propertyName;
						p.value = modelPath;
						properties.add(p);
					}
				}
			}
			antBuild.validateProperties(generator.properties, properties);

			JobManager.inst.newJob(jobID, fileManager, properties);

			return Response.accepted().entity(jobID).build();
		}
		catch (BadRequestException | IOException e)
		{
			e.printStackTrace();
			return new Error(e).build(400);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new Error(e).build(500);
		}
	}

	// Delete a job request while it is processing
	@DELETE
	public Response delete(@QueryParam("id") String id)
	{
		if (isNullOrEmpty(id))
		{
			return Response.status(400).build();
		}
		else
		{
			try
			{
				if (JobManager.inst.getJobState(id) == JobState.Success)
				{
					return new Error("The job completed successfully and therefore was not cancelled").build(Status.CONFLICT);
				}
				else if (JobManager.inst.getJobState(id) == JobState.Cancelled)
				{
					return new Error("The job has already been cancelled").build(Status.NOT_MODIFIED);
				}
				else
				{
					JobManager.inst.cancelJob(id);
					return Response.ok().build();
				}
			}
			catch (JobNotFoundException e)
			{
				e.printStackTrace();
				return new Error(e).build(Status.NOT_FOUND);
			}
		}
	}

	private Boolean isNullOrEmpty(String param)
	{
		return (param == null || param.isEmpty());
	}

	private InputStream getInputStream(FormDataBodyPart formData)
	{
		if (formData.getEntity() instanceof InputStream)
		{
			return (InputStream)formData.getEntity();
		}
		else
		{
			return formData.getEntityAs(InputStream.class);
		}
	}

	private String getEntityString(FormDataBodyPart formData)
	{
		if (formData.getEntity() instanceof String)
		{
			return formData.getEntity().toString();
		}
		else
		{
			return formData.getEntityAs(String.class);
		}
	}
}
