package generator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

public class GitManager
{
	// Private repository token setter
	// .setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", ""));

	public static void cloneRepo(String gitHubUser, String repoName) throws InvalidRemoteException, TransportException, GitAPIException, IOException
	{
		Git clone = null;
		File cloneFolder = null;

		try
		{
			String gitHubURL = "https://github.com/" + gitHubUser + "/" + repoName + ".git";
			cloneFolder = new File(FileManager.baseGenPath, gitHubUser + "~" + repoName);
			if (cloneFolder.exists())
			{
				FileUtils.cleanDirectory(cloneFolder);
				FileUtils.forceDelete(cloneFolder);
			}

			clone = Git.cloneRepository()
					.setURI(gitHubURL)
					.setDirectory(cloneFolder)
					.call();
		}
		catch (Exception e)
		{
			if (cloneFolder != null)
			{
				FileUtils.forceDelete(cloneFolder);
			}
			throw e;
		}
		finally
		{
			if (clone != null) clone.getRepository().close();
		}
	}

	public static boolean updateRepo(String gitHubUser, String repoName) throws GitAPIException, IOException
	{
		Git repo = null;

		try
		{
			File repoFolder = new File(FileManager.baseGenPath, gitHubUser + "~" + repoName);
			repo = Git.open(repoFolder);
			MergeResult result = repo.pull().call().getMergeResult();

			// Return true if files have changed
			return result.getMergeStatus() != MergeStatus.ALREADY_UP_TO_DATE;
		}
		finally
		{
			if (repo != null) repo.getRepository().close();
		}
	}

	public static void deleteRepo(String gitHubUser, String repoName)
	{
		try
		{
			File repoFolder = new File(FileManager.baseGenPath, gitHubUser + "~" + repoName);
			if (repoFolder.exists())
			{
				FileUtils.forceDelete(repoFolder);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
