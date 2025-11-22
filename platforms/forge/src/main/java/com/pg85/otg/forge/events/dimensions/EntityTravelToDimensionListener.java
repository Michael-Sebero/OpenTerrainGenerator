package com.pg85.otg.forge.events.dimensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalMaterialData;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.dimensions.DimensionConfig;
import com.pg85.otg.forge.ForgeEngine;
import com.pg85.otg.forge.blocks.portal.BlockPortalOTG;
import com.pg85.otg.forge.dimensions.OTGTeleporter;
import com.pg85.otg.forge.materials.ForgeMaterialData;
import com.pg85.otg.forge.world.ForgeWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntityTravelToDimensionListener
{
	// OPTIMIZATION: Cache portal material lookups to avoid repeated DimensionConfig queries
	private static final Map<String, ArrayList<LocalMaterialData>> portalMaterialCache = new HashMap<>();
	
	// OPTIMIZATION: Cache dimension lookup results
	private static final Map<String, Integer> portalMaterialToDimensionCache = new HashMap<>();
	
	@SubscribeEvent
	public void entityTravelToDimension(EntityTravelToDimensionEvent e)
	{
		if(e.getDimension() == -1)
		{
			World entityWorld = e.getEntity().getEntityWorld();
			BlockPos entityPos = e.getEntity().getPosition();
			
			// OPTIMIZED: Reduce search radius and use early exit pattern
			BlockPos closestPortalPos = findNearestPortal(entityWorld, entityPos);
			
			if(closestPortalPos == null)
			{
				return;
			}

			// Find portal material
			BlockPos materialPos = findPortalMaterial(entityWorld, closestPortalPos);
			if(materialPos == null || materialPos.getY() <= 0)
			{
				return;
			}
			
			IBlockState blockState = entityWorld.getBlockState(materialPos);
			ForgeMaterialData playerPortalMaterial = ForgeMaterialData.ofMinecraftBlockState(blockState);

			// OPTIMIZED: Use cached portal materials and dimension lookups
			int destinationDim = findDestinationDimension(playerPortalMaterial, e.getEntity().dimension);
			
			if(destinationDim == -1)
			{
				return; // No matching OTG portal found
			}

			if(destinationDim == 0 && e.getEntity().dimension == 0)
			{
				// No custom dimensions exist, destroy the portal
				entityWorld.setBlockToAir(entityPos);
				return;
			}

			e.setCanceled(true); // Don't tp to nether

			if(e.getEntity() instanceof EntityPlayerMP)
			{
				OTGTeleporter.changeDimension(destinationDim, (EntityPlayerMP)e.getEntity(), true, false);
			} else {
				OTGTeleporter.changeDimension(destinationDim, e.getEntity());
			}
		}
	}
	
	// OPTIMIZED: Separate portal finding logic with early exit and reduced search area
	private BlockPos findNearestPortal(World world, BlockPos center)
	{
		BlockPos closest = null;
		int closestDist = Integer.MAX_VALUE;
		
		// OPTIMIZATION: Search in expanding shells, exit early when portal found
		// This dramatically reduces checks when portal is nearby
		for(int radius = 0; radius <= 2; radius++)
		{
			for(int x = -radius; x <= radius; x++)
			{
				for(int z = -radius; z <= radius; z++)
				{
					// OPTIMIZATION: Only check outer shell for radius > 0 to avoid duplicate checks
					if(radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius)
						continue;
						
					for(int y = -2; y < 4; y++)
					{
						BlockPos checkPos = center.add(x, y, z);
						if(world.getBlockState(checkPos).getBlock() instanceof BlockPortalOTG)
						{
							int dist = Math.abs(x) + Math.abs(y) + Math.abs(z);
							if(dist < closestDist)
							{
								closest = checkPos;
								closestDist = dist;
							}
						}
					}
				}
			}
			// OPTIMIZATION: Exit early if portal found in innermost radius
			if(closest != null && radius == 0)
				break;
		}
		
		return closest;
	}
	
	// OPTIMIZED: Limit downward search with counter to prevent infinite loops
	private BlockPos findPortalMaterial(World world, BlockPos portalPos)
	{
		BlockPos materialPos = portalPos;
		IBlockState blockState = world.getBlockState(materialPos);
		
		// OPTIMIZATION: Add search limit to prevent excessive iterations
		int searchLimit = 0;
		while(blockState.getBlock() instanceof BlockPortalOTG && materialPos.getY() > 0 && searchLimit < 10)
		{
			materialPos = materialPos.down();
			blockState = world.getBlockState(materialPos);
			searchLimit++;
		}
		
		return materialPos;
	}
	
	// OPTIMIZED: Cache portal materials and dimension lookups
	private int findDestinationDimension(ForgeMaterialData playerPortalMaterial, int currentDim)
	{
		// OPTIMIZATION: Check cache first
		String materialKey = playerPortalMaterial.toString();
		Integer cachedDim = portalMaterialToDimensionCache.get(materialKey);
		if(cachedDim != null && cachedDim != currentDim)
		{
			return cachedDim;
		}
		
		// Check overworld first
		ForgeWorld overWorld = ((ForgeEngine)OTG.getEngine()).getOverWorld();
		if(overWorld == null)
		{
			DimensionConfig dimConfig = OTG.getDimensionsConfig().Overworld;
			ArrayList<LocalMaterialData> portalMaterials = getPortalMaterials(dimConfig);

			for(LocalMaterialData portalMaterial : portalMaterials)
			{
				if(playerPortalMaterial.equals(portalMaterial))
				{
					int targetDim = currentDim != 0 ? 0 : -1;
					if(targetDim != -1)
					{
						// OPTIMIZATION: Cache the result
						portalMaterialToDimensionCache.put(materialKey, targetDim);
					}
					return targetDim;
				}
			}
		}

		// Check other dimensions
		ArrayList<LocalWorld> forgeWorlds = ((ForgeEngine)OTG.getEngine()).getAllWorlds();
		for(LocalWorld localWorld : forgeWorlds)
		{
			ForgeWorld forgeWorld = (ForgeWorld)localWorld;
			DimensionConfig dimConfig = OTG.getDimensionsConfig().getDimensionConfig(forgeWorld.getName());
			ArrayList<LocalMaterialData> portalMaterials = getPortalMaterials(dimConfig);

			for(LocalMaterialData portalMaterial : portalMaterials)
			{
				if(playerPortalMaterial.equals(portalMaterial))
				{
					int worldDim = forgeWorld.getWorld().provider.getDimension();
					if(worldDim != currentDim)
					{
						// OPTIMIZATION: Cache the result
						portalMaterialToDimensionCache.put(materialKey, worldDim);
						return worldDim;
					}
				}
			}
		}
		
		return -1;
	}
	
	// OPTIMIZATION: Cache portal materials per dimension config
	private ArrayList<LocalMaterialData> getPortalMaterials(DimensionConfig dimConfig)
	{
		String cacheKey = dimConfig.PresetName != null ? dimConfig.PresetName : "overworld";
		ArrayList<LocalMaterialData> cached = portalMaterialCache.get(cacheKey);
		
		if(cached == null)
		{
			cached = dimConfig.Settings.GetDimensionPortalMaterials();
			portalMaterialCache.put(cacheKey, cached);
		}
		
		return cached;
	}
	
	// OPTIMIZATION: Clear caches when dimensions are added/removed or configs change
	public static void clearPortalCache()
	{
		portalMaterialCache.clear();
		portalMaterialToDimensionCache.clear();
	}
}
