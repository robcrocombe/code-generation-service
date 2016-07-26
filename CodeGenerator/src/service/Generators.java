package service;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import generator.AntBuild;
import generator.AntBuild.Generator;
import generator.AntRunner;
import generator.FileManager;
import generator.GeneratorManager;
import generator.GitManager;

@Path("generators")
public class Generators
{
	// Get a list of all generators
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll()
	{
		return Response.ok(GeneratorManager.inst.getGenerators(),
				MediaType.APPLICATION_JSON).build();
	}

	// Get details of one generator
	@GET
	@Path("/{owner}/{repo}")
	@Produces({"application/json", "aplication/xml"})
	public Response getOne(@PathParam("owner") String owner, @PathParam("repo") String repo,
			@DefaultValue("false") @QueryParam("raw") Boolean raw)
	{
		AntBuild build = new AntBuild();
		try
		{
			boolean modified = GitManager.updateRepo(owner, repo);
			Generator generator = build.getInformation(owner, repo);
			if (modified)
			{
				GeneratorManager.inst.addGenerator(generator.name, generator.description);
			}

			if (raw)
			{
				String buildFile = build.getRawBuild(owner, repo);
				return Response.ok(buildFile, MediaType.APPLICATION_XML).build();
			}
			else
			{
				return Response.ok(generator, MediaType.APPLICATION_JSON).build();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new Error(e).build(404);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new Error(e).build(500);
		}
	}

	// Add a new generator
	@POST
	public Response post(@QueryParam("owner") String githubOwner, @QueryParam("repo") String repoName)
	{
		Generator generator = null;

		try
		{
			// Clone repo
			GitManager.cloneRepo(githubOwner, repoName);

			// Get generator metadata
			generator = new AntBuild().getInformation(githubOwner, repoName);

			// Check the build.xml is valid using initAnt()
			FileManager fileManager = new FileManager(githubOwner, repoName);
			AntRunner runner = new AntRunner(fileManager, null);
			runner.initAnt();
		}
		catch (InvalidRemoteException | TransportException e)
		{
			GitManager.deleteRepo(githubOwner, repoName);
			e.printStackTrace();
			return new Error(e).build(400);
		}
		catch (FileNotFoundException e)
		{
			GitManager.deleteRepo(githubOwner, repoName);
			e.printStackTrace();
			return new Error(e).build(404);
		}
		catch (Exception e)
		{
			GitManager.deleteRepo(githubOwner, repoName);
			e.printStackTrace();
			return new Error(e).build(500);
		}

		// Add generator to global list
		if (generator != null)
		{
			GeneratorManager.inst.addGenerator(generator.name, generator.description);
		}

		return Response.status(201).build();
	}
}
