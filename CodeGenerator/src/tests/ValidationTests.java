package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

import exceptions.BadRequestException;
import exceptions.BadRequestException.ErrorCode;
import generator.AntBuild;
import generator.AntBuild.AntProperty;
import generator.AntWrapper.Property;

public class ValidationTests
{
	/**
	 * Test that having too many user properties returns
	 * the correct exception.
	 */
	@Test
	public void tooManyPropertiesTest()
	{
		ArrayList<AntProperty> genProperties = new ArrayList<AntProperty>();
		ArrayList<Property> userProperties = new ArrayList<Property>();

		genProperties.add(new AntProperty()
		{{
			name = "n1";
			value = "v1";
			description = "d1";
			isFile = false;
			isRequired = true;
		}});

		// 2 user properties > 1 generator property
		userProperties.add(new Property()
		{{
			name = "n1";
			value = "v1";
		}});
		userProperties.add(new Property()
		{{
			name = "n2";
			value = "v2";
		}});

		AntBuild antBuild = new AntBuild();
		try
		{
			antBuild.validateProperties(genProperties, userProperties);
		}
		catch (BadRequestException e)
		{
			assertEquals(ErrorCode.TOO_MANY, e.code);
			return;
		}
		fail();
	}

	/**
	 * Test that the user cannot give properties that are not
	 * valid generator properties.
	 */
	@Test
	public void invalidPropertiesTest()
	{
		ArrayList<AntProperty> genProperties = new ArrayList<AntProperty>();
		ArrayList<Property> userProperties = new ArrayList<Property>();

		genProperties.add(new AntProperty()
		{{
			name = "n1";
			value = "v1";
			description = "d1";
			isFile = false;
			isRequired = true;
		}});
		genProperties.add(new AntProperty()
		{{
			name = "n2";
			value = "v2";
			description = "d2";
			isFile = false;
			isRequired = true;
		}});

		userProperties.add(new Property()
		{{
			name = "n1";
			value = "v1";
		}});
		userProperties.add(new Property()
		{{
			name = "n3"; // Invalid property
			value = "v3";
		}});

		AntBuild antBuild = new AntBuild();
		try
		{
			antBuild.validateProperties(genProperties, userProperties);
		}
		catch (BadRequestException e)
		{
			assertEquals(ErrorCode.INVALID_PROPERTY, e.code);
			return;
		}
		fail();
	}

	/**
	 * Test that missing required properties are caught.
	 */
	@Test
	public void missingPropertiesTest()
	{
		ArrayList<AntProperty> genProperties = new ArrayList<AntProperty>();
		ArrayList<Property> userProperties = new ArrayList<Property>();

		genProperties.add(new AntProperty()
		{{
			name = "n1";
			value = "v1";
			description = "d1";
			isFile = false;
			isRequired = true;
		}});
		genProperties.add(new AntProperty()
		{{
			name = "n2";
			value = "v2";
			description = "d2";
			isFile = false;
			isRequired = false;
		}});

		// Missing required n1
		userProperties.add(new Property()
		{{
			name = "n2";
			value = "v2";
		}});

		AntBuild antBuild = new AntBuild();
		try
		{
			antBuild.validateProperties(genProperties, userProperties);
		}
		catch (BadRequestException e)
		{
			assertEquals(ErrorCode.NO_MATCH, e.code);
			return;
		}
		fail();
	}

	/**
	 * Test that properties declared as files are valid.
	 */
	@Test
	public void invalidFileTest()
	{
		ArrayList<AntProperty> genProperties = new ArrayList<AntProperty>();
		ArrayList<Property> userProperties = new ArrayList<Property>();

		genProperties.add(new AntProperty()
		{{
			name = "n1";
			value = "v1";
			description = "d1";
			isFile = true;
			isRequired = true;
		}});

		userProperties.add(new Property()
		{{
			name = "n1";
			value = "v1"; // Not a file path
		}});

		AntBuild antBuild = new AntBuild();
		try
		{
			antBuild.validateProperties(genProperties, userProperties);
		}
		catch (BadRequestException e)
		{
			assertEquals(ErrorCode.FILE_NOT_FOUND, e.code);
			return;
		}
		fail();
	}
}
