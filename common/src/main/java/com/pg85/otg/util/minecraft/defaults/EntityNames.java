package com.pg85.otg.util.minecraft.defaults;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains a lot of alternative mob names. The implementation should support
 * these names, along with the other names that are available on the current
 * platform.
 * 
 * Now supports modded entities with custom namespaces (e.g., "modid:creature").
 */
public enum EntityNames
{
    // See: net.minecraft.entity.EntityList for internal mob names list

    // Aliases don't need to contain underscores - the toInternalName()
    // function removes those when looking up anyway
    
    // Mobs

    BAT("bat", "bat"),
    BLAZE("blaze", "blaze"),
    CAVE_SPIDER("cave_spider", "cavespider"),
    CHICKEN("chicken", "chicken"),
    COW("cow", "cow"),
    CREEPER("creeper", "creeper"),
    DONKEY("donkey", "donkey"),
    ELDER_GUARDIAN("elder_guardian", "elderguardian"),
    ENDER_DRAGON("ender_dragon", "enderdragon", "dragon"),
    ENDERMAN("enderman", "enderman"),
    ENDERMITE("endermite", "endermite"),
    EVOCATION_ILLAGER("evocation_illager", "evocationillager", "evoker"),
    GHAST("ghast", "ghast"),
    GIANT("giant", "giant", "giantzombie", "zombiegiant"),
    GUARDIAN("guardian", "guardian"),
    HORSE("horse", "horse"),
    HUSK("husk", "husk"),
    LLAMA("llama", "llama"),
    MAGMA_CUBE("magma_cube", "magmaslime", "lavaslime", "magmacube"),
    MULE("mule", "mule"),
    MOOSHROOM("mooshroom", "mushroomcow", "mooshroom"),
    OCELOT("ocelot", "ozelot", "ocelot"),
    ILLUSION_ILLAGER("illusion_illager", "illusionillager", "illusioner"),
    PARROT("parrot", "parrot"),
    PIG("pig", "pig"),
    POLAR_BEAR("polar_bear", "polarbear"),
    RABBIT("rabbit", "rabbit"),
    SHEEP("sheep", "sheep"),
    SHULKER("shulker", "shulker"),
    SILVERFISH("silverfish", "silverfish"),
    SKELETON("skeleton", "skeleton"),
    SKELETON_HORSE("skeleton_horse", "skeletonhorse"),
    SLIME("slime", "slime"),
    SNOWMAN("snowman", "snowman"),
    SPIDER("spider", "spider"),
    SQUID("squid", "squid"),
    STRAY("stray", "stray"),
    VEX("vex", "vex"),    
    VILLAGER("villager", "villager"),
    VILLAGER_GOLEM("villager_golem", "villagergolem", "irongolem"),
    VINDICATION_ILLAGER("vindication_illager", "vindicationillager", "vindicator"),
    WITCH("witch", "witch"),
    WITHER("wither", "witherboss", "wither"),
    WITHER_SKELETON("wither_skeleton", "witherskeleton"),
    WOLF("wolf", "wolf"),
    ZOMBIE("zombie", "zombie"),
    ZOMBIE_HORSE("zombie_horse", "zombiehorse", "horsezombie"),
    ZOMBIE_PIGMAN("zombie_pigman", "zombiepigman", "pigzombie"),
    ZOMBIE_VILLAGER("zombie_villager", "zombievillager", "villagerzombie"),

    // Projectiles
    
    ARROW("arrow", "arrow"),
    DRAGON_FIREBALL("dragon_fireball", "dragonfireball"),
    EGG("egg", "egg", "thrownegg"),
    ENDER_PEARL("ender_pearl", "enderpearl", "thrownenderpearl"),
    EVOKATION_FANGS("evocation_fangs", "evocationfangs", "evokerfangs"),
    FIREBALL("fireball", "fireball"),
    FIREWORKS_ROCKET("fireworks_rocket", "fireworksrocket", "firework", "fireworksrocketentity"),
    LLAMA_SPIT("llama_spit","llamaspit"),
    SHULKER_BULLET("shulker_bullet", "shulkerbullet"),
    SMALL_FIREBALL("small_fireball","smallfireball"),
    SNOWBALL("snowball", "snowball"),
    SPECTRAL_ARROW("spectral_arrow", "spectralarrow"),
    XP_BOTTLE("xp_bottle","xpbottle", "thrownexpbottle"),
    XP_ORB("xp_orb", "xp_orb", "experienceorb"),

    // Entities
    
    AREA_EFFECT_CLOUD("area_effect_cloud", "areaeffectcloud"),
    ARMOR_STAND("armor_stand", "armorstand"),
    BOAT("boat", "boat"),
    CHEST_MINECART("chest_minecart", "chestminecart", "minecartchest"),
    COMMANDBLOCK_MINECART("commandblock_minecart", "commandblockminecart", "minecartcommand"),
    ENDER_CRYSTAL("ender_crystal", "endercrystal"),
    EYE_OF_ENDER_SIGNAL("eye_of_ender_signal", "eye_of_ender_signal","endersignal"),
    FALLING_BLOCK("falling_block", "fallingblock", "fallingsand"),
    FURNACE_MINECART("furnace_minecart","furnaceminecart","minecartfurnace"),
    HOPPER_MINECART("hopper_minecart", "hopperminecart", "minecarthopper"),
    ITEM("item", "item", "droppeditem"),
    ITEM_FRAME("item_frame", "itemframe"),
    LEASH_KNOT("leash_knot", "leashknot", "leashhitch"),
    MINECART("minecart", "minecart"),
    PAINTING("painting", "painting"),
    POTION("potion", "potion", "splashpotion", "thrownpotion"),
    SPAWNER_MINECART("spawner_minecart", "spawnerminecart", "minecartmobspawner"),
    TNT("tnt", "tnt", "primedtnt"),
    TNT_MINECART("tnt_minecart", "tntminecart", "minecarttnt"),
    WITHER_SKULL("wither_skull", "witherskull");
   
    // Contains all aliases (alias, internalName)
    private static final Map<String, String> MOB_ALIASES = new HashMap<String, String>();

    // Auto-register all aliases in the enum
    static
    {
        for (EntityNames alt : EntityNames.values())
        {
            register(alt.internalMinecraftName, alt.aliases);
        }
    }

    /**
     * Returns the internal name of the mob. If it can't be found, it returns
     * the alias. Now supports modded entities with custom namespaces.
     *
     * @param alias The alias (e.g., "creeper", "minecraft:creeper", "modid:creature")
     * @return The internal name with namespace, or the original alias if not found
     */
    public static String toInternalName(String alias)
    {
        if (alias == null || alias.trim().isEmpty())
        {
            return alias;
        }
        
        String trimmedAlias = alias.trim().toLowerCase();
        
        // If it already contains a namespace and it's NOT minecraft, assume it's a modded entity
        if (trimmedAlias.contains(":"))
        {
            String[] parts = trimmedAlias.split(":", 2);
            String namespace = parts[0];
            
            // For non-minecraft namespaces, return as-is (modded entity)
            if (!"minecraft".equals(namespace))
            {
                return trimmedAlias;
            }
            
            // For minecraft namespace, try to find in aliases
            String entityPath = parts[1];
            String normalized = normalizeEntityName(entityPath);
            
            // Check if this exact minecraft: entity exists in our aliases
            for (Map.Entry<String, String> entry : MOB_ALIASES.entrySet())
            {
                if (normalizeEntityName(entry.getKey()).equals("minecraft:" + normalized))
                {
                    return entry.getValue();
                }
            }
            
            // Not found in aliases, return as-is
            return trimmedAlias;
        }
        
        // No namespace provided - try to find in vanilla aliases
        String normalized = normalizeEntityName(trimmedAlias);
        
        for (Map.Entry<String, String> entry : MOB_ALIASES.entrySet())
        {
            if (normalizeEntityName(entry.getKey()).equals("minecraft:" + normalized))
            {
                return entry.getValue();
            }
        }
        
        // Not found in aliases - assume vanilla entity and add minecraft namespace
        return "minecraft:" + trimmedAlias;
    }
    
    /**
     * Normalizes an entity name by removing common variations.
     * This helps match different naming conventions.
     * 
     * @param name The entity name to normalize
     * @return The normalized name
     */
    private static String normalizeEntityName(String name)
    {
        return name.toLowerCase()
                   .replace("_", "")
                   .replace("entity", "")
                   .trim();
    }
    
    /**
     * Checks if a given entity name is a valid vanilla Minecraft entity.
     * 
     * @param entityName The entity name to check (with or without namespace)
     * @return True if the entity is a known vanilla entity, false otherwise
     */
    public static boolean isVanillaEntity(String entityName)
    {
        if (entityName == null || entityName.trim().isEmpty())
        {
            return false;
        }
        
        String normalized = normalizeEntityName(entityName.replace("minecraft:", ""));
        
        for (String key : MOB_ALIASES.keySet())
        {
            if (normalizeEntityName(key).equals("minecraft:" + normalized))
            {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Register aliases here
     *
     * @param internalMinecraftName The internal Minecraft mob id, for example Ozelot
     * @param aliases               The alias, for example Ocelot
     */
    private static void register(String internalMinecraftName, String... aliases)
    {
        for (String alias : aliases)
        {
            MOB_ALIASES.put("minecraft:" + alias, "minecraft:" + internalMinecraftName);
        }
    }

    private final String[] aliases;
    private final String internalMinecraftName;

    EntityNames(String internalMinecraftName, String... aliases)
    {
        this.internalMinecraftName = internalMinecraftName;
        this.aliases = aliases;
    }

    /**
     * Gets the internal Minecraft name of this mob.
     * @return The internal Minecraft name.
     */
    public String getInternalName()
    {
        return "minecraft:" + this.internalMinecraftName;
    }
}
