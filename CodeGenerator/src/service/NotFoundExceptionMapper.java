package service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException>
{
	@Override
	public Response toResponse(NotFoundException exception)
	{
		exception.printStackTrace();

		return new Error(exception).build(Status.NOT_FOUND);
	}
}
