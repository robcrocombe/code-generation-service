package generator;

import java.util.ArrayList;

public class AntWrapper
{
	public static class JobRequest
	{
		public String githubOwner;
		public String repoName;
		public ArrayList<FileRequest> files;
		public ArrayList<Property> properties;
	}

	public static class Property
	{
		public String name;
		public String value;

		@Override
		public boolean equals(Object obj)
		{
			if (obj != null)
			{
				return name.equals(((Property)obj).name);
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
}
