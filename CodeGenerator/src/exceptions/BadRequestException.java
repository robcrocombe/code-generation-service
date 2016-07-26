package exceptions;

public class BadRequestException extends Exception
{
	private static final long serialVersionUID = 1L;
	// For JUnit testing
	public ErrorCode code;

	public BadRequestException(ErrorCode code, String msg)
	{
		super(msg);
		this.code = code;
	}

	public static enum ErrorCode
	{
		TOO_MANY,
		NO_MATCH,
		INVALID_PROPERTY,
		FILE_NOT_FOUND
	}
}
