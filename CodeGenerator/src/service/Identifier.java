package service;

import java.util.concurrent.atomic.AtomicInteger;

public class Identifier
{
	public static final Identifier inst = new Identifier();
	private AtomicInteger sourceID = new AtomicInteger();

	private Identifier()
	{
		sourceID.set(0);
	}

	public String next()
	{
		return Integer.toString(sourceID.incrementAndGet());
	}
}
