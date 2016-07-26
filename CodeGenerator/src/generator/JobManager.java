package generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import exceptions.JobNotFoundException;
import generator.AntWrapper.Property;

public class JobManager
{
	public static final JobManager inst = new JobManager();

	public ConcurrentHashMap<String, Job> threads;
	private ThreadPoolExecutor executor;

	private JobManager()
	{
		executor = new ThreadPoolExecutor(10, 10,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		threads = new ConcurrentHashMap<String, Job>();
	}

	public void newJob(String jobID, FileManager fileManager, ArrayList<Property> properties) throws IOException
	{
		Job job = new Job(fileManager, properties);
		job.run(executor);
		threads.put(jobID, job);
	}

	public JobState getJobState(String id) throws JobNotFoundException
	{
		if (threads.containsKey(id))
		{
			return threads.get(id).getState();
		}
		else
		{
			throw new JobNotFoundException();
		}
	}

	public File getGeneratedZip(String id) throws JobNotFoundException
	{
		if (threads.containsKey(id))
		{
			return threads.get(id).getGeneratedZip();
		}
		else
		{
			throw new JobNotFoundException();
		}
	}

	public String getJobError(String id) throws JobNotFoundException
	{
		if (threads.containsKey(id))
		{
			return threads.get(id).getError();
		}
		else
		{
			throw new JobNotFoundException();
		}
	}

	public Boolean cancelJob(String id) throws JobNotFoundException
	{
		if (threads.containsKey(id))
		{
			return threads.get(id).cancel();
		}
		else
		{
			throw new JobNotFoundException();
		}
	}

	public void cancelAllJobs()
	{
		executor.shutdown();
	}

	public Boolean jobIsDone(String id) throws JobNotFoundException
	{
		if (threads.containsKey(id))
		{
			return threads.get(id).isDone();
		}
		else
		{
			throw new JobNotFoundException();
		}
	}

	public class Job
	{
		private Future<?> future;
		private AntRunner antRunner;

		public Job(FileManager fileManager, ArrayList<Property> properties) throws IOException
		{
			antRunner = new AntRunner(fileManager, properties);
		}

		public void run(ThreadPoolExecutor executor)
		{
			future = executor.submit(antRunner);
		}

		public JobState getState()
		{
			return antRunner.state;
		}

		public File getGeneratedZip()
		{
			return antRunner.generatedZip;
		}

		public String getError()
		{
			return antRunner.error;
		}

		public Boolean cancel()
		{
			return future.cancel(true);
		}

		public Boolean isDone()
		{
			return future.isDone();
		}

		public Calendar expiryDate()
		{
			return antRunner.expiryDate;
		}

		public void removeJobFiles()
		{
			antRunner.removeJobFiles();
		}
	}
}
