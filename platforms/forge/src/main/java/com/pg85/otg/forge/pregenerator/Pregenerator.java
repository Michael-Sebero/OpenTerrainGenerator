package com.pg85.otg.forge.pregenerator;

import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.forge.world.ForgeWorld;
import com.pg85.otg.util.ChunkCoordinate;

public class Pregenerator
{
	// In-game UI - kept for compatibility
	public String pregenerationWorld = "";
	public String preGeneratorProgressStatus = "";
	public String preGeneratorProgress = "";
	public String progressScreenCycle = "";
	public String progressScreenRadius = "";
	public String progressScreenElapsedTime = "";
	public String progressScreenEstimatedTime = "";
	public int progressScreenWorldSizeInBlocks;
	public long progressScreenServerUsedMbs = 0;
	public long progressScreenServerTotalMbs = 0;

	private ForgeWorld world;
	private ChunkCoordinate preGeneratorCenterPoint;
	
	public Pregenerator(LocalWorld world)
	{
		this.world = (ForgeWorld)world;
		this.pregenerationWorld = world.getConfigs().getWorldConfig().getName();
	}

	public int getPregenerationRadius()
	{
		return 0;
	}
	
	public void setPregeneratorIsRunning(boolean pregeneratorIsRunning)
	{
		// No-op: pregenerator disabled
	}

	public int setPregenerationRadius(int radius)
	{
		return 0;
	}

	public int getPregenerationBorderLeft()
	{
		return 0;
	}

	public int getPregenerationBorderRight()
	{
		return 0;
	}

	public int getPregenerationBorderTop()
	{
		return 0;
	}

	public int getPregenerationBorderBottom()
	{
		return 0;
	}
	
	public void setPreGeneratorCenterPoint(ChunkCoordinate chunkCoord)
	{
		this.preGeneratorCenterPoint = chunkCoord;
	}

	public ChunkCoordinate getPregenerationCenterPoint()
	{
		return this.preGeneratorCenterPoint;
	}

	public boolean isRunning()
	{
		return false;
	}
	
	public boolean isInitialised()
	{
		return false;
	}

	public void processTick()
	{
		// No-op: pregenerator disabled
	}

	public void shutDown()
	{
		// No-op: pregenerator disabled
	}

	public void savePregeneratorData()
	{
		// No-op: pregenerator disabled
	}
}
