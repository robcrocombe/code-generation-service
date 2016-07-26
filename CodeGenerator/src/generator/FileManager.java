package generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import exceptions.NotFoundException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class FileManager
{
	public static final String baseOutputPath = System.getenv("output_path");
	public static final String baseGenPath = System.getenv("gen_path");
	public final String gitHubUser;
	public final String repoName;
	public File jobFolder;
	public File repoFolder;

	public FileManager(String jobID, String gitHubUser, String repoName) throws FileNotFoundException
	{
		this.gitHubUser = gitHubUser;
		this.repoName = repoName;
		this.jobFolder = new File(baseOutputPath, jobID);
		this.repoFolder = new File(baseGenPath, gitHubUser + "~" + repoName);

		if (!repoFolder.exists())
		{
			throw new FileNotFoundException("Base path is empty");
		}
	}

	public FileManager(String gitHubUser, String repoName) throws FileNotFoundException
	{
		this.gitHubUser = gitHubUser;
		this.repoName = repoName;
		this.repoFolder = new File(baseGenPath, gitHubUser + "~" + repoName);

		if (!repoFolder.exists())
		{
			throw new FileNotFoundException("Base path is empty");
		}
	}

	public void init() throws IOException, NotFoundException
	{
		if (!repoFolder.exists())
		{
			throw new NotFoundException("No published generator found");
		}

		if (jobFolder.exists())
		{
			FileUtils.forceDelete(jobFolder);
		}
		FileUtils.forceMkdir(jobFolder);
	}

	public String uploadModel(InputStream uploadStream, String fileName) throws IOException
	{
		File outputFile = new File(jobFolder, fileName);
		outputFile.getParentFile().mkdirs();
		saveFile(uploadStream, outputFile);
		return outputFile.getAbsolutePath();
	}

	private void saveFile(InputStream uploadInputStream, File outputFile) throws IOException
	{
		try (OutputStream outputStream = new FileOutputStream(outputFile))
		{
			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = uploadInputStream.read(bytes)) != -1)
			{
				outputStream.write(bytes, 0, read);
			}
			outputStream.flush();
		}
	}

	public void saveAntOutput(String output) throws IOException
	{
		if (!output.equals(""))
		{
			File outputFile = new File(jobFolder, "gen/ant-output.txt");
			FileUtils.writeStringToFile(outputFile, output);
		}
	}

	public void removeJobFiles()
	{
		try
		{
			FileUtils.forceDelete(jobFolder);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public File compress() throws FileNotFoundException, ZipException
	{
		File generatedZip = new File(jobFolder, repoName + jobFolder.getName() + ".zip");
		File genFolder = new File(jobFolder, "gen");

		if (genFolder.exists())
		{
			ZipFile zipFile = new ZipFile(generatedZip);
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			parameters.setIncludeRootFolder(false);

			zipFile.addFolder(genFolder, parameters);
			return generatedZip;
		}
		else
		{
			throw new FileNotFoundException("The generated folder was not found");
		}
	}

	public String uncompress(File zip) throws ZipException
	{
		File outputFolder = new File(jobFolder, FilenameUtils.getBaseName(zip.getName()));
		outputFolder.mkdir();
		ZipFile zipFile = new ZipFile(zip);
		zipFile.extractAll(outputFolder.getAbsolutePath());
		zip.delete();

		return outputFolder.getAbsolutePath();
	}
}
