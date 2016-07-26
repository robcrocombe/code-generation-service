package service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import generator.JobManager;
import generator.JobMonitor;

public class InitialisationListener implements ServletContextListener
{
	private final ScheduledExecutorService scheduler;

	public InitialisationListener()
	{
		scheduler = Executors.newScheduledThreadPool(1);
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0)
	{
		scheduler.scheduleAtFixedRate(new JobMonitor(), 30, 60, TimeUnit.MINUTES);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0)
	{
		scheduler.shutdown();
		JobManager.inst.cancelAllJobs();
	}
}
