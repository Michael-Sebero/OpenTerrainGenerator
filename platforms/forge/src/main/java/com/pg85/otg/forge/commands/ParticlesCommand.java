package com.pg85.otg.forge.commands;

import java.util.List;

import com.pg85.otg.OTG;
import com.pg85.otg.logging.LogMarker;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;

public class ParticlesCommand extends BaseCommand
{
    // Minecraft formatting codes - using escape sequences
    private static final String SECTION_SIGN = "\u00A7";
    private static final String COLOR_GREEN = SECTION_SIGN + "2";
    private static final String COLOR_BRIGHT_GREEN = SECTION_SIGN + "a";
    
    ParticlesCommand()
    {
        name = "particles";
        usage = "particles";
        description = "View a list of particles that can be used fort OTG portals, or spawned inside biome objects using the Particle() tag.";
    }

    @Override
    public boolean onCommand(ICommandSender sender, List<String> args)
    {
        sender.sendMessage(new TextComponentString(""));
        OTG.log(LogMarker.INFO, "-- Particles List --");
        sender.sendMessage(new TextComponentString("-- Particles List --"));
        sender.sendMessage(new TextComponentString(""));
        
        for (String entry : EnumParticleTypes.getParticleNames())
        {
            String msg = entry;
            
            // FIXED: Remove Minecraft color codes for console logging
            String logMsg = msg.replace(COLOR_GREEN, "")
                               .replace(SECTION_SIGN, "")
                               .replace(COLOR_BRIGHT_GREEN, "");
            OTG.log(LogMarker.INFO, logMsg);
            sender.sendMessage(new TextComponentString(MESSAGE_COLOR + "- " + msg));
        }

        OTG.log(LogMarker.INFO, "----");
        return true;
    }
}
