package com.pg85.otg.generator.biome.layers;

import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.generator.biome.ArraysCache;

/**
 * Initializes river generation by randomly assigning river bit patterns.
 * Updated with 1.16.5 improvements: better documentation and clearer comments
 * while maintaining full 1.12.2 compatibility.
 * 
 * This layer assigns one of two river bit patterns to each cell, creating
 * the foundation for river region detection in LayerRiver.
 * 
 * Note: The coordinate inversion (zi + z, xi + x) is intentional and matches
 * the original 1.12.2 behavior. The 1.16.5 version has a TODO questioning this,
 * but we maintain it here for compatibility.
 */
public class LayerRiverInit extends Layer
{
    LayerRiverInit(long paramLong, int defaultOceanId, Layer paramGenLayer)
    {
        super(paramLong, defaultOceanId);
        this.child = paramGenLayer;
    }

    @Override
    public int[] getInts(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        int[] childInts = this.child.getInts(world, cache, x, z, xSize, zSize);
        int[] thisInts = cache.getArray(xSize * zSize);

        int currentPiece;
        
        for (int zi = 0; zi < zSize; zi++)
        {
            for (int xi = 0; xi < xSize; xi++)
            {
                // NOTE: Coordinates are reversed here (zi + z, xi + x)
                // This is the original 1.12.2 behavior. The 1.16.5 version questions
                // whether this is still needed, but we maintain it for compatibility.
                // If river patterns appear incorrect, this would be the first place to investigate.
                initChunkSeed(zi + z, xi + x);
                
                currentPiece = childInts[xi + zi * xSize];
                
                // Randomly assign one of two river bit patterns
                // This creates distinct "river regions" that will form boundaries
                // when processed by LayerRiver
                if (nextInt(2) == 0)
                {
                    currentPiece |= RiverBitOne;
                } else {
                    currentPiece |= RiverBitTwo;
                }

                thisInts[xi + zi * xSize] = currentPiece;
            }
        }
        return thisInts;
    }
}
