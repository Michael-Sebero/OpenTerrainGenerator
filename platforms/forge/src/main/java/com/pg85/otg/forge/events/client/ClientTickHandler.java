package com.pg85.otg.forge.events.client;

import java.util.ArrayList;
import java.util.List;

import com.pg85.otg.customobjects.bofunctions.ParticleFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class ClientTickHandler
{
    private long lastSpawnedTimeIn100Ms = 0;
    public static final ArrayList<ParticleFunction<?>> ClientParticleFunctions = new ArrayList<ParticleFunction<?>>();
    
    // Reusable list to avoid allocations
    private final ArrayList<ParticleFunction<?>> tempParticleFunctions = new ArrayList<ParticleFunction<?>>();
    
    // Cache Minecraft instance
    private Minecraft mc;
    
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event)
    {
        if(event.phase != Phase.END)
        {
            return;
        }
        
        long currentTimeIn100Ms = System.currentTimeMillis() / 100L;
        if(currentTimeIn100Ms == lastSpawnedTimeIn100Ms)
        {
            return;
        }
        
        lastSpawnedTimeIn100Ms = currentTimeIn100Ms;
        
        // Early exit if no particles - avoid synchronization when possible
        if(ClientParticleFunctions.isEmpty())
        {
            return;
        }
        
        // Cache Minecraft instance
        if(mc == null)
        {
            mc = Minecraft.getMinecraft();
        }
        
        if(!mc.inGameHasFocus)
        {
            return;
        }
        
        // Copy particle functions under lock
        tempParticleFunctions.clear();
        synchronized(ClientParticleFunctions)
        {
            if(!ClientParticleFunctions.isEmpty())
            {
                tempParticleFunctions.addAll(ClientParticleFunctions);
            }
        }
        
        if(tempParticleFunctions.isEmpty())
        {
            return;
        }
        
        for(int i = 0, size = tempParticleFunctions.size(); i < size; i++)
        {
            ParticleFunction<?> particleData = tempParticleFunctions.get(i);
            
            double interval = particleData.interval * 10;
            
            boolean shouldSpawn = particleData.firstSpawn || 
                ((currentTimeIn100Ms - particleData.intervalOffset) % interval == 0);
            
            if(!shouldSpawn)
            {
                continue;
            }
            
            if(particleData.firstSpawn)
            {
                particleData.intervalOffset = currentTimeIn100Ms;
                particleData.firstSpawn = false;
            }
            
            String particleName = particleData.particleName;
            if(particleName == null || particleName.trim().isEmpty())
            {
                continue;
            }
            
            double x = particleData.x + 0.5;
            double y = particleData.y;
            double z = particleData.z + 0.5;
            
            double velocityY = particleData.velocityYSet ? particleData.velocityY : 0;
            double velocityX = particleData.velocityXSet ? particleData.velocityX : Math.random() * 0.2 - 0.1;
            double velocityZ = particleData.velocityZSet ? particleData.velocityZ : Math.random() * 0.2 - 0.1;
            
            EnumParticleTypes enumParticleType = EnumParticleTypes.getByName(particleName);
            if(enumParticleType != null)
            {
                mc.renderGlobal.spawnParticle(enumParticleType.getParticleID(), true, x, y, z, velocityX, velocityY, velocityZ);
            }
        }
    }
}
