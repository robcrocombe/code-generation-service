package service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class Error
{
	public String error;

	public Error(Exception err)
	{
		this.error = err.getMessage();
	}

	public Error(String err)
	{
		this.error = err;
	}

	public Response build(Status status)
	{
		return Response.status(status)
				.entity(this)
				.type(MediaType.APPLICATION_JSON)
				.build();
	}

	public Response build(int status)
	{
		return Response.status(status)
				.entity(this)
				.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
