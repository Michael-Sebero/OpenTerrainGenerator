package com.pg85.otg.forge.gui.dimensions.base;

import java.util.ArrayList;

import com.pg85.otg.forge.gui.dimensions.OTGGuiDimensionSettingsList;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PregeneratorSettingsEntry implements IGuiListEntry
{
	private final OTGGuiDimensionSettingsList otgGuiDimensionSettingsList;
	private final OTGGuiDimensionSettingsList parent;
    
    public PregeneratorSettingsEntry(OTGGuiDimensionSettingsList otgGuiDimensionSettingsList, OTGGuiDimensionSettingsList parent)
    {
        this.otgGuiDimensionSettingsList = otgGuiDimensionSettingsList;
		this.parent = parent;
    }
    
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks)
    {
		ArrayList<String> lines = new ArrayList<String>();
		
		lines.add("");
		lines.add("Pre-generator is disabled.");

        int linespacing = 11;
        
        for(int a = 0; a < lines.size(); a++)
        {
        	this.otgGuiDimensionSettingsList.mc.fontRenderer.drawString(
    			lines.get(a), 
    			x + 6,  			
    			y + slotHeight - this.otgGuiDimensionSettingsList.mc.fontRenderer.FONT_HEIGHT - 5 + (a * linespacing), 
    			16777215
			);
        }
    }

    public void keyTyped(char typedChar, int keyCode)
    {
    }
    
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY)
    {
        return false;
    }

    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY)
    {
    }

    public void updatePosition(int slotIndex, int x, int y, float partialTicks)
    {
    }

	@Override
	public String getLabelText() {
		return null;
	}

	@Override
	public String getDisplayText() {
		return null;
	}   	
}
