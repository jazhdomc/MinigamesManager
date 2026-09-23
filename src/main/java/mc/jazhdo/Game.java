package mc.jazhdo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;

public abstract class Game {
    protected final Minigames plugin;
    protected final FileConfiguration config;
    protected final ConfigurationSection gameConfig;
    protected final Logger log;
    protected final int worldId;
    protected final String gameName;
    protected final byte[] metadata;
    protected Map<String, Integer> scores = new HashMap<>();
    protected Map<String, List<String>> teams = new HashMap<>();
    protected World world;
    protected Location spawnLoc;
    protected String worldName;

    // Provided functions
    public Game(GameArgs args) {
        plugin = args.plugin();
        config = plugin.getConfig();
        log = plugin.getLogger();
        worldId = args.worldId();
        gameName = args.gameName();
        metadata = args.metadata();
        gameConfig = config.getConfigurationSection(gameName.toLowerCase());

        // Setup & start
        setup();
    }
    private void copyFolder(File src, File dest) {
        dest.mkdirs();
        for (File f : src.listFiles()) {
            String fName = f.getName();
            if (f.isDirectory()) copyFolder(f, new File(dest, fName));
            else {
                try {
                    if (fName.equals("uid.dat") || fName.equals("session.lock")) continue;
                    Files.copy(f.toPath(), new File(dest, fName).toPath());
                } catch (IOException e) {
                    log.warning("Error occurred while copying files: ".concat(e.getMessage()));
                }
            }
        }
    }
    protected void deleteFolder(File folder) {
        File[] fileList = folder.listFiles();
        if (fileList != null) 
            for (File file : fileList) 
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
        folder.delete();
    }
    private void setup() {
        // Take care of the world and folder if it already exists
        worldName = gameName + "Game" + Integer.toString(worldId);
        File gameFolder = new File(plugin.worldContainer, worldName);
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) Bukkit.unloadWorld(existing, false);
        if (gameFolder.exists()) deleteFolder(gameFolder);

        // Create new world
        copyFolder(new File(plugin.worldContainer, getMap()), gameFolder);
        world = new WorldCreator(worldName).environment(World.Environment.NORMAL).generator("VoidGen").createWorld();
        world.setMetadata("game", new FixedMetadataValue(plugin, gameName));
        world.setMetadata("id", new FixedMetadataValue(plugin, worldId));

        // Get spawn location
        spawnLoc = new Location(world, gameConfig.getDouble("spawn.x"), gameConfig.getDouble("spawn.y"), gameConfig.getDouble("spawn.z"), (float) gameConfig.getDouble("spawn.yaw"), (float) gameConfig.getDouble("spawn.pitch"));

        // Move on to start
        start();
    }
    public Location getSpawnLocation() {
        return spawnLoc;
    }
    public byte[] getMetadata() {
        return metadata;
    }
    protected void resetWorld() {
        plugin.deleteOnUnload(worldName);
        Bukkit.unloadWorld(world, false);
    }
    public void leaveGame(Player player) {
        plugin.resetPlayer2Lobby(player);
        attemptLeave(player);
    }

    // Required functions
    protected abstract boolean hasSpace();
    protected abstract String getMap();
    protected abstract void start();
    protected abstract void attemptLeave(Player player);

    // Optional functions
    protected void onBlockBreak(BlockBreakEvent event) {}
    protected void onBlockPlace(BlockPlaceEvent event) {}
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {}
    protected void onPlayerDeath(PlayerDeathEvent event) {}
    protected void onPlayerMove(PlayerMoveEvent event) {}
    protected void onPlayerQuit(PlayerQuitEvent event) {}
    protected void onAsyncPlayerChat(AsyncPlayerChatEvent event) {}
    protected void onPlayerTeleport(PlayerTeleportEvent event) {}
    protected void onPlayerRespawn(PlayerRespawnEvent event) {}
}