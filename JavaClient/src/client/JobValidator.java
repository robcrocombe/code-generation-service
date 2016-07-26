package client;

import java.io.File;
import java.util.ArrayList;

import client.JsonObjects.AntProperty;
import client.JsonObjects.Property;

public class JobValidator
{
	public void validateProperties(ArrayList<AntProperty> genProperties,
			ArrayList<Property> userProperties) throws BadRequestException
	{
		if (userProperties.size() > genProperties.size())
		{
			throw new BadRequestException(
					"Too many properties declared to use this generator.");
		}

		// Ensure all user properties are valid generator properties
		// Better than checking equals as lists may be different sizes
		if (userProperties.retainAll(genProperties))
		{
			throw new BadRequestException(
					"Declared properties do not match those in the generator.");
		}

		// Now genProperties.size() >= userProperties.size()
		// So it can be looped through
		for (AntProperty genProperty : genProperties)
		{
			Property userProperty = findOne(userProperties, genProperty.name);

			if (userProperty == null)
			{
				if (genProperty.isRequired)
				{
					throw new BadRequestException("Required property \"" + genProperty.name + "\" not found.");
				}
				else continue;
			}
			else
			{
				if (genProperty.isFile)
				{
					if (!new File(userProperty.value).exists())
					{
						throw new BadRequestException("File \"" + userProperty.value + "\" not found.");
					}
				}
			}
		}
	}

	private Property findOne(ArrayList<Property> properties, String search)
	{
		for (Property property : properties)
		{
			if (property.name.equals(search))
			{
				return property;
			}
		}
		return null;
	}

	public static class BadRequestException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public BadRequestException(String msg)
		{
			super(msg);
		}
	}
}
