package generator;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map.Entry;

import generator.JobManager.Job;

public class JobMonitor implements Runnable
{
	@Override
	public synchronized void run()
	{
		Calendar now = Calendar.getInstance();
		Iterator<?> it = JobManager.inst.threads.entrySet().iterator();

		while (it.hasNext())
		{
			Entry<?, ?> pair = (Entry<?, ?>)it.next();
			Job job = (Job)pair.getValue();

			if (job.expiryDate() != null)
			{
				// If expire time is older than the current time
				// then remove the job
				if (job.expiryDate().compareTo(now) < 0)
				{
					job.removeJobFiles();
					it.remove();
				}
			}
		}
	}
}
