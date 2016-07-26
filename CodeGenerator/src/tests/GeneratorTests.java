package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import generator.AntWrapper.FileRequest;
import generator.AntWrapper.JobRequest;
import generator.AntWrapper.Property;
import service.Generators;
import service.Jobs;

public class GeneratorTests
{
	@Test
	public void publishEMFTest() throws Exception
	{
		Generators genService = new Generators();
		Response response = genService.post("robcrocombe", "pacs-emf-gen");

		if (response.getEntity() != null)
		{
			System.out.println(response.getEntity().toString());
		}
		assertEquals(201, response.getStatus());
	}
	
	@Test
	public void jobWithEmfTest() throws Exception
	{
		URI testFilePath = this.getClass().getResource("test.model").toURI();
		File generatedZip = simulateJobPost(testFilePath, "robcrocombe", "pacs-emf-gen");

		assertTrue(generatedZip.exists());
	}

	private File simulateJobPost(URI modelFilePath,
			String githubUser, String repoName) throws Exception
	{
		Jobs jobService = new Jobs();
		File modelFile = new File(modelFilePath);

		try (InputStream modelStream = new FileInputStream(modelFile))
		{
			String relativeFile = relativePath(modelFile.getParent(), modelFilePath);

			JobRequest jobRequest = new JobRequest();
			jobRequest.githubOwner = githubUser;
			jobRequest.repoName = repoName;
			jobRequest.files = new ArrayList<FileRequest>();
			jobRequest.files.add(new FileRequest("file", "model", relativeFile));

			jobRequest.properties = new ArrayList<Property>();
			jobRequest.properties.add(new Property()
			{{
				name = "mediaOnly";
				value = "false";
			}});

			ObjectMapper mapper = new ObjectMapper();
			String jsonString = mapper.writeValueAsString(jobRequest);

			FormDataMultiPart multipart = new FormDataMultiPart();
			multipart.field("file", modelStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
			multipart.field("manifest", jsonString, MediaType.APPLICATION_JSON_TYPE);

			Response POSTresponse = jobService.post(multipart);

			String id = POSTresponse.getEntity().toString();
			Boolean processing = true;
			String generatedZip = "null";

			while (processing)
			{
				Response GETresponse = jobService.get(id);
				switch (GETresponse.getStatus())
				{
				case 202:
					System.out.println(GETresponse.getEntity().toString());
					break;
				case 400:
					fail("400 Bad Request");
					break;
				case 404:
					fail("404 Job Not Found");
					break;
				case 499:
					System.out.println(GETresponse.getEntity().toString());
					processing = false;
					continue;
				case 500:
					fail(GETresponse.getEntity().toString());
					break;
				case 200:
					generatedZip = GETresponse.getEntity().toString();
					processing = false;
					continue;
				}

				Thread.sleep(500);
			}

			System.out.println(generatedZip);
			return new File(generatedZip);
		}
	}

	private String relativePath(String basePath, URI filePath)
	{
		return Paths.get(basePath).relativize(Paths.get(filePath)).toString();
	}
}
