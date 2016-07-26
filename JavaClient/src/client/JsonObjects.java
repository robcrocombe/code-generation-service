package client;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class JsonObjects
{
	public static class JobRequest
	{
		public String githubOwner;
		public String repoName;
		public ArrayList<FileRequest> files = new ArrayList<FileRequest>();
		public ArrayList<Property> properties = new ArrayList<Property>();
	}

	public static class Property
	{
		public String name;
		public String value;

		public Property()
		{ }

		public Property(String name, String value)
		{
			this.name = name;
			this.value = value;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj != null)
			{
				if (obj instanceof AntProperty)
				{
					return name.equals(((AntProperty)obj).name);
				}
				else if (obj instanceof Property)
				{
					return name.equals(((Property)obj).name);
				}
			}
			return false;
		}
	}

	public static class FileRequest
	{
		public String fieldName;
		public String propertyName;
		public String filePath;

		public FileRequest()
		{ }

		public FileRequest(String fieldName, String propertyName, String filePath)
		{
			this.fieldName = fieldName;
			this.propertyName = propertyName;
			this.filePath = filePath;
		}
	}

	public static class Generator
	{
		public String name;
		public String description;
		public ArrayList<AntProperty> properties;
	}

	public static class AntProperty
	{
		public String name;
		public String value;
		public String description;
		public Boolean isFile;
		public Boolean isRequired;

		@Override
		public boolean equals(Object obj)
		{
			if (obj != null)
			{
				if (obj instanceof Property)
				{
					return name.equals(((Property)obj).name);
				}
				else if (obj instanceof AntProperty)
				{
					return name.equals(((AntProperty)obj).name);
				}
			}
			return false;
		}
	}

	public static class GenResponse
	{
		public int status;
		public Generator gen;

		public GenResponse()
		{ }

		public GenResponse(int status, Generator gen)
		{
			this.status = status;
			this.gen = gen;
		}
	}

	public static class GenAllResponse
	{
		public int status;
		public ArrayList<Generator> generators;

		public GenAllResponse()
		{ }

		public GenAllResponse(int status, ArrayList<Generator> generators)
		{
			this.status = status;
			this.generators = generators;
		}
	}

	@XmlRootElement
	public static class Error
	{
		@XmlElement(name="error")
		public String message;
	}
}