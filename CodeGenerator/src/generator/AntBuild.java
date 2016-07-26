package generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.robcrocombe.XMLParser;
import com.robcrocombe.XMLParser.Rule;
import com.robcrocombe.XMLParser.Type;

import exceptions.BadRequestException;
import exceptions.BadRequestException.ErrorCode;
import generator.AntWrapper.Property;

public class AntBuild
{
	public String getRawBuild(String gitHubUser, String repoName) throws IOException
	{
		File repoFolder = new File(FileManager.baseGenPath, gitHubUser + "~" + repoName);
		if (!repoFolder.exists())
		{
			throw new FileNotFoundException("Generator Not Found");
		}

		File buildFile = new File(repoFolder, "build.xml");
		if (buildFile.exists())
		{
			return FileUtils.readFileToString(buildFile);
		}
		else
		{
			throw new FileNotFoundException("Build.xml Not Found");
		}
	}

	public Generator getInformation(String gitHubUser, String repoName)
			throws FileNotFoundException, SAXException, Exception
	{
		final Generator generator = new Generator();
		generator.properties = new ArrayList<AntProperty>();
		generator.name = gitHubUser + "/" + repoName;

		File repoFolder = new File(FileManager.baseGenPath, gitHubUser + "~" + repoName);
		if (!repoFolder.exists())
		{
			throw new FileNotFoundException("Generator Not Found");
		}

		Rule descAttrRule = new Rule(Type.ELEMENT, 0, "project")
		{
			@Override
			public void handleElement(Node node, NamedNodeMap attr)
			{
				Node descNode = attr.getNamedItem("description");

				if (descNode != null)
				{
					generator.description = descNode.getNodeValue();
				}
			}
		};

		Rule descElemRule = new Rule(Type.CONTENT, 1, "description")
		{
			@Override
			public void handleContent(Node node, String value)
			{
				if (value != null && !value.isEmpty() && generator.description == null)
				{
					generator.description = value;
				}
			}
		};

		Rule propertyRule = new Rule(Type.COMMENT, 1)
		{
			@Override
			public void handleComment(Node node, String comment)
			{
				AntProperty p = new AntProperty();

				if (contains(comment, "#file"))
				{
					p.isFile = true;
				}
				else if (contains(comment, "#string"))
				{
					p.isFile = false;
				}
				else
				{
					// Comment does not have property annotations
					return;
				}

				if (contains(comment, "#optional"))
				{
					p.isRequired = false;
				}
				else
				{
					p.isRequired = true;
				}

				Node property = node.getNextSibling();
				while (property.getNodeType() != Node.ELEMENT_NODE)
				{
					property = property.getNextSibling();
				}

				if (!property.getNodeName().toLowerCase().equals("property"))
				{
					// No property found after comment
					return;
				}

				p.name = XMLParser.getAttributeValue(property, "name");
				p.value = XMLParser.getAttributeValue(property, "value");
				p.description = XMLParser.getAttributeValue(property, "description");
				generator.properties.add(p);
			}
		};

		try (InputStream buildStream = new FileInputStream(new File(repoFolder, "build.xml")))
		{
			XMLParser parser = new XMLParser(propertyRule, descAttrRule, descElemRule);
			parser.parse(buildStream);
			return generator;
		}
	}

	public void validateProperties(ArrayList<AntProperty> genProperties,
			ArrayList<Property> userProperties) throws BadRequestException
	{
		if (userProperties.size() > genProperties.size())
		{
			throw new BadRequestException(ErrorCode.TOO_MANY,
					"Too many properties declared to use this generator");
		}

		// Ensure all user properties are valid generator properties
		// Better than checking equals as lists may be different sizes
		if (userProperties.retainAll(genProperties))
		{
			throw new BadRequestException(ErrorCode.INVALID_PROPERTY,
					"Declared properties do not match those in the generator");
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
					throw new BadRequestException(ErrorCode.NO_MATCH, "Required property \"" +
							genProperty.name + "\" not found");
				}
				else continue;
			}
			else
			{
				if (genProperty.isFile)
				{
					if (!new File(userProperty.value).exists())
					{
						throw new BadRequestException(ErrorCode.FILE_NOT_FOUND, "File \"" +
								userProperty.value + "\" not found");
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

	// Assumes phrase is lower case
	private boolean contains(String string, String phrase)
	{
		return string.toLowerCase().contains(phrase);
	}

	public static class Generator
	{
		public String name;
		public String description;
		public ArrayList<AntProperty> properties;
	}

	public static class AntProperty extends Property
	{
		public String description;
		public Boolean isFile;
		public Boolean isRequired;
	}
}
