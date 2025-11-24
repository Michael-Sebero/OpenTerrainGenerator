package com.pg85.otg.configuration.settingType;

import com.pg85.otg.exception.InvalidConfigException;

/**
 * Reads and writes colors. The colors are represented as integers internally,
 * but are written as hexadecimal colors in upper case, starting with a #.
 *
 * <p>Color reading allows multiple formats. Colors starting with 0x or
 * # are interpreted as hexadecimal numbers, colors starting with 0 as octal
 * numbers and other colors as decimal numbers. Colors are case insensitive.
 */
class ColorSetting extends Setting<Integer>
{
    private final int defaultValue; // Made final

    ColorSetting(String name, String defaultValue)
    {
        super(name);
        this.defaultValue = Integer.decode(defaultValue);
    }

    @Override
    public Integer getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public Integer read(String string) throws InvalidConfigException
    {
        try
        {
            int value = Integer.decode(string); // Use primitive to avoid boxing
            if (value > 0xffffff || value < 0)
            {
                throw new InvalidConfigException("Color must have 6 hexadecimal digits");
            }
            return value;
        } catch (NumberFormatException e)
        {
            throw new InvalidConfigException("Invalid color " + string);
        }
    }

    @Override
    public String write(Integer value)
    {
        // FIX: Original had potential issue with negative values
        // Ensure we're working with the lower 24 bits only
        return String.format("#%06X", value & 0xFFFFFF);
    }
}
