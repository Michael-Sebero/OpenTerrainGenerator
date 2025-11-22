package com.pg85.otg.forge.network.server.packets;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;

import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.forge.network.OTGPacket;
import com.pg85.otg.forge.network.client.AbstractClientMessageHandler;

public class PregeneratorStatusPacket extends OTGPacket
{
	public PregeneratorStatusPacket()
	{
		super();
	}
	
	public PregeneratorStatusPacket(ByteBuf nettyBuffer)
	{
		super(nettyBuffer);
	}
	
	public static void writeToStream(ByteBufOutputStream stream) throws IOException
	{
	    stream.writeInt(PluginStandardValues.ProtocolVersion);
	    stream.writeInt(0); // 0 = Normal packet
	    stream.writeInt(0); // Number of pregenerators (always 0 - disabled)
	}
	
	public static class Handler extends AbstractClientMessageHandler<PregeneratorStatusPacket>
	{
		@Override
		public IMessage handleClientMessage(EntityPlayer player, PregeneratorStatusPacket message, MessageContext ctx)
		{
			// Pregenerator disabled - just release the buffer and return
			message.getData().release();
			return null;
		}
	}
}
