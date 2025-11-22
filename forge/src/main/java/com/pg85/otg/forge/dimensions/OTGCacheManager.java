package com.pg85.otg.forge.dimensions;

import com.pg85.otg.OTG;
import com.pg85.otg.forge.events.dimensions.EntityTravelToDimensionListener;
import com.pg85.otg.logging.LogMarker;

/**
 * OPTIMIZATION: Central cache management for OTG dimension system
 * This class provides methods to clear and rebuild caches when dimensions change
 */
public class OTGCacheManager
{
	/**
	 * Clear all caches - call this when dimensions are added or removed
	 */
	public static void clearAllCaches()
	{
		OTG.log(LogMarker.DEBUG, "Clearing OTG dimension caches");
		EntityTravelToDimensionListener.clearPortalCache();
		// Add more cache clearing here as needed
	}
	
	/**
	 * Rebuild dimension registry cache - call this after loading dimensions
	 */
	public static void rebuildDimensionCache()
	{
		OTG.log(LogMarker.DEBUG, "Rebuilding OTG dimension cache");
		OTGDimensionManager.rebuildDimensionCache();
	}
	
	/**
	 * Full cache refresh - clear and rebuild everything
	 */
	public static void refreshAllCaches()
	{
		clearAllCaches();
		rebuildDimensionCache();
	}
}
