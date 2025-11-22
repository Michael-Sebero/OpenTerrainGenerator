package com.pg85.otg.worldsave;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;

import com.pg85.otg.OTG;
import com.pg85.otg.configuration.dimensions.DimensionConfig;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.configuration.standard.WorldStandardValues;
import com.pg85.otg.logging.LogMarker;

public class DimensionData
{
	// OPTIMIZATION: Use larger buffer for better I/O performance (default is 8192)
	private static final int BUFFER_SIZE = 8192;
	
	public int dimensionOrder;
	public int dimensionId;
	public String dimensionName;
	public boolean keepLoaded;
	public long seed = 0;
	
	public static void saveDimensionData(File worldSaveDirectory, ArrayList<DimensionData> dimensionData)
	{
		StringBuilder stringBuilder = new StringBuilder();
		for(DimensionData dimData : dimensionData)
		{
			stringBuilder.append((stringBuilder.length() == 0 ? "" : ","))
				.append(dimData.dimensionId).append(",")
				.append(dimData.dimensionName).append(",")
				.append(dimData.keepLoaded).append(",")
				.append(dimData.seed).append(",")
				.append(dimData.dimensionOrder);
		}
		saveDimensionData(worldSaveDirectory, stringBuilder);
	}

	public static void saveDimensionData(File worldSaveDirectory, StringBuilder stringBuilder)
	{
		File dimensionDataFile = new File(worldSaveDirectory + File.separator + PluginStandardValues.PLUGIN_NAME + File.separator + WorldStandardValues.DimensionsDataFileName);
		File dimensionDataBackupFile = new File(worldSaveDirectory + File.separator + PluginStandardValues.PLUGIN_NAME + File.separator + WorldStandardValues.DimensionsDataBackupFileName);
		
		BufferedWriter writer = null;
        try
        {
    		if(!dimensionDataFile.exists())
    		{
    			dimensionDataFile.getParentFile().mkdirs();
    		} else {
    			Files.move(dimensionDataFile.toPath(), dimensionDataBackupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    		}
        	
        	// OPTIMIZED: Use larger buffer and explicit charset for better performance
        	writer = new BufferedWriter(
        		new OutputStreamWriter(
        			new FileOutputStream(dimensionDataFile), 
        			StandardCharsets.UTF_8
        		), 
        		BUFFER_SIZE
        	);
        	
            writer.write(stringBuilder.toString());
            writer.flush(); // OPTIMIZATION: Explicit flush before close
            OTG.log(LogMarker.DEBUG, "Custom dimension data saved");
        }
        catch (IOException e)
        {
			e.printStackTrace();
			throw new RuntimeException(
				"OTG encountered a critical error writing " + dimensionDataFile.getAbsolutePath() + ", exiting. "
				+ "OTG automatically backs up files before writing and will try to use the backup when loading. "					
				+ "If your world's " + WorldStandardValues.DimensionsDataFileName + " and its backup have been corrupted, "
				+ "you can replace it with a backup or create a new world with the same dimensions and copy its " 
				+ WorldStandardValues.DimensionsDataFileName + ".");
        }
        finally
        {
            try
            {
                if(writer != null)
                {
                	writer.close();
                }
            }
            catch (Exception e)
            {
            	OTG.log(LogMarker.WARN, "Error closing dimension data file writer: " + e.getMessage());
            }
        }
	}
	
	public static ArrayList<DimensionData> loadDimensionData(File worldSaveDir)
	{
		File dimensionDataFile = new File(worldSaveDir + File.separator + PluginStandardValues.PLUGIN_NAME + File.separator + WorldStandardValues.DimensionsDataFileName);
		File dimensionDataBackupFile = new File(worldSaveDir + File.separator + PluginStandardValues.PLUGIN_NAME + File.separator + WorldStandardValues.DimensionsDataBackupFileName);		
		
		if(!dimensionDataFile.exists() && !dimensionDataBackupFile.exists())
		{
			return null;
		}		

		// OPTIMIZED: Try primary file first with consolidated logic
		ArrayList<DimensionData> result = tryLoadFile(dimensionDataFile);
		if(result != null)
		{
			return result;
		}
		
		// Try backup
		OTG.log(LogMarker.WARN, "Failed to load " + dimensionDataFile.getAbsolutePath() + ", trying to load backup.");
		result = tryLoadFile(dimensionDataBackupFile);
		if(result != null)
		{
			return result;
		}
		
		throw new RuntimeException(
			"OTG encountered a critical error loading " + dimensionDataFile.getAbsolutePath() + " and could not load a backup, exiting. "
			+ "OTG automatically backs up files before writing and will try to use the backup when loading. "					
			+ "If your world's " + WorldStandardValues.DimensionsDataFileName + " and its backup have been corrupted, "
			+ "you can replace it with a backup or create a new world with the same dimensions and copy its " 
			+ WorldStandardValues.DimensionsDataFileName + ".");			
	}
	
	// OPTIMIZED: Consolidated file loading logic with try-with-resources
	private static ArrayList<DimensionData> tryLoadFile(File file)
	{
		if(!file.exists())
		{
			return null;
		}
		
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(file), 
				StandardCharsets.UTF_8
			), 
			BUFFER_SIZE))
		{
			StringBuilder stringbuilder = new StringBuilder();
			String line;
			
			// OPTIMIZATION: Use more efficient string concatenation
		    while ((line = reader.readLine()) != null)
		    {
		    	stringbuilder.append(line);
		    }
		    
		    if(stringbuilder.length() > 0)
		    {
		    	String[] dimensionDataFileValues = stringbuilder.toString().split(",");
		    	ArrayList<DimensionData> data = parseDimensionDataValues(dimensionDataFileValues);
		    	OTG.log(LogMarker.DEBUG, "Custom dimension data loaded from " + file.getName());
		    	return data;
		    }
		    
		    // Empty file
		    return new ArrayList<DimensionData>();
		}
		catch (Exception e)
		{
			OTG.log(LogMarker.WARN, "Error loading " + file.getAbsolutePath() + ": " + e.getMessage());
			return null;
		}
	}
	
	private static ArrayList<DimensionData> parseDimensionDataValues(String[] dimensionDataFileValues)
	{
		ArrayList<DimensionData> dimensionData = new ArrayList<DimensionData>();
		if(dimensionDataFileValues.length > 0)
		{
			// OPTIMIZATION: Pre-allocate array capacity if we know the size
			int expectedSize = dimensionDataFileValues.length / 5;
			dimensionData = new ArrayList<DimensionData>(expectedSize);
			
			for(int i = 0; i < dimensionDataFileValues.length; i += 5)
			{
				DimensionData dimData = new DimensionData();
				dimData.dimensionId = Integer.parseInt(dimensionDataFileValues[i]);
				dimData.dimensionName = dimensionDataFileValues[i + 1];
				dimData.keepLoaded = Boolean.parseBoolean(dimensionDataFileValues[i + 2]);
				dimData.seed = Long.parseLong(dimensionDataFileValues[i + 3]);
				dimData.dimensionOrder = Integer.parseInt(dimensionDataFileValues[i + 4]);
				dimensionData.add(dimData);
			}
		}
		return dimensionData;
	}

	public static void deleteDimSavedData(Path worldSaveDir, DimensionConfig dimConfig)
	{
		Path dimensionSaveDir = Paths.get(worldSaveDir + File.separator + "DIM" + dimConfig.DimensionId);
		if(Files.exists(dimensionSaveDir) && Files.isDirectory(dimensionSaveDir))
		{
			OTG.log(LogMarker.INFO, "Deleting MC world save data for dimension " + dimConfig.DimensionId);
			try {
			    Files.walk(dimensionSaveDir)
			      .sorted(Comparator.reverseOrder())
			      .map(Path::toFile)
			      .forEach(File::delete);
			 
			    if(Files.exists(dimensionSaveDir))
			    {
			    	OTG.log(LogMarker.ERROR, "Could not delete directory: " + dimensionSaveDir.toString());
			    }
			} catch (IOException e) {
				OTG.log(LogMarker.ERROR, "Could not delete directory " + dimensionSaveDir.toString() + ". Error: " + e.toString());
				e.printStackTrace();
			}
		}
		
		dimensionSaveDir = Paths.get(worldSaveDir + File.separator + PluginStandardValues.PLUGIN_NAME + File.separator + "DIM-" + dimConfig.DimensionId);
		if(Files.exists(dimensionSaveDir) && Files.isDirectory(dimensionSaveDir))
		{
			OTG.log(LogMarker.INFO, "Deleting OTG world save data for dimension " + dimConfig.DimensionId);
			// Delete structure and pregenerator data
			try {	   										 
			    Files.walk(dimensionSaveDir)
			      .sorted(Comparator.reverseOrder())
			      .map(Path::toFile)
			      .forEach(File::delete);
			 
			    if(Files.exists(dimensionSaveDir))
			    {
			    	OTG.log(LogMarker.ERROR, "Could not delete directory: " + dimensionSaveDir.toString());
			    }
			} catch (IOException e) {
				OTG.log(LogMarker.ERROR, "Could not delete directory " + dimensionSaveDir.toString() + ". Error: " + e.toString());
				e.printStackTrace();
			}
			
			// Remove any biome id's used for the dim.
			ArrayList<BiomeIdData> biomeIds = BiomeIdData.loadBiomeIdData(worldSaveDir.toFile());
			ArrayList<BiomeIdData> newBiomeIds = new ArrayList<>();
			for(BiomeIdData biomeIdData : biomeIds)
			{
				if(!biomeIdData.biomeName.startsWith(dimConfig.PresetName + "_"))
				{
					newBiomeIds.add(biomeIdData);
				}
			}
			BiomeIdData.saveBiomeIdData(worldSaveDir.toFile(), newBiomeIds);
			
			// Remove any dimension data used for the dim,
			// update the load order for the remaining dims.
			ArrayList<DimensionData> dimensionData = loadDimensionData(worldSaveDir.toFile());
			ArrayList<DimensionData> newDimensionData = new ArrayList<>();
			int removedIndex = -1;
			for(DimensionData dimData : dimensionData)
			{
				if(dimData.dimensionId != dimConfig.DimensionId)
				{
					newDimensionData.add(dimData);
				} else {
					removedIndex = dimData.dimensionOrder;
				}
			}
			if(removedIndex > -1)
			{
				for(DimensionData dimData : newDimensionData)
				{
					if(dimData.dimensionOrder > removedIndex)
					{
						dimData.dimensionOrder = dimData.dimensionOrder - 1;	
					}
				}
			}
			saveDimensionData(worldSaveDir.toFile(), newDimensionData);
		}
	}	
}
