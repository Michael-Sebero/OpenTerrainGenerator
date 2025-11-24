package com.pg85.otg.util.helpers;

public final class MathHelper // Made final - utility class
{
    private static final float[] SINE_TABLE = new float[65536]; // Better name

    private MathHelper()
    {
        // Prevent instantiation
    }
    
    public static float sqrt(float paramFloat)
    {
        return (float) Math.sqrt(paramFloat);
    }

    public static float sin(float paramFloat)
    {
        return SINE_TABLE[((int) (paramFloat * 10430.378F) & 0xFFFF)];
    }

    public static float cos(float paramFloat)
    {
        return SINE_TABLE[((int) (paramFloat * 10430.378F + 16384.0F) & 0xFFFF)];
    }

    public static int floor(double d0)
    {
        int i = (int) d0;
        return d0 < i ? i - 1 : i;
    }

    public static long floor_double_long(double d)
    {
        long l = (long) d;
        return d >= l ? l : l - 1L;
    }

    public static int abs(int number)
    {
        // OPTIMIZATION: Use Math.abs or bit manipulation
        return number >= 0 ? number : -number;
    }

    static
    {
        for (int i = 0; i < 65536; i++)
        {
            SINE_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0D / 65536.0D);
        }
    }

    public static int ceil(float floatNumber)
    {
        int truncated = (int) floatNumber;
        return floatNumber > truncated ? truncated + 1 : truncated;
    }

    public static int clamp(int check, int min, int max)
    {
        return check > max ? max : (check < min ? min : check);
    }
    
    /**
     * Modulus, rather than java's modulo (%)
     * which does a remainder operation.
     */
    public static int mod(int x, int y)
    {
        int result = x % y;
        return result < 0 ? result + y : result; // OPTIMIZATION: Simplified
    }
    
    public static boolean tryParseInt(String value)
    {  
        try {  
            Integer.parseInt(value);  
            return true;  
        } catch (NumberFormatException e) {  
            return false;  
        }  
    }
}
