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
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.chat.TextComponent;

public class Minigames extends JavaPlugin {
    public final Map<String, Function<GameArgs, Game>> gameTypes = new HashMap<>();
    public final Map<String, Map<Integer, Game>> games = new HashMap<>();
    public final Map<String, Inventory> selectionMenus = new HashMap<>();
    private final List<String> worldDelete = new ArrayList<>();
    public ItemStack quickSelectionMenu, return2MainLobby, exit;
    private GameListener listener;
    private Logger log;
    public File worldContainer;

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

    public void setLobbyInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setItem(0, quickSelectionMenu);
        inv.setItem(8, return2MainLobby);
    }

    @Override
    public void onEnable() {
        log = getLogger();
        log.log(Level.INFO, "Starting...");

        // Setup config
        saveDefaultConfig();

        // Setup listeners
        Commands commands = new Commands(this);
        getCommand("join").setExecutor(commands);
        listener = new GameListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // Setup games
        gameTypes.put("Bridge", BridgeGame::new);
        gameTypes.put("PillarsOfFortune", PillarsOfFortune::new);
        for (String key : gameTypes.keySet()) games.put(key, new HashMap<>());

        // Setup game selection inventories
        Inventory bridgeGUI = Bukkit.createInventory(null, 27, "Bridge");
        exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "Close Menu");
        exitMeta.setLore(List.of(ChatColor.RESET + "This closes the game selection interface."));
        exit.setItemMeta(exitMeta);
        bridgeGUI.setItem(0, exit);
        for (int i = 0; i < 4; i++) {
            ItemStack gameType = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            ItemMeta gameTypeMeta = gameType.getItemMeta();
            String playersInType = Integer.toString(i + 1);
            gameTypeMeta.setDisplayName(ChatColor.RESET + "Play " + playersInType + "v" + playersInType);
            gameTypeMeta.setLore(List.of(ChatColor.RESET + "Open Lobbies: 0/0"));
            gameType.setItemMeta(gameTypeMeta);
            bridgeGUI.setItem(10 + (2 * i), gameType);
        }
        selectionMenus.put("Bridge", bridgeGUI);
        Inventory POFGUI = Bukkit.createInventory(null, 27, "Pillars of Fortune");
        POFGUI.setItem(0, exit);
        ItemStack icon = new ItemStack(Material.IRON_FENCE);
        ItemMeta iconMeta = icon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.RESET + "Play");
        iconMeta.setLore(List.of(ChatColor.RESET + "Open Lobbies: 0/0"));
        icon.setItemMeta(iconMeta);
        POFGUI.setItem(13, icon);
        selectionMenus.put("PillarsOfFortune", POFGUI);

        // World container
        worldContainer = Bukkit.getWorldContainer();

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
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (int i = 0; i < 4; i++) {
                ItemStack item = bridgeGUI.getItem(10 + (2 * i));
                ItemMeta meta = item.getItemMeta();
                ByteArrayDataOutput metadata = ByteStreams.newDataOutput();
                metadata.writeInt(i + 1);
                byte[] bytes = metadata.toByteArray();
                int open = 0, total = 0;
                for (Game game : games.get("Bridge").values()) {
                    if (Arrays.equals(bytes, game.getMetadata())) {
                        if (game.hasSpace()) open++;
                        total++;
                    }
                }
                meta.setLore(List.of(ChatColor.RESET + "Open Lobbies: " + Integer.toString(open) + "/" + Integer.toString(total)));
                item.setItemMeta(meta);
                item.setAmount(total);
                bridgeGUI.setItem(10 + (2 * i), item);
            }
            ItemStack pof = POFGUI.getItem(13);
            ItemMeta pofMeta = pof.getItemMeta();
            int open = 0, total = 0;
            for (Game game : games.get("PillarsOfFortune").values()) {
                if (game.hasSpace()) open++;
                total++; 
            }
            pofMeta.setLore(List.of(ChatColor.RESET + "Open Lobbies: " + Integer.toString(open) + "/" + Integer.toString(total)));
            pof.setItemMeta(pofMeta);
            pof.setAmount(total);
            POFGUI.setItem(13, pof);
        }, 0l, 20l);
    }
    
    @Override
    public void onDisable() {
        log.log(Level.INFO, "Shutting down...");
    }
}
