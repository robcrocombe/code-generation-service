package client;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import client.JobValidator.BadRequestException;
import client.JsonObjects.AntProperty;
import client.JsonObjects.GenAllResponse;
import client.JsonObjects.GenResponse;
import client.JsonObjects.Generator;
import client.JsonObjects.Property;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class Main
{
	private GeneratorService generatorService;
	private JobService jobService;
	private BufferedReader in;
	private String[] args;

	private File outputFolder = null;
	private boolean unzip = false;
	private int pollRate = 500;

	public static void main(String[] args)
	{
		new Main().start(args);
	}

	public void initialise()
	{
		String host = "http://localhost:8080/service/";

		// Set options
		ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		for (String arg : args)
		{
			// Ignore case
			String option = arg.toLowerCase();

			if (option.startsWith("-output="))
			{
				outputFolder = new File(getPairValue(arg));
				argsList.remove(arg);
			}
			else if (option.startsWith("-host="))
			{
				host = getPairValue(arg);
				argsList.remove(arg);
			}
			else if (option.startsWith("-pollrate="))
			{
				try
				{
					pollRate = Integer.parseInt(getPairValue(arg));
					argsList.remove(arg);
				}
				catch (Exception e)
				{
					out.println("Poll rate is not a valid integer.");
					System.exit(0);
				}
			}
			else if (option.equals("-unzip"))
			{
				unzip = true;
				argsList.remove(arg);
			}
		}

		// Reassign args if options were removed in previous loop
		if (argsList.size() != args.length)
		{
			args = argsList.toArray(new String[argsList.size()]);
		}

		Client client = ClientBuilder.newBuilder()
				.register(JacksonFeature.class)
				.register(MultiPartFeature.class)
				.build();

		client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);

		WebTarget target = client.target(host);

		generatorService = new GeneratorService(target);
		jobService = new JobService(target);

		in = new BufferedReader(new InputStreamReader(System.in));
	}

	public void start(String[] argsIn)
	{
		try
		{
			args = argsIn;
			initialise();

			if (args.length == 0)
			{
				out.println("No command specified.");
				System.exit(0);
			}

			switch (args[0])
			{
			case "list":
				getGenerators();
				break;

			case "view":
				viewGenerator();
				break;

			case "publish":
				publishGenerator();
				break;

			case "run":
				newJob();
				break;

			default:
				out.println("There is no function with the name \"" + args[0] + "\".");
				break;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String getPairValue(String arg)
	{
		String[] pair = arg.split("=");
		if (pair.length != 2)
		{
			out.println("Parameter \"" + arg + "\" is invalid. It must be in the format \"name=value\".");
			System.exit(0);
			return null;
		}
		else return pair[1];
	}

	private void getGenerators()
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
	}

	private void viewGenerator()
	{
		if (args.length < 2)
		{
			out.println("Please give a GitHub repository in the form 'owner/repo'.");
			System.exit(0);
		}

		String[] repo = args[1].split("/");
		if (repo.length != 2)
		{
			out.println("Invalid GitHub repository. Please format as 'owner/repo'.");
			System.exit(0);
		}

		GenResponse res = generatorService.getGenerator(repo[0], repo[1]);

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
	}

	private void publishGenerator()
	{
		if (args.length < 2)
		{
			out.println("Please give a GitHub repository in the form 'owner/repo'.");
			System.exit(0);
		}

		String[] repo = args[1].split("/");
		if (repo.length != 2)
		{
			out.println("Invalid GitHub repository. Please format as 'owner/repo'.");
			System.exit(0);
		}

		generatorService.postGenerator(repo[0], repo[1]);
	}

	private void newJob() throws IOException, InterruptedException
	{
		if (args.length < 2)
		{
			out.println("Please give a GitHub repository in the form 'owner/repo'.");
			System.exit(0);
		}

		String[] repo = args[1].split("/");
		if (repo.length != 2)
		{
			out.println("Invalid GitHub repository. Please format as 'owner/repo'.");
			System.exit(0);
		}

		GenResponse genRes = generatorService.getGenerator(repo[0], repo[1]);
		if (genRes.status != 200)
		{
			return;
		}

		ArrayList<String> params = new ArrayList<String>(Arrays.asList(args));
		ArrayList<Property> properties = new ArrayList<Property>();

		// Remove run and generator arguments
		params.remove(0);
		params.remove(0);
		if (params.size() == 0)
		{
			out.println("No generator parameters specified.");
			System.exit(0);
		}

		for (String param : params)
		{
			String[] pair = param.split("=");
			if (pair.length != 2)
			{
				out.println("Parameter \"" + param + "\" is invalid. It must be in the format \"name=value\".");
				return;
			}
			properties.add(new Property(pair[0], pair[1]));
		}

		try
		{
			new JobValidator().validateProperties(genRes.gen.properties, properties);
		}
		catch (BadRequestException e)
		{
			out.println(e.getMessage());
			return;
		}

		out.println("Submitting new job...");
		String jobID = jobService.postJob(genRes.gen.properties, properties, repo[0], repo[1]);

		if (jobID != null)
		{
			out.println("Running, press Q to cancel...");
			Thread jobRunner = new Thread(new JobRunner(jobID));
			jobRunner.start();

			while (true)
			{
				String stop = in.readLine();
				if (stop != null && stop.equalsIgnoreCase("q"))
				{
					cancelJob(jobID);
					break;
				}
			}
		}
	}

	private void cancelJob(String jobID)
	{
		Response res = jobService.cancelJob(jobID);

		switch (res.getStatus())
		{
		// Success
		case 200:
			return;

			// Error
		case 304:
		case 404:
		case 409:
			jobService.error(res);
			return;

			// Unknown
		default:
			String message = res.readEntity(String.class);
			out.print(message);
			out.println(".");
			return;
		}
	}

	private class JobRunner implements Runnable
	{
		private String jobID;
		private String prevStatus = "";

		public JobRunner(String jobID)
		{
			this.jobID = jobID;
		}

		@Override
		public void run()
		{
			try
			{
				while (true)
				{
					Response res = jobService.pollJob(jobID);

					switch (res.getStatus())
					{
					// Processing
					case 202:
						String progress = res.readEntity(String.class);
						if (!progress.equals(prevStatus))
						{
							out.print(progress);
							out.println("...");
						}
						prevStatus = progress;
						break;
						// Error
					case 500:
						jobService.error(res);
						System.exit(0);
						return;
						// Success
					case 200:
						downloadOutput(res);
						System.exit(0);
						return;
						// Cancelled or an unknown error
					default:
						String message = res.readEntity(String.class);
						out.print(message);
						out.println(".");
						System.exit(0);
						return;
					}

					Thread.sleep(pollRate);
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		private void downloadOutput(Response res)
		{
			File zip = null;

			try
			{
				InputStream input = (InputStream)res.getEntity();
				byte[] byteArray = IOUtils.toByteArray(input);

				if (outputFolder != null)
				{
					if (outputFolder.isDirectory())
					{
						zip = new File(outputFolder, jobID + ".zip");
					}
					else
					{
						out.println("Output parameter is not a directory. Save to current directory instead? (Y/N)");
						while (true)
						{
							String choice = in.readLine();
							if (choice.equalsIgnoreCase("y"))
							{
								outputFolder = new File(Paths.get(".").toAbsolutePath().normalize().toString());
								zip = new File(outputFolder, jobID + ".zip");
								break;
							}
							else if (choice.equalsIgnoreCase("n"))
							{
								out.println("Job cancelled.");
								System.exit(0);
							}
						}
					}
				}
				else
				{
					outputFolder = new File(Paths.get(".").toAbsolutePath().normalize().toString());
					zip = new File(outputFolder, jobID + ".zip");
				}

				try (FileOutputStream fos = new FileOutputStream(zip))
				{
					fos.write(byteArray);
					fos.flush();
					out.println("Created " + zip.getPath());
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(0);
			}

			if (unzip && zip != null)
			{
				try
				{
					out.println("Extracting zip file...");
					ZipFile zipFile = new ZipFile(zip);
					zipFile.extractAll(outputFolder.getAbsolutePath());
				}
				catch (ZipException e)
				{
					out.println("Failed to unzip the generated output.");
					e.printStackTrace();
				}
			}
		}
	}
}
