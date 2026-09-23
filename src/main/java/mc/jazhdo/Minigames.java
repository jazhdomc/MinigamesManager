package mc.jazhdo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.chat.TextComponent;

public class Minigames extends JavaPlugin {
    public final Map<String, Function<GameArgs, Game>> gameTypes = new HashMap<>();
    public final Map<String, Map<Integer, Game>> games = new HashMap<>();
    public final Map<String, Inventory> selectionMenus = new HashMap<>();
    private final Map<String, ItemStack[]> gameIcons = new HashMap<>();
    private final List<String> worldDelete = new ArrayList<>();
    public ItemStack quickSelectionMenu, return2MainLobby, exit;
    public Location lobbySpawn;
    private FileConfiguration config;
    private GameListener listener;
    private Logger log;
    public File worldContainer;
    public BukkitScheduler scheduler;

    private class Commands implements CommandExecutor {
        private final Minigames plugin;

        public Commands(Minigames plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Make sure there is a sender
            if (sender == null) return false;

            // Make sure sender is a player
            if (sender instanceof Player player) {
                if (listener.getGame(player.getWorld()) != null) {
                    sendError(player, "You cannot use this command while in a game.");
                    return true;
                }

                // Validate arguments
                switch (args.length) {
                    case 1 -> {
                        // Get and validate game class
                        Inventory selectionMenu = selectionMenus.get(args[0]);
                        if (selectionMenu == null) {
                            sendError(player, "Game " + args[0] + " is not a type of game. Existing types: " + String.join(", ", gameTypes.keySet()));
                            return true;
                        }
                        
                        // Show selection inventory interface
                        player.openInventory(selectionMenu);
                    }
                    case 0 -> sendError(player, "<game> argument required. (/join <game>)");
                    default -> sendError(player, "Too many arguments. Required amount: 1 (/join <game>)");
                }
            } else sendError(sender, "This command can only be used as a player.");

            // Command send used
            return true;
        }
    }

    public void sendInfo(CommandSender player, String msg) {
        sendMessage(player, ChatColor.WHITE + msg);
    }

    public void sendError(CommandSender player, String msg) {
        sendMessage(player, ChatColor.RED + msg);
    }

    private void sendMessage(CommandSender player, String msg) {
        player.spigot().sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "[Minigames] " + msg));
    }

    public Game getGame(String gameName, int index) {
        return games.get(gameName).get(index);
    }

    public void setNull(String gameName, int index) {
        games.get(gameName).put(index, null);
    }

    public void deleteOnUnload(String worldName) {
        worldDelete.add(worldName);
    }

    public boolean isWorld2Delete(String worldName) {
        if (worldDelete.contains(worldName)) {
            worldDelete.remove(worldName);
            return true;
        }
        return false;
    }

    public void resetPlayer2Lobby(Player player) {
        player.teleport(lobbySpawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        resetPlayer(player);
        PlayerInventory inv = player.getInventory();
        inv.setHeldItemSlot(4);
        inv.setItem(0, quickSelectionMenu);
        inv.setItem(8, return2MainLobby);
    }

    public void resetPlayer(Player player) {
        player.getInventory().clear();
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0.0f);
        player.setHealth(20);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        player.setFireTicks(0);
        player.setExp(0f);
        player.setTotalExperience(0);
    }

    private ItemStack generateGameIcon(Material displayItem) {
        ItemStack icon = new ItemStack(displayItem);
        ItemMeta iconMeta = icon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Play");
        iconMeta.setLore(List.of(ChatColor.RESET + "Open Lobbies: 0/0"));
        icon.setItemMeta(iconMeta);
        return icon;
    }

    @Override
    public void onEnable() {
        log = getLogger();
        log.log(Level.INFO, "Starting...");

        // Setup config
        saveDefaultConfig();
        config = getConfig();

        // Setup listeners
        Commands commands = new Commands(this);
        getCommand("join").setExecutor(commands);
        listener = new GameListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // Setup games
        gameTypes.put("Bridge", BridgeGame::new);
        gameTypes.put("PillarsOfFortune", PillarsOfFortune::new);
        gameTypes.put("TntRun", TntRun::new);
        for (String key : gameTypes.keySet()) games.put(key, new HashMap<>());

        // Setup game selection inventories
        exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "Close Menu");
        exitMeta.setLore(List.of(ChatColor.RESET + "This closes the game selection interface."));
        exit.setItemMeta(exitMeta);
        Inventory bridgeGUI = Bukkit.createInventory(null, 27, "Bridge");
        {
            bridgeGUI.setItem(0, exit);
            ItemStack[] icons = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack icon = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                ItemMeta iconMeta = icon.getItemMeta();
                String playersInType = Integer.toString(i + 1);
                iconMeta.setDisplayName(ChatColor.RESET + "Play " + playersInType + "v" + playersInType);
                iconMeta.setLore(List.of(ChatColor.RESET + "Open Lobbies: 0/0"));
                icon.setItemMeta(iconMeta);
                bridgeGUI.setItem(10 + (2 * i), icon);
                icons[i] = icon;
            }
            selectionMenus.put("Bridge", bridgeGUI);
            gameIcons.put("Bridge", icons);
        }
        Inventory POFGUI = Bukkit.createInventory(null, 27, "Pillars of Fortune");
        {
            POFGUI.setItem(0, exit);
            ItemStack icon = generateGameIcon(Material.IRON_FENCE);
            POFGUI.setItem(13, icon);
            selectionMenus.put("PillarsOfFortune", POFGUI);
            gameIcons.put("PillarsOfFortune", new ItemStack[]{icon});
        }
        Inventory tntRunGUI = Bukkit.createInventory(null, 27, "Tnt Run");
        {
            tntRunGUI.setItem(0, exit);
            ItemStack icon = generateGameIcon(Material.TNT);
            tntRunGUI.setItem(13, icon);
            selectionMenus.put("TntRun", tntRunGUI);
            gameIcons.put("TntRun", new ItemStack[]{icon});
        }

        // World container
        worldContainer = Bukkit.getWorldContainer();

        // Scheduler
        scheduler = Bukkit.getScheduler();

        // Lobby spawn
        lobbySpawn = new Location(Bukkit.getWorld(config.getString("lobby.world")), config.getDouble("lobby.x"), config.getDouble("lobby.y"), config.getDouble("lobby.z"), (float) config.getDouble("lobby.yaw"), (float) config.getDouble("lobby.pitch"));

        // Quick select & main lobby hotbar interface items
        quickSelectionMenu = new ItemStack(Material.EMPTY_MAP);
        ItemMeta quickSelectionMenuMeta = quickSelectionMenu.getItemMeta();
        quickSelectionMenuMeta.setDisplayName(ChatColor.RESET + "Quick Select");
        quickSelectionMenuMeta.setLore(List.of(ChatColor.RESET + "Opens up the quick select game menu."));
        quickSelectionMenu.setItemMeta(quickSelectionMenuMeta);
        return2MainLobby = new ItemStack(Material.BARRIER);
        ItemMeta return2MainLobbyMeta = return2MainLobby.getItemMeta();
        return2MainLobbyMeta.setDisplayName(ChatColor.RESET + "Return to Main Lobby");
        return2MainLobbyMeta.setLore(List.of(ChatColor.RESET + "Teleports you to the main lobby."));
        return2MainLobby.setItemMeta(return2MainLobbyMeta);

        // Open lobby count update loop
        scheduler.runTaskTimer(this, () -> {
            for (int i = 0; i < 4; i++) {
                ByteArrayDataOutput metadata = ByteStreams.newDataOutput();
                metadata.writeInt(i + 1);
                updateLobbyCount("Bridge", getAndCheck(bridgeGUI, 10 + (2 * i), gameIcons.get("Bridge")[i]), true, metadata.toByteArray());
            }
            updateLobbyCount("PillarsOfFortune", getAndCheck(POFGUI, 13, gameIcons.get("PillarsOfFortune")[0]), false);
            updateLobbyCount("TntRun", getAndCheck(tntRunGUI, 13, gameIcons.get("TntRun")[0]), false);
        }, 20l, 20l);
    }
    private void updateLobbyCount(String internalGameName, ItemStack gameIcon, boolean usesMetadata) {
        updateLobbyCount(internalGameName, gameIcon, usesMetadata, new byte[0]);
    }
    private void updateLobbyCount(String internalGameName, ItemStack gameIcon, boolean usesMetadata, byte[] metadataBytes) {
        ItemMeta gameIconMeta = gameIcon.getItemMeta();
        int open = 0, total = 0;
        for (Game game : games.get(internalGameName).values()) {
            if (game == null) continue;
            if (!usesMetadata || Arrays.equals(metadataBytes, game.getMetadata())) {
                if (game.hasSpace()) open++;
                total++;
            }
        }
        gameIconMeta.setLore(List.of(ChatColor.RESET + "Open Lobbies: " + Integer.toString(open) + "/" + Integer.toString(total)));
        gameIcon.setItemMeta(gameIconMeta);
        gameIcon.setAmount(Math.clamp(total, 1, 64));
    }
    private ItemStack getAndCheck(Inventory GUI, int position, ItemStack backup) {
        ItemStack icon = GUI.getItem(position);
        if (icon == null || icon.getType() == Material.AIR) {
            icon = backup;
            GUI.setItem(position, icon);
        }
        return icon;
    }
    
    @Override
    public void onDisable() {
        log.log(Level.INFO, "Shutting down...");
    }
}
