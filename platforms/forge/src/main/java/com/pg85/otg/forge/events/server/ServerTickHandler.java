package com.pg85.otg.forge.events.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalMaterialData;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.dimensions.DimensionConfig;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.customobjects.bo3.bo3function.BO3EntityFunction;
import com.pg85.otg.customobjects.bo4.bo4function.BO4EntityFunction;
import com.pg85.otg.customobjects.bofunctions.EntityFunction;
import com.pg85.otg.customobjects.bofunctions.ModDataFunction;
import com.pg85.otg.customobjects.bofunctions.ParticleFunction;
import com.pg85.otg.customobjects.bofunctions.SpawnerFunction;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.forge.ForgeEngine;
import com.pg85.otg.forge.OTGPlugin;
import com.pg85.otg.forge.dimensions.OTGTeleporter;
import com.pg85.otg.forge.network.server.ServerPacketManager;
import com.pg85.otg.forge.util.MobSpawnGroupHelper;
import com.pg85.otg.forge.world.ForgeWorld;
import com.pg85.otg.logging.LogMarker;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.materials.MaterialHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class ServerTickHandler
{
    private int currentTimeInSeconds = 0;
    private int lastSpawnedTimeInSeconds = 0;
    
    // Pre-allocated collections to avoid GC pressure
    private final Set<ChunkCoordinate> eligibleChunksForSpawning = new HashSet<>(289); // 17x17
    private final ArrayList<PlayerPosData> playerCoords = new ArrayList<>(16);
    private final ArrayList<SpawnerDistanceData> spawnerDatasWithDistance = new ArrayList<>(64);
    private final ArrayList<SpawnerFunction<?>> spawnerDataSortedByDistance = new ArrayList<>(64);
    private final ArrayList<ParticleFunction<?>> particleDataForOTGPerPlayer = new ArrayList<>(64);
    private final ArrayList<EntityPlayer> tempPlayerList = new ArrayList<>(16);
    
    // Reusable random instance
    private final Random random = new Random();
    
    // Constants
    private static final double MAX_DIST_SQ = 33.0 * 33.0;
    private static final int MOB_COUNT_RADIUS = 32;
    private static final byte SPAWN_RADIUS = 8;
    
    // Simple data holder classes to avoid Object[] allocations
    private static class PlayerPosData {
        double x, y, z;
        EntityPlayer player;
        
        void set(EntityPlayer p) {
            this.x = p.posX;
            this.y = p.posY;
            this.z = p.posZ;
            this.player = p;
        }
    }
    
    private static class SpawnerDistanceData implements Comparable<SpawnerDistanceData> {
        double distance;
        SpawnerFunction<?> spawner;
        
        void set(double dist, SpawnerFunction<?> s) {
            this.distance = dist;
            this.spawner = s;
        }
        
        @Override
        public int compareTo(SpawnerDistanceData o) {
            return Double.compare(this.distance, o.distance);
        }
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
        if(event.phase == Phase.START)
        {
            ((ForgeEngine)OTG.getEngine()).processPregeneratorTick();
            teleportPlayers();
            return;
        }

        if(event.phase != Phase.END)
        {
            return;
        }
        
        // Particles and Spawners
        currentTimeInSeconds = (int)(System.currentTimeMillis() / 1000L);
        if(currentTimeInSeconds != lastSpawnedTimeInSeconds)
        {
            lastSpawnedTimeInSeconds = currentTimeInSeconds;
            for(LocalWorld forgeWorld : ((ForgeEngine)OTG.getEngine()).getWorldLoader().getAllLoadedWorlds())
            {
                findChunksForSpawning((ForgeWorld)forgeWorld);
            }
        }

        // ModData - process IMC messages
        processIMCMessages();
    }
    
    private void processIMCMessages()
    {
        List<IMCMessage> messages = FMLInterModComms.fetchRuntimeMessages(OTGPlugin.Instance);
        if(messages.isEmpty())
        {
            return;
        }
        
        for(IMCMessage imcMessage : messages)
        {
            if(imcMessage.key.equalsIgnoreCase("GetModData") && imcMessage.isStringMessage())
            {
                handleGetModData(imcMessage);
            }
            else if(imcMessage.key.equalsIgnoreCase("ModData"))
            {
                handleModData(imcMessage);
            }
        }
    }
    
    private void handleGetModData(IMCMessage imcMessage)
    {
        String[] paramString = imcMessage.getStringValue().split(",");
        if(paramString.length != 3)
        {
            OTG.log(LogMarker.WARN, "The mod " + imcMessage.getSender() + " has sent invalid parameters: " + imcMessage.getStringValue());
            return;
        }
        
        String worldName = paramString[0];
        ForgeWorld forgeWorld = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getWorld(worldName);
        if(forgeWorld == null)
        {
            forgeWorld = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getUnloadedWorld(worldName);
        }
        
        int chunkX, chunkZ;
        try
        {
            chunkX = Integer.parseInt(paramString[1]);
            chunkZ = Integer.parseInt(paramString[2]);
        }
        catch(NumberFormatException ex)
        {
            OTG.log(LogMarker.WARN, "The mod " + imcMessage.getSender() + " sent invalid coordinates: " + imcMessage.getStringValue());
            return;
        }

        StringBuilder messageString = new StringBuilder();
        HashMap<String, ArrayList<ModDataFunction<?>>> modDataInChunk = forgeWorld.getWorldSession()
            .getModDataForChunk(ChunkCoordinate.fromChunkCoords(chunkX, chunkZ));
        
        if(modDataInChunk != null && !modDataInChunk.isEmpty())
        {
            for(Entry<String, ArrayList<ModDataFunction<?>>> modNameAndData : modDataInChunk.entrySet())
            {
                if(modNameAndData.getKey().equalsIgnoreCase(imcMessage.getSender()))
                {
                    for(ModDataFunction<?> modData : modNameAndData.getValue())
                    {
                        messageString.append("[").append(modData.x).append(",")
                            .append(modData.y).append(",").append(modData.z).append(",")
                            .append(modData.modData).append("]");
                    }
                }
            }
            FMLInterModComms.sendRuntimeMessage(OTGPlugin.Instance, imcMessage.getSender(), "ModData", 
                "[[" + worldName + "," + chunkX + "," + chunkZ + "]" + 
                (messageString.length() > 0 ? messageString.toString() : "[]") + "]");
        }
        else
        {
            FMLInterModComms.sendRuntimeMessage(OTGPlugin.Instance, imcMessage.getSender(), "ModData", 
                "[[" + worldName + "," + chunkX + "," + chunkZ + "]]");
        }
    }
    
    private void handleModData(IMCMessage imcMessage)
    {
        String[] paramString = imcMessage.getStringValue().replace("[[", "").replace("]]", "").split("\\]\\[");
        String[] chunkCoordString = paramString[0].split(",");
        String worldName = chunkCoordString[0];
        
        if(paramString.length < 2)
        {
            return; // Chunk hasn't been populated yet
        }
        
        ForgeWorld world = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getWorld(worldName);
        if(world == null)
        {
            world = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getUnloadedWorld(worldName);
        }
        
        if(world == null || world.getConfigs() == null || world.getConfigs().getWorldConfig() == null)
        {
            OTG.log(LogMarker.FATAL, "Error: Failed to load world \"" + worldName + "\"");
            throw new RuntimeException("Error: Failed to load world \"" + worldName + "\"");
        }
        
        for(int i = 1; i < paramString.length; i++)
        {
            if(paramString[i].isEmpty())
            {
                continue;
            }
            
            String[] modDataString = paramString[i].split(",");
            int modDataBlockX = Integer.parseInt(modDataString[0]);
            int modDataBlockY = Integer.parseInt(modDataString[1]);
            int modDataBlockZ = Integer.parseInt(modDataString[2]);
            String modDataText = modDataString[3];

            String[] paramString2 = modDataText.split("\\/");
            if(paramString2.length <= 1)
            {
                continue;
            }
            
            if(paramString2[0].equals("mob"))
            {
                spawnModDataMob(world, modDataBlockX, modDataBlockY, modDataBlockZ, paramString2);
            }
            else if(paramString2[0].equals("block"))
            {
                setModDataBlock(world, modDataBlockX, modDataBlockY, modDataBlockZ, paramString2[1]);
            }
        }
    }
    
    private void spawnModDataMob(ForgeWorld world, int x, int y, int z, String[] params)
    {
        try
        {
            EntityFunction<?> entityFunc = world.isBo4Enabled() ? new BO4EntityFunction() : new BO3EntityFunction();
            entityFunc.x = x;
            entityFunc.y = y;
            entityFunc.z = z;
            entityFunc.processEntityName(params[1]);
            entityFunc.groupSize = params.length > 2 ? Integer.parseInt(params[2]) : 1;
            if(params.length > 5)
            {
                entityFunc.processNameTagOrFileName(params[5]);
            }
            else
            {
                entityFunc.nameTagOrNBTFileName = null;
                entityFunc.originalNameTagOrNBTFileName = null;
            }
            entityFunc.rotation = 0;
            world.spawnEntity(entityFunc, null);
        }
        catch(NumberFormatException ex)
        {
            if(OTG.getPluginConfig().spawnLog)
            {
                OTG.log(LogMarker.WARN, "Error in ModData: parameter count was not a number");
            }
        }
    }
    
    private void setModDataBlock(ForgeWorld world, int x, int y, int z, String materialName)
    {
        try
        {
            LocalMaterialData material = MaterialHelper.readMaterial(materialName);
            world.setBlock(x, y, z, material, null, null, false);
        }
        catch(InvalidConfigException e)
        {
            if(OTG.getPluginConfig().spawnLog)
            {
                OTG.log(LogMarker.WARN, "Error in ModData: invalid material");
            }
        }
    }

    private void findChunksForSpawning(ForgeWorld world)
    {
        WorldServer worldServer = (WorldServer)world.getWorld();
        List<EntityPlayer> playerEntities = worldServer.playerEntities;
        
        if(playerEntities.isEmpty())
        {
            return;
        }

        // Clear and rebuild eligible chunks
        eligibleChunksForSpawning.clear();
        playerCoords.clear();
        
        // Ensure we have enough PlayerPosData objects
        while(playerCoords.size() < playerEntities.size())
        {
            playerCoords.add(new PlayerPosData());
        }
        
        int playerCount = 0;
        for(int i = 0, size = playerEntities.size(); i < size; i++)
        {
            EntityPlayer entityplayer = playerEntities.get(i);
            int playerPosChunkX = MathHelper.floor(entityplayer.posX / 16.0);
            int playerPosChunkZ = MathHelper.floor(entityplayer.posZ / 16.0);
            
            // Store player data
            playerCoords.get(playerCount).set(entityplayer);
            playerCount++;

            for(int l = -SPAWN_RADIUS; l <= SPAWN_RADIUS; l++)
            {
                for(int i1 = -SPAWN_RADIUS; i1 <= SPAWN_RADIUS; i1++)
                {
                    boolean isEdge = l == -SPAWN_RADIUS || l == SPAWN_RADIUS || i1 == -SPAWN_RADIUS || i1 == SPAWN_RADIUS;
                    if(!isEdge)
                    {
                        eligibleChunksForSpawning.add(ChunkCoordinate.fromChunkCoords(l + playerPosChunkX, i1 + playerPosChunkZ));
                    }
                }
            }
        }

        // Process OTG entity despawning - batch process
        processEntityDespawning(worldServer, playerCount);
        
        // Process spawners
        processSpawners(world, worldServer, playerCount);
        
        // Process particles for each player
        processParticles(world, worldServer, playerCount);
    }
    
    private void processEntityDespawning(WorldServer worldServer, int playerCount)
    {
        List<Entity> loadedEntities = worldServer.loadedEntityList;
        
        for(int x = 0, size = loadedEntities.size(); x < size; x++)
        {
            Entity entity = loadedEntities.get(x);
            NBTTagCompound entityData = entity.getEntityData();

            if(!entityData.hasKey("OTG"))
            {
                continue;
            }
            
            // Check if player in range
            boolean playerInRange = false;
            for(int a = 0; a < playerCount; a++)
            {
                PlayerPosData pData = playerCoords.get(a);
                double dx = pData.x - entity.posX;
                double dy = pData.y - entity.posY;
                double dz = pData.z - entity.posZ;
                double distSq = dx * dx + dy * dy + dz * dz;

                if(distSq < MAX_DIST_SQ)
                {
                    playerInRange = true;
                    break;
                }
            }
            
            if(playerInRange)
            {
                continue;
            }

            int despawnTimer = entityData.getInteger("OTGDT");
            if(despawnTimer <= 1)
            {
                entity.setDead();
            }
            else
            {
                entityData.setInteger("OTGDT", despawnTimer - 1);
            }
        }
    }
    
    private void processSpawners(ForgeWorld world, WorldServer worldServer, int playerCount)
    {
        spawnerDatasWithDistance.clear();
        spawnerDataSortedByDistance.clear();
        
        int spawnerIndex = 0;
        
        for(ChunkCoordinate chunkCoord : eligibleChunksForSpawning)
        {
            ArrayList<SpawnerFunction<?>> spawnerDataForOTG = world.getWorldSession()
                .getSpawnersForChunk(chunkCoord);

            if(spawnerDataForOTG == null || spawnerDataForOTG.isEmpty())
            {
                continue;
            }
            
            for(int s = 0, sSize = spawnerDataForOTG.size(); s < sSize; s++)
            {
                SpawnerFunction<?> spawnerData = spawnerDataForOTG.get(s);
                double distToClosestPlayer = MAX_DIST_SQ;

                for(int a = 0; a < playerCount; a++)
                {
                    PlayerPosData pData = playerCoords.get(a);
                    double dx = pData.x - spawnerData.x;
                    double dy = pData.y - spawnerData.y;
                    double dz = pData.z - spawnerData.z;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    if(distSq < distToClosestPlayer)
                    {
                        distToClosestPlayer = distSq;
                    }
                }

                if(distToClosestPlayer > 0 && distToClosestPlayer < MAX_DIST_SQ)
                {
                    // Ensure we have enough SpawnerDistanceData objects
                    while(spawnerDatasWithDistance.size() <= spawnerIndex)
                    {
                        spawnerDatasWithDistance.add(new SpawnerDistanceData());
                    }
                    spawnerDatasWithDistance.get(spawnerIndex).set(distToClosestPlayer, spawnerData);
                    spawnerIndex++;
                }
            }
        }
        
        // Sort by distance - use sublist to avoid processing unused entries
        if(spawnerIndex > 0)
        {
            List<SpawnerDistanceData> activeSpawners = spawnerDatasWithDistance.subList(0, spawnerIndex);
            Collections.sort(activeSpawners);
            
            for(int i = 0; i < spawnerIndex; i++)
            {
                spawnerDataSortedByDistance.add(spawnerDatasWithDistance.get(i).spawner);
            }
        }

        // Process spawners
        for(int i = 0, size = spawnerDataSortedByDistance.size(); i < size; i++)
        {
            processSpawner(spawnerDataSortedByDistance.get(i), worldServer);
        }
    }
    
    private void processSpawner(SpawnerFunction<?> spawnerData, WorldServer worldServer)
    {
        int interval = spawnerData.interval;
        
        boolean shouldSpawn = spawnerData.firstSpawn || 
            ((currentTimeInSeconds - spawnerData.intervalOffset) % interval == 0);
        
        if(!shouldSpawn)
        {
            return;
        }
        
        if(spawnerData.firstSpawn)
        {
            spawnerData.intervalOffset = currentTimeInSeconds;
            spawnerData.firstSpawn = false;
        }

        String mobTypeName = spawnerData.mobName;
        Class<? extends Entity> entityClass = MobSpawnGroupHelper.toMinecraftClass(mobTypeName);
        
        if(entityClass == null)
        {
            if(OTG.getPluginConfig().spawnLog)
            {
                OTG.log(LogMarker.WARN, "Could not find entity: " + mobTypeName);
            }
            return;
        }

        ResourceLocation resourceLocation = MobSpawnGroupHelper.resourceLocationFromMinecraftClass(entityClass);
        
        NBTTagCompound nbttagcompound = null;
        if(spawnerData.getMetaData() != null)
        {
            try
            {
                NBTBase nbtbase = JsonToNBT.getTagFromJson(spawnerData.getMetaData());
                if(!(nbtbase instanceof NBTTagCompound))
                {
                    return;
                }
                nbttagcompound = (NBTTagCompound)nbtbase;
                nbttagcompound.setString("id", resourceLocation.toString());
            }
            catch(NBTException nbtexception)
            {
                if(OTG.getPluginConfig().spawnLog)
                {
                    OTG.log(LogMarker.WARN, "Invalid NBT tag for mob: " + spawnerData.getMetaData());
                }
                return;
            }
        }

        // Count existing mobs in area
        int worldMobCount = countMobsInArea(worldServer, entityClass, spawnerData.x, spawnerData.y, spawnerData.z);
        
        if(worldMobCount >= spawnerData.maxCount)
        {
            return;
        }

        float x = spawnerData.x + 0.5F;
        float y = spawnerData.y;
        float z = spawnerData.z + 0.5F;

        int groupSize = spawnerData.groupSize;
        int spawnChance = spawnerData.spawnChance;
        int max = spawnerData.maxCount;
        int despawnTime = spawnerData.despawnTime;

        for(int r = 0; r < groupSize && worldMobCount < max; r++)
        {
            if(spawnChance <= random.nextInt(100))
            {
                continue;
            }

            Entity entity = createEntity(entityClass, nbttagcompound, worldServer);
            if(entity == null)
            {
                break;
            }

            setupEntity(entity, spawnerData, x, y, z, despawnTime);

            if(entity instanceof EntityLiving)
            {
                if(trySpawnLivingEntity((EntityLiving)entity, spawnerData, worldServer, nbttagcompound, x, y, z))
                {
                    worldMobCount++;
                }
            }
            else
            {
                if(trySpawnEntity(entity, spawnerData, worldServer, x, y, z))
                {
                    worldMobCount++;
                }
            }
        }
    }
    
    private int countMobsInArea(WorldServer worldServer, Class<? extends Entity> entityClass, int cx, int cy, int cz)
    {
        int count = 0;
        List<Entity> loadedEntities = worldServer.loadedEntityList;
        
        for(int x = 0, size = loadedEntities.size(); x < size; x++)
        {
            Entity entity = loadedEntities.get(x);
            if(entity.getClass() == entityClass && entity.getEntityData().hasKey("OTG"))
            {
                if(entity.posX >= cx - MOB_COUNT_RADIUS && entity.posX <= cx + MOB_COUNT_RADIUS &&
                   entity.posY >= cy - MOB_COUNT_RADIUS && entity.posY <= cy + MOB_COUNT_RADIUS &&
                   entity.posZ >= cz - MOB_COUNT_RADIUS && entity.posZ <= cz + MOB_COUNT_RADIUS)
                {
                    count++;
                }
            }
        }
        return count;
    }
    
    private Entity createEntity(Class<? extends Entity> entityClass, NBTTagCompound nbt, WorldServer worldServer)
    {
        try
        {
            if(nbt == null)
            {
                return entityClass.getConstructor(World.class).newInstance(worldServer);
            }
            return EntityList.createEntityFromNBT(nbt, worldServer);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private void setupEntity(Entity entity, SpawnerFunction<?> spawnerData, float x, float y, float z, int despawnTime)
    {
        double velocityX = spawnerData.velocityXSet ? spawnerData.velocityX : random.nextDouble() * 0.2 - 0.1;
        double velocityY = spawnerData.velocityYSet ? spawnerData.velocityY : (entity instanceof EntityLiving ? 0 : 0.1);
        double velocityZ = spawnerData.velocityZSet ? spawnerData.velocityZ : random.nextDouble() * 0.2 - 0.1;

        entity.setLocationAndAngles(x, y, z, spawnerData.yaw, spawnerData.pitch);
        if(!(entity instanceof EntityHanging))
        {
            entity.addVelocity(velocityX, velocityY, velocityZ);
        }

        entity.getEntityData().setBoolean("OTG", true);
        if(despawnTime > 0)
        {
            entity.getEntityData().setInteger("OTGDT", despawnTime - 1);
        }
    }
    
    private boolean trySpawnLivingEntity(EntityLiving entity, SpawnerFunction<?> spawnerData, 
            WorldServer worldServer, NBTTagCompound nbt, float x, float y, float z)
    {
        Result canSpawn = ForgeEventFactory.canEntitySpawn(entity, worldServer, x, y, z);
        
        boolean entityCanSpawnHere = false;
        if(canSpawn == Result.ALLOW)
        {
            entityCanSpawnHere = true;
        }
        else if(canSpawn == Result.DEFAULT)
        {
            entityCanSpawnHere = checkSpawnConditions(entity, worldServer);
        }

        if(!entityCanSpawnHere)
        {
            return false;
        }

        if(spawnerData.getMetaData() == null)
        {
            entity.onInitialSpawn(worldServer.getDifficultyForLocation(new BlockPos(x, y, z)), null);
        }
        worldServer.spawnEntity(entity);

        if(nbt != null)
        {
            spawnRidingEntities(entity, nbt, worldServer, x, y, z);
        }
        
        return true;
    }
    
    private boolean trySpawnEntity(Entity entity, SpawnerFunction<?> spawnerData, WorldServer worldServer, float x, float y, float z)
    {
        if(!checkSpawnConditions(entity, worldServer))
        {
            return false;
        }
        
        worldServer.spawnEntity(entity);
        return true;
    }
    
    private boolean checkSpawnConditions(Entity entity, WorldServer worldServer)
    {
        boolean b1 = worldServer.checkNoEntityCollision(entity.getEntityBoundingBox());
        boolean b2 = worldServer.getCollisionBoxes(entity, entity.getEntityBoundingBox()).isEmpty();
        boolean b3 = !worldServer.containsAnyLiquid(entity.getEntityBoundingBox());
        
        if(entity instanceof EntityCreature)
        {
            int ia = MathHelper.floor(entity.posX);
            int ja = MathHelper.floor(entity.getEntityBoundingBox().minY);
            int ka = MathHelper.floor(entity.posZ);
            boolean b5 = ((EntityCreature)entity).getBlockPathWeight(new BlockPos(ia, ja, ka)) >= 0.0F;
            return b1 && b2 && b3 && b5;
        }
        
        return b1 && b2 && b3;
    }
    
    private void spawnRidingEntities(Entity entity, NBTTagCompound nbt, WorldServer worldServer, float x, float y, float z)
    {
        Entity currentEntity = entity;
        NBTTagCompound currentNbt = nbt;
        
        while(currentEntity != null && currentNbt.hasKey("Riding", 10))
        {
            Entity rider = EntityList.createEntityFromNBT(currentNbt.getCompoundTag("Riding"), worldServer);
            if(rider != null)
            {
                rider.setLocationAndAngles(x, y, z, rider.rotationYaw, rider.rotationPitch);
                worldServer.spawnEntity(rider);
                currentEntity.startRiding(rider);
            }
            currentEntity = rider;
            currentNbt = currentNbt.getCompoundTag("Riding");
        }
    }
    
    private void processParticles(ForgeWorld world, WorldServer worldServer, int playerCount)
    {
        for(int a = 0; a < playerCount; a++)
        {
            PlayerPosData pData = playerCoords.get(a);
            EntityPlayer player = pData.player;
            particleDataForOTGPerPlayer.clear();

            for(ChunkCoordinate chunkCoord : eligibleChunksForSpawning)
            {
                ArrayList<ParticleFunction<?>> particleDataForOTG = world.getWorldSession().getParticlesForChunk(chunkCoord);

                if(particleDataForOTG == null || particleDataForOTG.isEmpty())
                {
                    continue;
                }
                
                for(int p = 0, pSize = particleDataForOTG.size(); p < pSize; p++)
                {
                    ParticleFunction<?> particleData = particleDataForOTG.get(p);
                    double dx = pData.x - particleData.x;
                    double dy = pData.y - particleData.y;
                    double dz = pData.z - particleData.z;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    if(distSq > 0 && distSq < MAX_DIST_SQ)
                    {
                        BlockPos pos = new BlockPos(particleData.x, particleData.y, particleData.z);
                        if(!worldServer.getBlockState(pos).getMaterial().isSolid())
                        {
                            particleDataForOTGPerPlayer.add(particleData);
                        }
                        else
                        {
                            world.getWorldSession().removeParticles(chunkCoord, particleData);
                        }
                    }
                }
            }
            
            if(!particleDataForOTGPerPlayer.isEmpty())
            {
                ServerPacketManager.sendParticlesPacket(particleDataForOTGPerPlayer, (EntityPlayerMP) player);
            }
        }
    }
    
    private void teleportPlayers()
    {
        MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
        if(mcServer == null || mcServer.worlds == null)
        {
            return;
        }
        
        for(WorldServer worldServer : mcServer.worlds)
        {
            if(worldServer == null)
            {
                continue;
            }
            
            boolean isOTGWorld = false;
            if(worldServer.getWorldInfo() instanceof DerivedWorldInfo)
            {
                isOTGWorld = ((DerivedWorldInfo)worldServer.getWorldInfo()).delegate.getGeneratorOptions()
                    .equals(PluginStandardValues.PLUGIN_NAME);
            }
            else
            {
                isOTGWorld = worldServer.getWorldInfo().getGeneratorOptions().equals(PluginStandardValues.PLUGIN_NAME);
            }
            
            if(!isOTGWorld)
            {
                continue;
            }
            
            // Use temp list to avoid ConcurrentModificationException
            tempPlayerList.clear();
            tempPlayerList.addAll(worldServer.playerEntities);
            
            for(int i = 0, size = tempPlayerList.size(); i < size; i++)
            {
                tryTeleportPlayer(tempPlayerList.get(i));
            }
        }
    }
    
    private void tryTeleportPlayer(EntityPlayer player)
    {
        ForgeWorld playerWorld = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getWorld(player.world);
        if(playerWorld == null)
        {
            return;
        }
        
        DimensionConfig dimConfig = OTG.getDimensionsConfig().getDimensionConfig(playerWorld.getName());
        if(dimConfig == null)
        {
            return;
        }

        // DimensionBelow
        String dimBelow = dimConfig.Settings.DimensionBelow;
        if(dimBelow != null && !dimBelow.trim().isEmpty())
        {
            if(player.getPosition().getY() < dimConfig.Settings.DimensionBelowHeight)
            {
                World destinationWorld = getDestinationWorld(dimBelow);
                if(destinationWorld != null)
                {
                    if(destinationWorld == playerWorld.getWorld())
                    {
                        BlockPos pos = player.getPosition();
                        player.world.setBlockToAir(new BlockPos(pos.getX(), 254, pos.getZ()));
                        player.world.setBlockToAir(new BlockPos(pos.getX(), 255, pos.getZ()));
                        player.setPositionAndUpdate(pos.getX(), 254, pos.getZ());
                    }
                    else
                    {
                        teleportPlayerToDimension(destinationWorld.provider.getDimension(), player);
                        return;
                    }
                }
            }
        }

        // DimensionAbove
        String dimAbove = dimConfig.Settings.DimensionAbove;
        if(dimAbove != null && !dimAbove.trim().isEmpty())
        {
            if(player.getPosition().getY() > dimConfig.Settings.DimensionAboveHeight)
            {
                World destinationWorld = getDestinationWorld(dimAbove);
                if(destinationWorld != null)
                {
                    if(destinationWorld == playerWorld.getWorld())
                    {
                        BlockPos pos = player.getPosition();
                        player.world.setBlockToAir(new BlockPos(pos.getX(), 1, pos.getZ()));
                        player.world.setBlockToAir(new BlockPos(pos.getX(), 2, pos.getZ()));
                        player.setPositionAndUpdate(pos.getX(), 1, pos.getZ());
                        
                        // Make a hole next to the player
                        player.world.setBlockToAir(new BlockPos(pos.getX() + 1, 0, pos.getZ()));
                        player.world.setBlockToAir(new BlockPos(pos.getX() + 1, 1, pos.getZ()));
                        player.world.setBlockToAir(new BlockPos(pos.getX() + 1, 2, pos.getZ()));
                    }
                    else
                    {
                        teleportPlayerToDimension(destinationWorld.provider.getDimension(), player);
                    }
                }
            }
        }
    }
    
    private World getDestinationWorld(String dimensionName)
    {
        String overworldPreset = OTG.getDimensionsConfig().Overworld.PresetName;
        boolean isOverworld = (overworldPreset == null && dimensionName.toLowerCase().trim().equals("overworld")) ||
            (overworldPreset != null && overworldPreset.equals(dimensionName));
        
        if(isOverworld)
        {
            return DimensionManager.getWorld(0);
        }
        
        ForgeWorld destinationForgeWorld = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getWorld(dimensionName);
        if(destinationForgeWorld == null)
        {
            destinationForgeWorld = (ForgeWorld)((ForgeEngine)OTG.getEngine()).getUnloadedWorld(dimensionName);
        }
        
        return destinationForgeWorld != null ? destinationForgeWorld.getWorld() : null;
    }
    
    private void teleportPlayerToDimension(int newDimension, EntityPlayer player)
    {
        if(player instanceof EntityPlayerMP)
        {
            OTGTeleporter.changeDimension(newDimension, (EntityPlayerMP)player, false, false);
        }
    }
}
