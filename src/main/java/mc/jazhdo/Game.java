package mc.jazhdo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class Game {
    protected final Minigames plugin;
    protected final FileConfiguration config;
    protected final ConfigurationSection gameConfig;
    protected final Logger log;
    protected final int teamSize, worldId;
    protected final String gameName;
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
        teamSize = args.teamSize();
        worldId = args.worldId();
        gameName = args.gameName();
        gameConfig = config.getConfigurationSection(gameName.toLowerCase());

        // Setup & start
        setup();
    }
    private void copyFolder(File src, File dest) {
        dest.mkdirs();
        for (File f : src.listFiles()) {
            if (f.isDirectory()) copyFolder(f, new File(dest, f.getName()));
            else {
                try {
                    Files.copy(f.toPath(), new File(dest, f.getName()).toPath());
                } catch (IOException e) {
                    log.warning("Error occurred while copying files: ".concat(e.getMessage()));
                }
            }
        }
        File uidFile = new File(dest, "uid.dat");
        if (uidFile.exists()) uidFile.delete();
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

        // Create new world with a random map
        List<String> maps = gameConfig.getStringList("maps");
        copyFolder(new File(plugin.worldContainer, maps.get(ThreadLocalRandom.current().nextInt(maps.size()))), gameFolder);
        world = new WorldCreator(worldName).environment(World.Environment.NORMAL).createWorld();

        // Get spawn location
        spawnLoc = new Location(world, gameConfig.getDouble("spawn.x"), gameConfig.getDouble("spawn.y"), gameConfig.getDouble("spawn.z"), (float) gameConfig.getDouble("spawn.yaw"), (float) gameConfig.getDouble("spawn.pitch"));

        // Move on to start
        start();
    }
    public Location getSpawnLocation() {
        return spawnLoc;
    }

    // Required functions
    protected abstract boolean hasSpace();
    protected abstract void start();

    // Optional functions
    protected void onBlockBreak(BlockBreakEvent event) {}
    protected void onBlockPlace(BlockPlaceEvent event) {}
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {}
    protected void onPlayerDeath(PlayerDeathEvent event) {}
    protected void onPlayerJoin(PlayerJoinEvent event) {}
    protected void onPlayerMove(PlayerMoveEvent event) {}
    protected void onPlayerQuit(PlayerQuitEvent event) {}
}

