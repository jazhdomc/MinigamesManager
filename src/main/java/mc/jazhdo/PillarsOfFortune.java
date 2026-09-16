package mc.jazhdo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class PillarsOfFortune extends Game {
    private enum STATE {WAITING, PLAYTIME, SCORES}
    private STATE currentState = STATE.WAITING;
    private final ItemStack newGame, lobby;
    private Random random = new Random();
    private int countdown;
    private final Set<Player> alive = new LinkedHashSet<>(), dead = new LinkedHashSet<>();
    private final List<Location> spawns = new ArrayList<>();
    private boolean countingDown = false;
    private final Location lobbySpawn;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final BossBar bossBar;
    private final BukkitScheduler scheduler;
    private final String prefix = ChatColor.GOLD + "[Pillars of Fortune] ";
    private BukkitTask generationLoop;
    private final List<ItemStack> multiLoot = List.of(
        new ItemStack(Material.STONE),
        new ItemStack(Material.GRASS),
        new ItemStack(Material.DIRT),
        new ItemStack(Material.COBBLESTONE),
        new ItemStack(Material.WOOD, 1, (short) 0),
        new ItemStack(Material.WOOD, 1, (short) 1),
        new ItemStack(Material.WOOD, 1, (short) 2),
        new ItemStack(Material.WOOD, 1, (short) 3),
        new ItemStack(Material.WOOD, 1, (short) 4),
        new ItemStack(Material.WOOD, 1, (short) 5),
        new ItemStack(Material.SAPLING, 1, (short) 0),
        new ItemStack(Material.SAPLING, 1, (short) 1),
        new ItemStack(Material.SAPLING, 1, (short) 2),
        new ItemStack(Material.SAPLING, 1, (short) 3),
        new ItemStack(Material.SAPLING, 1, (short) 4),
        new ItemStack(Material.SAPLING, 1, (short) 5),
        new ItemStack(Material.SAND),
        new ItemStack(Material.GRAVEL),
        new ItemStack(Material.GOLD_ORE),
        new ItemStack(Material.IRON_ORE),
        new ItemStack(Material.COAL_ORE),
        new ItemStack(Material.LOG, 1, (short) 0),
        new ItemStack(Material.LOG, 1, (short) 1),
        new ItemStack(Material.LOG, 1, (short) 2),
        new ItemStack(Material.LOG, 1, (short) 3),
        new ItemStack(Material.LEAVES, 1, (short) 0),
        new ItemStack(Material.LEAVES, 1, (short) 1),
        new ItemStack(Material.LEAVES, 1, (short) 2),
        new ItemStack(Material.LEAVES, 1, (short) 3),
        new ItemStack(Material.SPONGE),
        new ItemStack(Material.GLASS),
        new ItemStack(Material.LAPIS_ORE),
        new ItemStack(Material.LAPIS_BLOCK),
        new ItemStack(Material.DISPENSER),
        new ItemStack(Material.SANDSTONE, 1, (short) 0),
        new ItemStack(Material.SANDSTONE, 1, (short) 1),
        new ItemStack(Material.SANDSTONE, 1, (short) 2),
        new ItemStack(Material.NOTE_BLOCK),
        new ItemStack(Material.POWERED_RAIL),
        new ItemStack(Material.DETECTOR_RAIL),
        new ItemStack(Material.PISTON_STICKY_BASE),
        new ItemStack(Material.WEB),
        new ItemStack(Material.LONG_GRASS, 1, (short) 0),
        new ItemStack(Material.LONG_GRASS, 1, (short) 1),
        new ItemStack(Material.LONG_GRASS, 1, (short) 2),
        new ItemStack(Material.DEAD_BUSH),
        new ItemStack(Material.PISTON_BASE),
        new ItemStack(Material.WOOL, 1, (short) 0),
        new ItemStack(Material.WOOL, 1, (short) 1),
        new ItemStack(Material.WOOL, 1, (short) 2),
        new ItemStack(Material.WOOL, 1, (short) 3),
        new ItemStack(Material.WOOL, 1, (short) 4),
        new ItemStack(Material.WOOL, 1, (short) 5),
        new ItemStack(Material.WOOL, 1, (short) 6),
        new ItemStack(Material.WOOL, 1, (short) 7),
        new ItemStack(Material.WOOL, 1, (short) 8),
        new ItemStack(Material.WOOL, 1, (short) 9),
        new ItemStack(Material.WOOL, 1, (short) 10),
        new ItemStack(Material.WOOL, 1, (short) 11),
        new ItemStack(Material.WOOL, 1, (short) 12),
        new ItemStack(Material.WOOL, 1, (short) 13),
        new ItemStack(Material.WOOL, 1, (short) 14),
        new ItemStack(Material.WOOL, 1, (short) 15),
        new ItemStack(Material.YELLOW_FLOWER),
        new ItemStack(Material.RED_ROSE),
        new ItemStack(Material.BROWN_MUSHROOM),
        new ItemStack(Material.RED_MUSHROOM),
        new ItemStack(Material.GOLD_BLOCK),
        new ItemStack(Material.IRON_BLOCK),
        new ItemStack(Material.STEP, 1, (short) 0),
        new ItemStack(Material.STEP, 1, (short) 1), // #2 is old problematic wood slab
        new ItemStack(Material.STEP, 1, (short) 3),
        new ItemStack(Material.STEP, 1, (short) 4),
        new ItemStack(Material.STEP, 1, (short) 5),
        new ItemStack(Material.STEP, 1, (short) 6),
        new ItemStack(Material.STEP, 1, (short) 7),
        new ItemStack(Material.BRICK),
        new ItemStack(Material.TNT),
        new ItemStack(Material.BOOKSHELF),
        new ItemStack(Material.MOSSY_COBBLESTONE),
        new ItemStack(Material.OBSIDIAN),
        new ItemStack(Material.TORCH),
        new ItemStack(Material.WOOD_STAIRS),
        new ItemStack(Material.CHEST),
        new ItemStack(Material.DIAMOND_ORE),
        new ItemStack(Material.DIAMOND_BLOCK),
        new ItemStack(Material.WORKBENCH),
        new ItemStack(Material.SOIL),
        new ItemStack(Material.FURNACE),
        new ItemStack(Material.LADDER),
        new ItemStack(Material.RAILS),
        new ItemStack(Material.COBBLESTONE_STAIRS),
        new ItemStack(Material.LEVER),
        new ItemStack(Material.STONE_PLATE),
        new ItemStack(Material.WOOD_PLATE),
        new ItemStack(Material.REDSTONE_ORE),
        new ItemStack(Material.REDSTONE_TORCH_ON),
        new ItemStack(Material.STONE_BUTTON),
        new ItemStack(Material.SNOW),
        new ItemStack(Material.ICE),
        new ItemStack(Material.SNOW_BLOCK),
        new ItemStack(Material.CACTUS),
        new ItemStack(Material.CLAY),
        new ItemStack(Material.JUKEBOX),
        new ItemStack(Material.FENCE),
        new ItemStack(Material.PUMPKIN),
        new ItemStack(Material.NETHERRACK),
        new ItemStack(Material.SOUL_SAND),
        new ItemStack(Material.GLOWSTONE),
        new ItemStack(Material.JACK_O_LANTERN),
        new ItemStack(Material.STAINED_GLASS),
        new ItemStack(Material.TRAP_DOOR),
        new ItemStack(Material.MONSTER_EGGS, 1, (short) 0),
        new ItemStack(Material.MONSTER_EGGS, 1, (short) 1),
        new ItemStack(Material.MONSTER_EGGS, 1, (short) 2),
        new ItemStack(Material.SMOOTH_BRICK, 1, (short) 0),
        new ItemStack(Material.SMOOTH_BRICK, 1, (short) 1),
        new ItemStack(Material.SMOOTH_BRICK, 1, (short) 2),
        new ItemStack(Material.SMOOTH_BRICK, 1, (short) 3),
        new ItemStack(Material.HUGE_MUSHROOM_1),
        new ItemStack(Material.HUGE_MUSHROOM_2),
        new ItemStack(Material.IRON_FENCE),
        new ItemStack(Material.THIN_GLASS),
        new ItemStack(Material.MELON_BLOCK),
        new ItemStack(Material.VINE),
        new ItemStack(Material.FENCE_GATE),
        new ItemStack(Material.BRICK_STAIRS),
        new ItemStack(Material.SMOOTH_STAIRS),
        new ItemStack(Material.MYCEL),
        new ItemStack(Material.WATER_LILY),
        new ItemStack(Material.NETHER_BRICK),
        new ItemStack(Material.NETHER_FENCE),
        new ItemStack(Material.NETHER_BRICK_STAIRS),
        new ItemStack(Material.ENCHANTMENT_TABLE),
        new ItemStack(Material.ENDER_STONE),
        new ItemStack(Material.REDSTONE_LAMP_OFF),
        new ItemStack(Material.WOOD_STEP, 1, (short) 0),
        new ItemStack(Material.WOOD_STEP, 1, (short) 1),
        new ItemStack(Material.WOOD_STEP, 1, (short) 2),
        new ItemStack(Material.WOOD_STEP, 1, (short) 3),
        new ItemStack(Material.WOOD_STEP, 1, (short) 4),
        new ItemStack(Material.WOOD_STEP, 1, (short) 5),
        new ItemStack(Material.SANDSTONE_STAIRS),
        new ItemStack(Material.EMERALD_ORE),
        new ItemStack(Material.ENDER_CHEST),
        new ItemStack(Material.TRIPWIRE_HOOK),
        new ItemStack(Material.EMERALD_BLOCK),
        new ItemStack(Material.SPRUCE_WOOD_STAIRS),
        new ItemStack(Material.BIRCH_WOOD_STAIRS),
        new ItemStack(Material.JUNGLE_WOOD_STAIRS),
        new ItemStack(Material.BEACON),
        new ItemStack(Material.COBBLE_WALL),
        new ItemStack(Material.WOOD_BUTTON),
        new ItemStack(Material.ANVIL),
        new ItemStack(Material.TRAPPED_CHEST),
        new ItemStack(Material.GOLD_PLATE),
        new ItemStack(Material.IRON_PLATE),
        new ItemStack(Material.DAYLIGHT_DETECTOR),
        new ItemStack(Material.REDSTONE_BLOCK),
        new ItemStack(Material.QUARTZ_ORE),
        new ItemStack(Material.HOPPER),
        new ItemStack(Material.QUARTZ_BLOCK),
        new ItemStack(Material.QUARTZ_STAIRS),
        new ItemStack(Material.ACTIVATOR_RAIL),
        new ItemStack(Material.DROPPER),
        new ItemStack(Material.STAINED_CLAY),
        new ItemStack(Material.STAINED_GLASS_PANE),
        new ItemStack(Material.LEAVES_2, 1, (short) 0),
        new ItemStack(Material.LEAVES_2, 1, (short) 1),
        new ItemStack(Material.LOG_2, 1, (short) 0),
        new ItemStack(Material.LOG_2, 1, (short) 1),
        new ItemStack(Material.ACACIA_STAIRS),
        new ItemStack(Material.DARK_OAK_STAIRS),
        new ItemStack(Material.SLIME_BLOCK),
        new ItemStack(Material.IRON_TRAPDOOR),
        new ItemStack(Material.PRISMARINE),
        new ItemStack(Material.SEA_LANTERN),
        new ItemStack(Material.HAY_BLOCK),
        new ItemStack(Material.CARPET),
        new ItemStack(Material.HARD_CLAY),
        new ItemStack(Material.COAL_BLOCK),
        new ItemStack(Material.PACKED_ICE),
        new ItemStack(Material.DOUBLE_PLANT),
        new ItemStack(Material.RED_SANDSTONE),
        new ItemStack(Material.RED_SANDSTONE_STAIRS),
        new ItemStack(Material.STONE_SLAB2),
        new ItemStack(Material.SPRUCE_FENCE_GATE),
        new ItemStack(Material.BIRCH_FENCE_GATE),
        new ItemStack(Material.JUNGLE_FENCE_GATE),
        new ItemStack(Material.DARK_OAK_FENCE_GATE),
        new ItemStack(Material.ACACIA_FENCE_GATE),
        new ItemStack(Material.SPRUCE_FENCE),
        new ItemStack(Material.BIRCH_FENCE),
        new ItemStack(Material.JUNGLE_FENCE),
        new ItemStack(Material.DARK_OAK_FENCE),
        new ItemStack(Material.ACACIA_FENCE),
        new ItemStack(Material.END_ROD),
        new ItemStack(Material.CHORUS_PLANT),
        new ItemStack(Material.CHORUS_FLOWER),
        new ItemStack(Material.PURPUR_BLOCK),
        new ItemStack(Material.PURPUR_PILLAR),
        new ItemStack(Material.PURPUR_STAIRS),
        new ItemStack(Material.PURPUR_SLAB),
        new ItemStack(Material.END_BRICKS),
        new ItemStack(Material.GRASS_PATH),
        new ItemStack(Material.MAGMA),
        new ItemStack(Material.NETHER_WART_BLOCK),
        new ItemStack(Material.RED_NETHER_BRICK),
        new ItemStack(Material.BONE_BLOCK),
        new ItemStack(Material.OBSERVER),
        new ItemStack(Material.WHITE_GLAZED_TERRACOTTA),
        new ItemStack(Material.ORANGE_GLAZED_TERRACOTTA),
        new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA),
        new ItemStack(Material.LIGHT_BLUE_GLAZED_TERRACOTTA),
        new ItemStack(Material.YELLOW_GLAZED_TERRACOTTA),
        new ItemStack(Material.LIME_GLAZED_TERRACOTTA),
        new ItemStack(Material.PINK_GLAZED_TERRACOTTA),
        new ItemStack(Material.GRAY_GLAZED_TERRACOTTA),
        new ItemStack(Material.SILVER_GLAZED_TERRACOTTA),
        new ItemStack(Material.CYAN_GLAZED_TERRACOTTA),
        new ItemStack(Material.PURPLE_GLAZED_TERRACOTTA),
        new ItemStack(Material.BLUE_GLAZED_TERRACOTTA),
        new ItemStack(Material.BROWN_GLAZED_TERRACOTTA),
        new ItemStack(Material.GREEN_GLAZED_TERRACOTTA),
        new ItemStack(Material.RED_GLAZED_TERRACOTTA),
        new ItemStack(Material.BLACK_GLAZED_TERRACOTTA),
        new ItemStack(Material.CONCRETE),
        new ItemStack(Material.CONCRETE_POWDER),
        new ItemStack(Material.APPLE),
        new ItemStack(Material.ARROW),
        new ItemStack(Material.COAL, 1, (short) 0),
        new ItemStack(Material.COAL, 1, (short) 1),
        new ItemStack(Material.DIAMOND),
        new ItemStack(Material.IRON_INGOT),
        new ItemStack(Material.GOLD_INGOT),
        new ItemStack(Material.STICK),
        new ItemStack(Material.BOWL),
        new ItemStack(Material.STRING),
        new ItemStack(Material.FEATHER),
        new ItemStack(Material.SULPHUR),
        new ItemStack(Material.SEEDS),
        new ItemStack(Material.WHEAT),
        new ItemStack(Material.BREAD),
        new ItemStack(Material.FLINT),
        new ItemStack(Material.PORK),
        new ItemStack(Material.GRILLED_PORK),
        new ItemStack(Material.PAINTING),
        new ItemStack(Material.GOLDEN_APPLE),
        new ItemStack(Material.SIGN),
        new ItemStack(Material.WOOD_DOOR),
        new ItemStack(Material.BUCKET),
        new ItemStack(Material.IRON_DOOR),
        new ItemStack(Material.REDSTONE),
        new ItemStack(Material.SNOW_BALL),
        new ItemStack(Material.LEATHER),
        new ItemStack(Material.CLAY_BRICK),
        new ItemStack(Material.CLAY_BALL),
        new ItemStack(Material.SUGAR_CANE),
        new ItemStack(Material.PAPER),
        new ItemStack(Material.BOOK),
        new ItemStack(Material.SLIME_BALL),
        new ItemStack(Material.EGG),
        new ItemStack(Material.COMPASS),
        new ItemStack(Material.WATCH),
        new ItemStack(Material.GLOWSTONE_DUST),
        new ItemStack(Material.RAW_FISH),
        new ItemStack(Material.COOKED_FISH),
        new ItemStack(Material.INK_SACK, 1, (short) 0),
        new ItemStack(Material.INK_SACK, 1, (short) 1),
        new ItemStack(Material.INK_SACK, 1, (short) 2),
        new ItemStack(Material.INK_SACK, 1, (short) 3),
        new ItemStack(Material.INK_SACK, 1, (short) 4),
        new ItemStack(Material.INK_SACK, 1, (short) 5),
        new ItemStack(Material.INK_SACK, 1, (short) 6),
        new ItemStack(Material.INK_SACK, 1, (short) 7),
        new ItemStack(Material.INK_SACK, 1, (short) 8),
        new ItemStack(Material.INK_SACK, 1, (short) 9),
        new ItemStack(Material.INK_SACK, 1, (short) 10),
        new ItemStack(Material.INK_SACK, 1, (short) 11),
        new ItemStack(Material.INK_SACK, 1, (short) 12),
        new ItemStack(Material.INK_SACK, 1, (short) 13),
        new ItemStack(Material.INK_SACK, 1, (short) 14),
        new ItemStack(Material.INK_SACK, 1, (short) 15),
        new ItemStack(Material.BONE),
        new ItemStack(Material.SUGAR),
        new ItemStack(Material.DIODE),
        new ItemStack(Material.COOKIE),
        new ItemStack(Material.MELON),
        new ItemStack(Material.PUMPKIN_SEEDS),
        new ItemStack(Material.MELON_SEEDS),
        new ItemStack(Material.RAW_BEEF),
        new ItemStack(Material.COOKED_BEEF),
        new ItemStack(Material.RAW_CHICKEN),
        new ItemStack(Material.COOKED_CHICKEN),
        new ItemStack(Material.ROTTEN_FLESH),
        new ItemStack(Material.ENDER_PEARL),
        new ItemStack(Material.BLAZE_ROD),
        new ItemStack(Material.GHAST_TEAR),
        new ItemStack(Material.GOLD_NUGGET),
        new ItemStack(Material.NETHER_STALK),
        new ItemStack(Material.GLASS_BOTTLE),
        new ItemStack(Material.SPIDER_EYE),
        new ItemStack(Material.FERMENTED_SPIDER_EYE),
        new ItemStack(Material.BLAZE_POWDER),
        new ItemStack(Material.MAGMA_CREAM),
        new ItemStack(Material.BREWING_STAND_ITEM),
        new ItemStack(Material.CAULDRON_ITEM),
        new ItemStack(Material.EYE_OF_ENDER),
        new ItemStack(Material.SPECKLED_MELON),
        new ItemStack(Material.EXP_BOTTLE),
        new ItemStack(Material.FIREBALL),
        new ItemStack(Material.WRITTEN_BOOK),
        new ItemStack(Material.EMERALD),
        new ItemStack(Material.ITEM_FRAME),
        new ItemStack(Material.FLOWER_POT_ITEM),
        new ItemStack(Material.CARROT_ITEM),
        new ItemStack(Material.POTATO_ITEM),
        new ItemStack(Material.BAKED_POTATO),
        new ItemStack(Material.POISONOUS_POTATO),
        new ItemStack(Material.EMPTY_MAP),
        new ItemStack(Material.GOLDEN_CARROT),
        new ItemStack(Material.SKULL_ITEM),
        new ItemStack(Material.NETHER_STAR),
        new ItemStack(Material.PUMPKIN_PIE),
        new ItemStack(Material.FIREWORK),
        new ItemStack(Material.FIREWORK_CHARGE),
        new ItemStack(Material.REDSTONE_COMPARATOR),
        new ItemStack(Material.NETHER_BRICK_ITEM),
        new ItemStack(Material.QUARTZ),
        new ItemStack(Material.PRISMARINE_SHARD),
        new ItemStack(Material.PRISMARINE_CRYSTALS),
        new ItemStack(Material.RABBIT),
        new ItemStack(Material.COOKED_RABBIT),
        new ItemStack(Material.RABBIT_FOOT),
        new ItemStack(Material.RABBIT_HIDE),
        new ItemStack(Material.ARMOR_STAND),
        new ItemStack(Material.LEASH),
        new ItemStack(Material.NAME_TAG),
        new ItemStack(Material.MUTTON),
        new ItemStack(Material.COOKED_MUTTON),
        new ItemStack(Material.BANNER),
        new ItemStack(Material.END_CRYSTAL),
        new ItemStack(Material.SPRUCE_DOOR_ITEM),
        new ItemStack(Material.BIRCH_DOOR_ITEM),
        new ItemStack(Material.JUNGLE_DOOR_ITEM),
        new ItemStack(Material.ACACIA_DOOR_ITEM),
        new ItemStack(Material.DARK_OAK_DOOR_ITEM),
        new ItemStack(Material.CHORUS_FRUIT),
        new ItemStack(Material.CHORUS_FRUIT_POPPED),
        new ItemStack(Material.BEETROOT),
        new ItemStack(Material.BEETROOT_SEEDS),
        new ItemStack(Material.DRAGONS_BREATH),
        new ItemStack(Material.SPECTRAL_ARROW),
        new ItemStack(Material.TIPPED_ARROW),
        new ItemStack(Material.SHULKER_SHELL),
        new ItemStack(Material.IRON_NUGGET)
    );
    private final List<ItemStack> singleLoot = List.of(
        new ItemStack(Material.WHITE_SHULKER_BOX),
        new ItemStack(Material.ORANGE_SHULKER_BOX),
        new ItemStack(Material.MAGENTA_SHULKER_BOX),
        new ItemStack(Material.LIGHT_BLUE_SHULKER_BOX),
        new ItemStack(Material.YELLOW_SHULKER_BOX),
        new ItemStack(Material.LIME_SHULKER_BOX),
        new ItemStack(Material.PINK_SHULKER_BOX),
        new ItemStack(Material.GRAY_SHULKER_BOX),
        new ItemStack(Material.SILVER_SHULKER_BOX),
        new ItemStack(Material.CYAN_SHULKER_BOX),
        new ItemStack(Material.PURPLE_SHULKER_BOX),
        new ItemStack(Material.BLUE_SHULKER_BOX),
        new ItemStack(Material.BROWN_SHULKER_BOX),
        new ItemStack(Material.GREEN_SHULKER_BOX),
        new ItemStack(Material.RED_SHULKER_BOX),
        new ItemStack(Material.BLACK_SHULKER_BOX),
        new ItemStack(Material.IRON_SPADE),
        new ItemStack(Material.IRON_PICKAXE),
        new ItemStack(Material.IRON_AXE),
        new ItemStack(Material.FLINT_AND_STEEL),
        new ItemStack(Material.BOW),
        new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.WOOD_SWORD),
        new ItemStack(Material.WOOD_SPADE),
        new ItemStack(Material.WOOD_PICKAXE),
        new ItemStack(Material.WOOD_AXE),
        new ItemStack(Material.STONE_SWORD),
        new ItemStack(Material.STONE_SPADE),
        new ItemStack(Material.STONE_PICKAXE),
        new ItemStack(Material.STONE_AXE),
        new ItemStack(Material.DIAMOND_SWORD),
        new ItemStack(Material.DIAMOND_SPADE),
        new ItemStack(Material.DIAMOND_PICKAXE),
        new ItemStack(Material.DIAMOND_AXE),
        new ItemStack(Material.MUSHROOM_SOUP),
        new ItemStack(Material.GOLD_SWORD),
        new ItemStack(Material.GOLD_SPADE),
        new ItemStack(Material.GOLD_PICKAXE),
        new ItemStack(Material.GOLD_AXE),
        new ItemStack(Material.WOOD_HOE),
        new ItemStack(Material.STONE_HOE),
        new ItemStack(Material.IRON_HOE),
        new ItemStack(Material.DIAMOND_HOE),
        new ItemStack(Material.GOLD_HOE),
        new ItemStack(Material.LEATHER_HELMET),
        new ItemStack(Material.LEATHER_CHESTPLATE),
        new ItemStack(Material.LEATHER_LEGGINGS),
        new ItemStack(Material.LEATHER_BOOTS),
        new ItemStack(Material.CHAINMAIL_HELMET),
        new ItemStack(Material.CHAINMAIL_CHESTPLATE),
        new ItemStack(Material.CHAINMAIL_LEGGINGS),
        new ItemStack(Material.CHAINMAIL_BOOTS),
        new ItemStack(Material.IRON_HELMET),
        new ItemStack(Material.IRON_CHESTPLATE),
        new ItemStack(Material.IRON_LEGGINGS),
        new ItemStack(Material.IRON_BOOTS),
        new ItemStack(Material.DIAMOND_HELMET),
        new ItemStack(Material.DIAMOND_CHESTPLATE),
        new ItemStack(Material.DIAMOND_LEGGINGS),
        new ItemStack(Material.DIAMOND_BOOTS),
        new ItemStack(Material.GOLD_HELMET),
        new ItemStack(Material.GOLD_CHESTPLATE),
        new ItemStack(Material.GOLD_LEGGINGS),
        new ItemStack(Material.GOLD_BOOTS),
        new ItemStack(Material.WATER_BUCKET),
        new ItemStack(Material.LAVA_BUCKET),
        new ItemStack(Material.MINECART),
        new ItemStack(Material.SADDLE),
        new ItemStack(Material.BOAT),
        new ItemStack(Material.MILK_BUCKET),
        new ItemStack(Material.STORAGE_MINECART),
        new ItemStack(Material.POWERED_MINECART),
        new ItemStack(Material.FISHING_ROD),
        new ItemStack(Material.CAKE),
        new ItemStack(Material.BED),
        new ItemStack(Material.SHEARS),
        new ItemStack(Material.BOOK_AND_QUILL),
        new ItemStack(Material.CARROT_STICK),
        new ItemStack(Material.ENCHANTED_BOOK),
        new ItemStack(Material.EXPLOSIVE_MINECART),
        new ItemStack(Material.HOPPER_MINECART),
        new ItemStack(Material.RABBIT_STEW),
        new ItemStack(Material.IRON_BARDING),
        new ItemStack(Material.GOLD_BARDING),
        new ItemStack(Material.DIAMOND_BARDING),
        new ItemStack(Material.BEETROOT_SOUP),
        new ItemStack(Material.SHIELD),
        new ItemStack(Material.ELYTRA),
        new ItemStack(Material.BOAT_SPRUCE),
        new ItemStack(Material.BOAT_BIRCH),
        new ItemStack(Material.BOAT_JUNGLE),
        new ItemStack(Material.BOAT_ACACIA),
        new ItemStack(Material.BOAT_DARK_OAK),
        new ItemStack(Material.TOTEM),
        new ItemStack(Material.GOLD_RECORD),
        new ItemStack(Material.GREEN_RECORD),
        new ItemStack(Material.RECORD_3),
        new ItemStack(Material.RECORD_4),
        new ItemStack(Material.RECORD_5),
        new ItemStack(Material.RECORD_6),
        new ItemStack(Material.RECORD_7),
        new ItemStack(Material.RECORD_8),
        new ItemStack(Material.RECORD_9),
        new ItemStack(Material.RECORD_10),
        new ItemStack(Material.RECORD_11),
        new ItemStack(Material.RECORD_12)
    );

    public PillarsOfFortune(GameArgs args) {
        super(args);

        // Get all the spawn locations in a list
        int x = gameConfig.getInt("position.x"), z = gameConfig.getInt("position.z"), y = gameConfig.getInt("player"), v1 = x, v2 = z;
        for (int i = 0; i < 4; i++) {
            if (i == 2) v1 = -v1;
            v2 = -v2;
            spawns.add(new Location(world, v1 + 0.5, y, v2 + 0.5));
        }
        v1 = z;
        v2 = x;
        for (int i = 0; i < 4; i++) {
            if (i == 2) v1 = -v1;
            v2 = -v2;
            spawns.add(new Location(world, v1 + 0.5, y, v2 + 0.5));
        }

        // New game selection menu shown after dying
        newGame = new ItemStack(Material.IRON_AXE);
        ItemMeta newGameMeta = newGame.getItemMeta();
        newGameMeta.setDisplayName(ChatColor.RESET + "New Game");
        newGameMeta.setLore(List.of(ChatColor.RESET + "Opens up the game selection menu"));
        newGame.setItemMeta(newGameMeta);

        // Lobby teleportation method shown after dying
        lobby = new ItemStack(Material.APPLE);
        ItemMeta lobbyMeta = lobby.getItemMeta();
        lobbyMeta.setDisplayName(ChatColor.RESET + "Lobby");
        lobbyMeta.setLore(List.of(ChatColor.RESET + "Teleports you to the Minigames lobby."));
        lobby.setItemMeta(lobbyMeta);

        // Lobby spawn for quick using
        lobbySpawn = Bukkit.getWorld("world").getSpawnLocation();

        // Scheduler var for quick using
        scheduler = Bukkit.getScheduler();

        // Scoreboard sidebar
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(ChatColor.GOLD + "POF", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Bossbar info
        bossBar = Bukkit.createBossBar("Waiting for players.", BarColor.RED, BarStyle.SEGMENTED_20);

        // Prepare potions and monster eggs
        
    }

    @Override
    public boolean hasSpace() {
        return currentState == STATE.WAITING && alive.size() < 8;
    }

    @Override
    public void start() {
        // Game loops
        new BukkitRunnable() {
            int bossBarDots = 0;

            @Override
            public void run() {
                if (alive.size() >= 2) {
                    if (countingDown == false) {
                        countingDown = true;
                        countdown = 60;
                        sendInfo("Game starting in 60 seconds");
                        bossBar.setTitle("Game starting in 60 seconds");
                    } else {
                        countdown--;
                        bossBar.setTitle("Game starting in " + Integer.toString(countdown) + " seconds");
                        bossBar.setProgress(countdown / 60.0);
                        if (countdown == 0) {
                            // Setup gameplay preparation
                            currentState = STATE.PLAYTIME;
                            bossBar.removeAll();

                            refreshScoreboard();

                            List<Location> tempSpawns = new ArrayList<>(spawns);
                            for (Player p : world.getPlayers()) {
                                // Setup player stuff
                                plugin.resetPlayer(p);

                                // Teleport player to their pillar
                                Location randomLoc = tempSpawns.get(random.nextInt(0, tempSpawns.size()));
                                p.setFallDistance(0f);
                                p.setVelocity(new Vector(0, 0, 0));
                                p.teleport(randomLoc);
                                p.setFallDistance(0f);
                                tempSpawns.remove(randomLoc);

                                // Set scoreboard
                                p.setScoreboard(scoreboard);
                            }

                            // Instructions
                            sendInfo("You get random items every 5 seconds.");

                            // Cage release countdown loop
                            new BukkitRunnable() {
                                int count = 6;

                                @Override
                                public void run() {
                                    count--;
                                    if (count == 3) sendInfo(ChatColor.RED + "Teaming is against the rules.");
                                    if (count > 0) sendInfo("Cages releasing in " + Integer.toString(count));
                                    else {
                                        this.cancel();
                                        for (Location loc : spawns) removeBox(loc);
                                        for (Player p : world.getPlayers()) p.setGameMode(GameMode.SURVIVAL);

                                        // Start random item generation loop
                                        generationLoop = scheduler.runTaskTimer(plugin, () -> {
                                            for (Player player : world.getPlayers()) {
                                                if (alive.contains(player)) { 
                                                    boolean single = random.nextBoolean();
                                                    List<ItemStack> itemList = single ? singleLoot : multiLoot;
                                                    List<ItemStack> item = new ArrayList<>();
                                                    item.add(itemList.get(random.nextInt(0, itemList.size())).clone());
                                                    if (!single) {
                                                        ItemStack newItem = item.get(0);
                                                        newItem.setAmount(List.of(4, 8, 8, 16).get(random.nextInt(0, 4)));
                                                        item.set(0, newItem);
                                                        if (random.nextInt(0, 3) == 0) item.add(multiLoot.get(random.nextInt(0, multiLoot.size())));
                                                    }
                                                    for (ItemStack itemI : item) player.getInventory().addItem(itemI);
                                                }
                                            }
                                        }, 0l, 100l);

                                        // Add slime in the center
                                        scheduler.runTaskLater(plugin, () -> {
                                            for (Player p : world.getPlayers()) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("The center is slime!"));
                                            for (int x = -1; x < 2; x++) {
                                                for (int z = -1; z < 2; z++) world.getBlockAt(x, 66, z).setType(Material.SLIME_BLOCK);
                                            }
                                        }, 200l);
                                    }
                                }
                            }.runTaskTimer(plugin, 0l, 20l);
                            this.cancel();
                        } else if (countdown == 1) broadcastActionBar("Game starting in 1 second");
                        else if (countdown % 10 == 0 || countdown < 10) broadcastActionBar("Game starting in " + Integer.toString(countdown) + " seconds");
                    }
                } else {
                    if (countingDown == true) {
                        sendInfo("Not enough players, game countdown reset.");
                        countingDown = false;
                        bossBar.setTitle("Waiting for players.");
                        bossBar.setProgress(1.0);
                        bossBarDots = 1;
                    } else {
                        bossBarDots++;
                        if (bossBarDots == 3) bossBarDots = 0;
                        bossBar.setTitle("Waiting for players." + ".".repeat(bossBarDots));
                    }
                }
            }
        }.runTaskTimer(plugin, 0l, 20l);
    }

    private void removeBox(Location spawn) {
        int blockX = spawn.getBlockX(), blockY = spawn.getBlockY(), blockZ = spawn.getBlockZ();
        for (int x = blockX - 1; x <= blockX + 1; x++) {
            for (int y = blockY; y <= blockY + 2; y++) {
                for (int z = blockZ - 1; z <= blockZ + 1; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }

    private void refreshScoreboard() {
        objective.getScore(ChatColor.GREEN + "Alive").setScore(alive.size());
        objective.getScore(ChatColor.RED + "Dead").setScore(dead.size());
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (currentState == STATE.PLAYTIME) {
            // Update playercount
            Player player = event.getEntity();
            alive.remove(player);
            dead.add(player);
            refreshScoreboard();

            // Death message
            event.setDeathMessage(null);
            Player killer = player.getKiller();
            sendInfo(ChatColor.DARK_RED + "Player " + ChatColor.RED + player.getName() + ChatColor.DARK_RED + (killer != null ? (" died to " + ChatColor.GREEN + killer.getName()) : " died."));

            // Respawn
            scheduler.runTaskLater(plugin, () -> {
                player.spigot().respawn();
                player.setGameMode(GameMode.SPECTATOR);
                PlayerInventory inv = player.getInventory();
                inv.setHeldItemSlot(4);
                inv.clear();
                // TODO: These are problematic because they require custom inventory handling (Next steps would be to generalize this)
                // inv.setItem(2, newGame);
                // inv.setItem(6, lobby);
            }, 1l);

            // Check if theres a winner
            checkEndGame();
        } else {
            // It has to STATE.WAITING here (You can't get out during STATE.COUNTDOWN)
            scheduler.runTaskLater(plugin, () -> event.getEntity().spigot().respawn(), 1l);
        }
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(spawnLoc);
    }

    @Override
    public void attemptLeave(Player player) {
        if (currentState == STATE.WAITING) {
            // Update playercount
            alive.remove(player);
            refreshScoreboard();

            // Send alert & reset boss bar for player
            sendDM(player, prefix + ChatColor.YELLOW + "Leaving the Pillars of Fortune game...");
            bossBar.removePlayer(player);
            sendInfo(ChatColor.YELLOW + "Player " + player.getName() + "has left.");
        }
    }

    @Override
    public String getMap() {
        List<String> maps = gameConfig.getStringList("maps");
        if (random == null) random = new Random();
        return maps.get(random.nextInt(0, maps.size()));
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (currentState == STATE.WAITING) {
            // Send alert
            Player player = event.getPlayer();
            sendInfo(ChatColor.YELLOW + "Player " + player.getName() + " has joined. " + ChatColor.AQUA + "(" + ChatColor.GOLD + Integer.toString(alive.size()) + "/8" + ChatColor.AQUA + ")");

            // Decrease countdown for less waiting
            if (countingDown == true && countdown > 20) {
                countdown -= 10;
                if (countdown % 10 == 0) broadcastActionBar("Game starting in " + Integer.toString(countdown) + " seconds");
            }

            // Change playercount
            alive.add(player);

            // Update views
            refreshScoreboard();
            bossBar.addPlayer(player);

            // Reset player
            plugin.resetPlayer(player);
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Update playercount
        Player player = event.getPlayer();
        if (currentState == STATE.WAITING) alive.remove(player);
        else if (currentState == STATE.PLAYTIME) {
            if (player.getGameMode() == GameMode.SURVIVAL) alive.remove(player);
            else dead.remove(player);
        }
        refreshScoreboard();
        
        // Reset player
        plugin.resetPlayer2Lobby(player);
        bossBar.removePlayer(player);

        // Send alert
        sendInfo(ChatColor.YELLOW + "Player " + player.getName() + " has left.");

        // End game if needed
        if (currentState == STATE.PLAYTIME) checkEndGame();
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (currentState != STATE.PLAYTIME) event.setCancelled(true);
    }

    private void checkEndGame() {
        if (alive.size() == 1) endGame(alive.iterator().next());
    }

    private void endGame(Player player) {
        broadcastExcluding(player, prefix + ChatColor.GREEN + player.getName() + " has won Pillars of Fortune!", prefix + ChatColor.GREEN + "You have won Pillars of Fortune!");
        currentState = STATE.SCORES;
        generationLoop.cancel();
        new BukkitRunnable() {
            private int count;

            @Override 
            public void run() {
                count++;

                if (count == 11) {
                    this.cancel();
                    for (Player p : world.getPlayers()) plugin.resetPlayer2Lobby(p);
                    resetWorld();
                } else broadcastActionBar("Teleporting you to the lobby in " + Integer.toString(11 - count) + "...");
            }
        }.runTaskTimer(plugin, 0l, 20l);
    }

    private void sendInfo(String msg) {
        broadcast(prefix + ChatColor.WHITE + msg);
    }

    private void sendDM(Player player, String msg) {
        player.spigot().sendMessage(TextComponent.fromLegacyText(msg));
    }

    private void broadcastExcluding(Player player, String broadcast, String excluded) {
        // Exclude player from playerlist
        List<Player> playerlist = world.getPlayers();
        playerlist.remove(player);

        // Send broadcast
        BaseComponent[] msg = TextComponent.fromLegacyText(broadcast);
        for (Player p : playerlist) p.spigot().sendMessage(msg);

        // Send direct message to player
        player.spigot().sendMessage(TextComponent.fromLegacyText(excluded));
    }

    private void broadcast(String msg) {
        BaseComponent[] bc = TextComponent.fromLegacyText(msg);
        for (Player player : world.getPlayers()) player.spigot().sendMessage(bc);
    }

    private void broadcastActionBar(String msg) {
        BaseComponent[] bc = TextComponent.fromLegacyText(msg);
        for (Player player : world.getPlayers()) player.spigot().sendMessage(ChatMessageType.ACTION_BAR, bc);
    }

    @Override 
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (currentState != STATE.PLAYTIME && event.getEntity() instanceof Player && event.getDamager() instanceof Player) event.setCancelled(true);
    }

    @Override
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        broadcast(ChatColor.GRAY + "<" + event.getPlayer().getName() + ">: " + event.getMessage());
    }
}