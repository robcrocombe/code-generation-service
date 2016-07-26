package generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import generator.AntWrapper.Property;
import net.lingala.zip4j.exception.ZipException;

public class AntRunner implements Runnable
{
	public JobState state = JobState.Waiting;
	public FileManager fileManager;
	public ArrayList<Property> properties;
	public String error;
	public Calendar expiryDate = null;
	public File generatedZip;

	private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	private ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
	private Project project;
	private ProjectHelper helper;

	public AntRunner(FileManager fileManager, ArrayList<Property> properties) throws IOException
	{
		this.fileManager = fileManager;
		this.properties = properties;
	}

	@Override
	public void run()
	{
		try
		{
			if (Thread.interrupted())
			{
				state = JobState.Cancelled;
				return;
			}
			state = JobState.Running;
			initAnt();
			if (Thread.interrupted())
			{
				state = JobState.Cancelled;
				return;
			}
			runAntBuild();
			if (Thread.interrupted())
			{
				state = JobState.Cancelled;
				return;
			}
			fileManager.saveAntOutput(outputStream.toString());
			if (Thread.interrupted())
			{
				state = JobState.Cancelled;
				return;
			}
			state = JobState.Compressing;
			generatedZip = fileManager.compress();
			state = JobState.Success;
		}
		catch (IOException | BuildException | ZipException e)
		{
			e.printStackTrace();
			//if (!errorStream.toString().equals(""))
			//{
			//	System.out.println(errorStream.toString());
			//}
			if (e.getMessage() == null)
			{
				if (!errorStream.toString().equals(""))
				{
					error = errorStream.toString();
				}
			}
			else
			{
				error = e.getMessage();
			}
			state = JobState.Error;
		}
		finally
		{
			// Set expiration 24 hours after job completed
			expiryDate = Calendar.getInstance();
			expiryDate.add(Calendar.DATE, 1);
		}
	}

	private void runAntBuild() throws BuildException
	{
		try
		{
			project.setBaseDir(fileManager.repoFolder);
			project.setProperty("outputRoot", new File(fileManager.jobFolder, "gen").getAbsolutePath());

			for (Property p : properties)
			{
				project.setProperty(p.name, p.value);
			}

			project.fireBuildStarted();
			project.executeTarget(project.getDefaultTarget());
			project.fireBuildFinished(null);
			System.out.println(outputStream.toString());
		}
		catch (BuildException e)
		{
			project.fireBuildFinished(e);
			throw e;
		}
	}

	public void initAnt() throws BuildException, FileNotFoundException
	{
		PrintStream printOut;
		PrintStream printErr;

		try
		{
			printOut = new PrintStream(outputStream, true, "utf-8");
			printErr = new PrintStream(errorStream, true, "utf-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			printOut = new PrintStream(outputStream);
			printErr = new PrintStream(errorStream);
		}

		BuildLogger logger = new DefaultLogger();
		logger.setOutputPrintStream(printOut);
		logger.setErrorPrintStream(printErr);
		logger.setMessageOutputLevel(Project.MSG_INFO);

		project = new Project();
		project.addBuildListener(logger);
		project.init();

		setAntTaskDefinitions();

		helper = ProjectHelper.getProjectHelper();
		project.addReference("ant.projectHelper", helper);

		File buildFile = new File(fileManager.repoFolder, "build.xml");
		if (!buildFile.exists())
		{
			throw new FileNotFoundException("File Not Found: " + buildFile.getPath());
		}
		project.setProperty("ant.file", buildFile.getAbsolutePath());

		helper.parse(project, buildFile);
	}

	private void setAntTaskDefinitions()
	{
		for (int i = 0; i < TaskDefinitions.names.length; ++i)
		{
			project.addTaskDefinition(TaskDefinitions.names[i], TaskDefinitions.classes[i]);
		}
	}

	public void removeJobFiles()
	{
		fileManager.removeJobFiles();
	}
}
