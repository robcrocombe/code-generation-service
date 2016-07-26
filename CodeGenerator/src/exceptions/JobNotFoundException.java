package exceptions;

public class JobNotFoundException extends Exception
{
	private static final long serialVersionUID = 1L;

	public JobNotFoundException()
	{
		super("A job with that ID was not found");
	}
}
