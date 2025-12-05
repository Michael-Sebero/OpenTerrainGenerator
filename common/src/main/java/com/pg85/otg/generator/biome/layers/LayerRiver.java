package com.pg85.otg.generator.biome.layers;

import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.generator.biome.ArraysCache;

/**
 * Generates river patterns by checking neighboring cells for boundary detection.
 * Updated with 1.16.5 improvements: clearer variable naming and better documentation
 * while maintaining full 1.12.2 compatibility.
 * 
 * Rivers are created at boundaries between different river regions or where
 * river bits are zero (edge detection).
 */
public class LayerRiver extends Layer
{
    LayerRiver(long seed, int defaultOceanId, Layer childLayer)
    {
        super(seed, defaultOceanId);
        this.child = childLayer;
    }

    @Override
    public int[] getInts(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        // Request expanded area to sample neighbors (cross pattern)
        int x0 = x - 1;
        int z0 = z - 1;
        int xSize0 = xSize + 2;
        int zSize0 = zSize + 2;
        int[] childInts = this.child.getInts(world, cache, x0, z0, xSize0, zSize0);
        int[] thisInts = cache.getArray(xSize * zSize);

        // Variables for cross-pattern sampling (1.16.5 clarity improvement)
        int northCheck;
        int southCheck;
        int eastCheck;
        int westCheck;
        int centerCheck;
        int currentPiece;
        
        for (int zi = 0; zi < zSize; zi++)
        {
            for (int xi = 0; xi < xSize; xi++)
            {
                // Calculate indices for north, south, east, west, and center samples
                // The +1 offset accounts for the expanded sampling area
                int northIdx = xi + 1 + (zi) * xSize0;
                int southIdx = xi + 1 + (zi + 2) * xSize0;
                int eastIdx = xi + 2 + (zi + 1) * xSize0;
                int westIdx = xi + 0 + (zi + 1) * xSize0;
                int centerIdx = xi + 1 + (zi + 1) * xSize0;
                
                // Extract river bits from each direction
                northCheck = childInts[northIdx] & RiverBits;
                southCheck = childInts[southIdx] & RiverBits;
                eastCheck = childInts[eastIdx] & RiverBits;
                westCheck = childInts[westIdx] & RiverBits;
                centerCheck = childInts[centerIdx] & RiverBits;
                
                currentPiece = childInts[centerIdx];
                
                // River detection algorithm:
                // 1. If any neighbor has river bits = 0, create a river (edge detection)
                if ((centerCheck == 0) || 
                    (westCheck == 0) || 
                    (eastCheck == 0) || 
                    (northCheck == 0) || 
                    (southCheck == 0))
                {
                    currentPiece |= RiverBits;
                }
                // 2. If center differs from any neighbor, create a river (boundary detection)
                else if ((centerCheck != westCheck) || 
                         (centerCheck != northCheck) || 
                         (centerCheck != eastCheck) || 
                         (centerCheck != southCheck))
                {
                    currentPiece |= RiverBits;
                } 
                // 3. Otherwise, clear river bits (interior of uniform region)
                else 
                {
                    // Set then XOR to clear the bits
                    currentPiece |= RiverBits;
                    currentPiece ^= RiverBits;
                }
                
                thisInts[xi + zi * xSize] = currentPiece;
            }
        }

        return thisInts;
    }
}
