package client;

import static java.lang.System.out;

import java.util.ArrayList;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import client.JsonObjects.Error;
import client.JsonObjects.GenAllResponse;
import client.JsonObjects.GenResponse;
import client.JsonObjects.Generator;

public class GeneratorService
{
	private WebTarget target;

	public GeneratorService(WebTarget target)
	{
		this.target = target;
	}

	public GenAllResponse getGenerators()
	{
		WebTarget resource = target.path("generators");

		try
		{
			Response response = resource.request(MediaType.APPLICATION_JSON_TYPE).get();

			switch (response.getStatus())
			{
			case 200:
				ArrayList<Generator> generators = response.readEntity(
						new GenericType<ArrayList<Generator>>(){});
				return new GenAllResponse(response.getStatus(), generators);

			default:
				out.println("Failed: HTTP error " + response.getStatus());
				break;
			}

			return new GenAllResponse(response.getStatus(), null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new GenAllResponse(0, null);
		}
	}

	public GenResponse getGenerator(String owner, String repo)
	{
		WebTarget resource = target.path("generators/" + owner + "/" + repo);

		try
		{
			Response response = resource.request(MediaType.APPLICATION_JSON_TYPE).get();

			switch (response.getStatus())
			{
			case 200:
				Generator gen = response.readEntity(Generator.class);
				return new GenResponse(response.getStatus(), gen);

			case 404:
				out.println("Error: That generator has not been published.");
				break;

			default:
				out.println("Failed: HTTP error " + response.getStatus());
				break;
			}

			return new GenResponse(response.getStatus(), null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new GenResponse(0, null);
		}
	}

	public int postGenerator(String gitHubUser, String repoName)
	{
		WebTarget resource = target.path("generators")
				.queryParam("owner", gitHubUser)
				.queryParam("repo", repoName);

		try
		{
			Response response = resource.request().post(null);

			switch (response.getStatus())
			{
			case 201:
				out.println("Successfully published " + gitHubUser + "/" + repoName);
				break;

			case 400:
			case 500:
				error(response);
				break;

			default:
				out.println("Failed: HTTP error " + response.getStatus());
				break;
			}

			return response.getStatus();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}

	private void error(Response response)
	{
		Error error = response.readEntity(Error.class);
		out.print(error.message);
		out.println(".");
	}
}
