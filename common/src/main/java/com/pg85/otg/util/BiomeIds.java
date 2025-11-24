package com.pg85.otg.util;

/**
 * Immutable class to hold the biome ids of a biome.
 * <p>
 * Most biomes have just one id: it is used during terrain generation and it
 * is used to save to the map files. Some biomes have two ids: one used during
 * generation, one is saved to the map files. The id used during generation
 * has to be unique, the one saved to the map files doesn't have to be unique.
 */
public final class BiomeIds // Made final as described as immutable
{
    private int otgBiomeId;
    private int savedId;
    private final boolean isVirtual; // Made final - shouldn't change

    /**
     * Creates a new biome id.
     *
     * @param otgBiomeId The id used during terrain generation.
     * @param savedId The id used in the world save files (the .mca files in
     *            the region directory).
     * @param isVirtual Whether this biome is virtual.
     */
    public BiomeIds(int otgBiomeId, int savedId, boolean isVirtual)
    {
        this.otgBiomeId = otgBiomeId;
        this.savedId = savedId;
        this.isVirtual = isVirtual;
    }

    /**
     * Gets whether this biome is virtual. A biome is virtual if the id used
     * during terrain generation isn't the same as the id used in the world
     * save files.
     *
     * @return True if the biome is virtual, false otherwise.
     */
    public boolean isVirtual()
    {
        return this.isVirtual;
    }

    /**
     * Gets the id that is saved to the world save files.
     *
     * @return The id.
     */
    public int getSavedId()
    {
        return savedId;
    }

    /**
     * Gets the id used during terrain generation.
     *
     * @return The id.
     */
    public int getOTGBiomeId()
    {
        return otgBiomeId;
    }

    public void setSavedId(int value)
    {
        savedId = value;
    }

    public void setOTGBiomeId(int value)
    {
        otgBiomeId = value;
    }

    @Override
    public String toString()
    {
        if (isVirtual())
        {
            return otgBiomeId + " (otg), " + savedId + " (saved)";
        } else
        {
            return Integer.toString(savedId);
        }
    }

    @Override
    public int hashCode()
    {
        // OPTIMIZATION: Use Objects.hash or better algorithm
        int result = 17;
        result = 31 * result + savedId;
        result = 31 * result + otgBiomeId;
        result = 31 * result + (isVirtual ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof BiomeIds)) // Combined null check
        {
            return false;
        }
        BiomeIds other = (BiomeIds) obj;
        return savedId == other.savedId && 
               otgBiomeId == other.otgBiomeId &&
               isVirtual == other.isVirtual; // Added missing field
    }
}
