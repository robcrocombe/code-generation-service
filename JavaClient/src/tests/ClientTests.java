package tests;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.BeforeClass;
import org.junit.Test;

import client.GeneratorService;
import client.JobService;
import client.JsonObjects.AntProperty;
import client.JsonObjects.FileRequest;
import client.JsonObjects.GenAllResponse;
import client.JsonObjects.GenResponse;
import client.JsonObjects.Generator;
import client.JsonObjects.JobRequest;
import client.JsonObjects.Property;

public class ClientTests
{
	private static GeneratorService generatorService;
	private static JobService jobService;

	@BeforeClass
	public static void initialise()
	{
		Client client = ClientBuilder.newBuilder()
				.register(JacksonFeature.class)
				.register(MultiPartFeature.class)
				.build();

		client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);

		WebTarget target = client.target("http://localhost:8080/service/");

		generatorService = new GeneratorService(target);
		jobService = new JobService(target);
	}

	@Test
	public void testFileAdder() throws Exception
	{
		FormDataMultiPart multipart = new FormDataMultiPart();
		JobRequest jobRequest = new JobRequest();
		ArrayList<FileInputStream> fileStreams = new ArrayList<FileInputStream>();

		URI uploadFolderPath = this.getClass().getResource("files/").toURI();
		File directory = new File(uploadFolderPath);

		try
		{
			jobService.addAllFiles(directory, multipart, jobRequest, fileStreams);

			for (FileRequest f : jobRequest.files)
			{
				System.out.println(f.fieldName);
				System.out.println(f.filePath);
				System.out.println("----");
			}
		}
		finally
		{
			for (FileInputStream stream : fileStreams)
			{
				IOUtils.closeQuietly(stream);
			}
		}
	}

	@Test
	public void testGetGenerators()
	{
		GenAllResponse res = generatorService.getGenerators();

		if (res.status == 200)
		{
			for (Generator gen : res.generators)
			{
				out.println(gen.name);
				if (gen.description != null && !gen.description.isEmpty())
				{
					out.println(gen.description);
				}
				out.println();
			}
		}

		assertEquals("GET ALL /generators", 200, res.status);
	}

	@Test
	public void testGetGenerator()
	{
		GenResponse res = generatorService.getGenerator("robcrocombe", "cgs-java-gen");

		if (res.status == 200)
		{
			out.println("Name: "+ res.gen.name);
			out.println("Description: "+ res.gen.description);
			out.println("Properties:");

			for (AntProperty p : res.gen.properties)
			{
				out.println("   Name: " + p.name);
				out.println("   Description: " + p.description);
				if (p.value != null && !p.value.isEmpty()) out.println("   Default Value: " + p.value);
				out.println();
			}
		}

		assertEquals("GET /generators", 200, res.status);
	}

	@Test
	public void testPostGenerator()
	{
		int status = generatorService.postGenerator("robcrocombe", "cgs-java-gen");

		assertEquals("POST /generators", 201, status);
	}

	@Test
	public void testPostFile() throws Exception
	{
		String modelPath = this.getClass().getResource("files/test.xml").getPath();

		ArrayList<Property> properties = new ArrayList<Property>();
		properties.add(new Property("model", modelPath));

		GenResponse response = generatorService.getGenerator("robcrocombe", "cgs-java-gen");

		if (response.status != 200)
		{
			fail("GET /generator failed");
		}

		String jobID = jobService.postJob(response.gen.properties, properties, "robcrocombe", "cgs-java-gen");
		assertNotEquals("POST /job", null, jobID);
	}
}
