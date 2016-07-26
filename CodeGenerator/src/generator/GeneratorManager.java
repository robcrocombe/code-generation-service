package generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class GeneratorManager
{
	public static final GeneratorManager inst = new GeneratorManager();

	private ArrayList<GenOverview> generators;
	private File genJSON;

	private GeneratorManager()
	{
		genJSON = new File(FileManager.baseGenPath, "generators.json");

		if (genJSON.exists())
		{
			try
			{
				ObjectMapper mapper = new ObjectMapper();
				generators = mapper.readValue(genJSON, TypeFactory.defaultInstance()
						.constructCollectionType(ArrayList.class, GenOverview.class));
			}
			catch (IOException e)
			{
				e.printStackTrace();
				// Probably shouldn't continue operation
				// until error is fixed
				System.exit(1);
			}
		}
		else
		{
			generators = new ArrayList<GenOverview>();
		}
	}

	public ArrayList<GenOverview> getGenerators()
	{
		return generators;
	}

	public void addGenerator(String name, String description)
	{
		// Check for existing generator
		for (GenOverview gen : generators)
		{
			if (gen.name.equals(name))
			{
				// Update existing generator
				gen.description = description;
				writeToFile();
				return;
			}
		}

		// Else generator not in list
		GenOverview gen = new GenOverview();
		gen.name = name;
		gen.description = description;

		generators.add(gen);
		writeToFile();
	}

	private synchronized void writeToFile()
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(genJSON, generators);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static class GenOverview
	{
		public String name;
		public String description;
	}
}
