package com.pg85.otg.generator.biome.layers;

import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalBiome;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.world.WorldConfig;
import com.pg85.otg.generator.biome.ArraysCache;
import com.pg85.otg.logging.LogMarker;
import com.pg85.otg.network.ConfigProvider;

/**
 * Finalizes biome generation by mixing base biomes with river biomes.
 * Updated with 1.16.5 improvements: cached config values, extracted helper methods,
 * and cleaner logic while maintaining full 1.12.2 compatibility.
 */
public class LayerMixWithRiver extends Layer
{
    private int defaultFrozenOceanId;
    private ConfigProvider configs;
    private int[] riverBiomes;
    private Layer riverLayer;
    
    // Cached configuration values (1.16.5 improvement)
    private boolean riversEnabled;
    private boolean frozenOcean;
	
    LayerMixWithRiver(long seed, Layer childLayer, Layer riverLayer, ConfigProvider configs, LocalWorld world, int defaultOceanId, int defaultFrozenOceanId)
    {
        super(seed, defaultOceanId);
        this.defaultFrozenOceanId = defaultFrozenOceanId;
        this.child = childLayer;
        this.configs = configs;
        this.riverLayer = riverLayer;
        this.riverBiomes = new int[world.getMaxBiomesCount()];
        
        // Cache configuration values to avoid repeated lookups (1.16.5 improvement)
        WorldConfig worldConfig = configs.getWorldConfig();
        this.riversEnabled = worldConfig.riversEnabled;
        this.frozenOcean = worldConfig.frozenOcean;
        
        LocalBiome biome;
        LocalBiome riverBiome;
        
        // Initialize river biome mappings
        for (int id = 0; id < this.riverBiomes.length; id++)
        {
            biome = configs.getBiomeByOTGIdOrNull(id);

            this.riverBiomes[id] = -1;
            if (biome != null && !biome.getBiomeConfig().riverBiome.isEmpty())
            {
            	riverBiome = world.getBiomeByNameOrNull(biome.getBiomeConfig().riverBiome);
            	if(riverBiome != null)
            	{
            		this.riverBiomes[id] = riverBiome.getIds().getOTGBiomeId();
            	} else {
            		OTG.log(LogMarker.WARN, "River biome \"" + biome.getBiomeConfig().riverBiome + "\" for biome " + biome.getBiomeConfig().getName() + " could not be found.");
            	}
            }
        }
    }

    @Override
    public void initWorldGenSeed(long worldSeed)
    {
        super.initWorldGenSeed(worldSeed);
        riverLayer.initWorldGenSeed(worldSeed + 31337);
    }

    @Override
    public int[] getInts(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        switch (cache.outputType)
        {
            case FULL:
                return this.getFull(world, cache, x, z, xSize, zSize);
            case WITHOUT_RIVERS:
                return this.getWithoutRivers(world, cache, x, z, xSize, zSize);
            case ONLY_RIVERS:
                return this.getOnlyRivers(world, cache, x, z, xSize, zSize);
            default:
                throw new UnsupportedOperationException("Unknown/invalid output type: " + cache.outputType);
        }
    }

    /**
     * Full biome generation with rivers mixed in.
     * Improved with 1.16.5 patterns: extracted helper method, cached config lookups.
     */
    private int[] getFull(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        int[] childInts = this.child.getInts(world, cache, x, z, xSize, zSize);
        int[] riverInts = this.riverLayer.getInts(world, cache, x, z, xSize, zSize);
        int[] thisInts = cache.getArray(xSize * zSize);
        
        int sample;
        int currentRiver;
        int biomeId;
        int riverBiomeId;
        
        for (int zi = 0; zi < zSize; zi++)
        {
            for (int xi = 0; xi < xSize; xi++)
            {
                int index = xi + zi * xSize;
                sample = childInts[index];
                currentRiver = riverInts[index];
                
                // Extract base biome ID using helper method (1.16.5 pattern)
                biomeId = extractBiomeId(sample);

                // Apply river biome if rivers are enabled and present
                if (this.riversEnabled && (currentRiver & RiverBits) != 0)
                {
                    riverBiomeId = this.riverBiomes[biomeId];
                    if (riverBiomeId >= 0)
                    {
                        biomeId = riverBiomeId;
                    }
                }
                
                thisInts[index] = biomeId;
            }
        }
        return thisInts;
    }

    /**
     * Generate biomes without rivers (for visualization/debugging).
     */
    private int[] getWithoutRivers(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        int[] childInts = this.child.getInts(world, cache, x, z, xSize, zSize);
        int[] thisInts = cache.getArray(xSize * zSize);
        
        int sample;
        int biomeId;
        
        for (int zi = 0; zi < zSize; zi++)
        {
            for (int xi = 0; xi < xSize; xi++)
            {
                int index = xi + zi * xSize;
                sample = childInts[index];
                
                // Extract base biome without applying rivers
                biomeId = extractBiomeId(sample);
                
                thisInts[index] = biomeId;
            }
        }
        return thisInts;
    }

    /**
     * Generate only river indicators (0 or 1) for visualization.
     */
    private int[] getOnlyRivers(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        int[] childInts = this.child.getInts(world, cache, x, z, xSize, zSize);
        int[] riverInts = this.riverLayer.getInts(world, cache, x, z, xSize, zSize);
        int[] thisInts = cache.getArray(xSize * zSize);
       
        int sample;
        int currentRiver;
        int biomeId;
        LocalBiome biome;
        
        for (int zi = 0; zi < zSize; zi++)
        {
            for (int xi = 0; xi < xSize; xi++)
            {
                int index = xi + zi * xSize;
                sample = childInts[index];
                currentRiver = riverInts[index];

                // Extract base biome ID
                biomeId = extractBiomeId(sample);
                biome = this.configs.getBiomeByOTGIdOrNull(biomeId);
                
                // Check if this position should have a river
                if (this.riversEnabled && 
                    (currentRiver & RiverBits) != 0 && 
                    biome != null &&
                    !biome.getBiomeConfig().riverBiome.isEmpty())
                {
                    thisInts[index] = 1;
                } else {
                    thisInts[index] = 0;
                }
            }
        }
        return thisInts;
    }
    
    /**
     * Extract the base biome ID from a sample value, handling land/ocean/ice bits.
     * Helper method inspired by 1.16.5's cleaner separation of concerns.
     * 
     * @param sample The sample value containing biome and flag bits
     * @return The extracted biome ID
     */
    private int extractBiomeId(int sample)
    {
        // Check if this is land with a valid biome set
        if ((sample & LandBit) != 0)
        {
            if ((sample & BiomeBitsAreSetBit) != 0)
            {
                return sample & BiomeBits;
            } else {
                // Land bit is set but no biome specified - fallback to ocean
                return this.defaultOceanId;
            }
        }
        // Check for frozen ocean
        else if (this.frozenOcean && (sample & IceBit) != 0)
        {
            return this.defaultFrozenOceanId;
        }
        // Default to regular ocean
        else
        {
            return this.defaultOceanId;
        }
    }
}
