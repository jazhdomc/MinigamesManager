package mc.jazhdo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class GameListener implements Listener {
    private final Minigames plugin;
    private final ItemStack leave, back2QuickSelect;
    private final Inventory quickSelectInv;

    public GameListener(Minigames plugin) {
        this.plugin = plugin;

        // Leave item
        leave = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leave.getItemMeta();
        leaveMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "Leave");
        leaveMeta.setLore(List.of(ChatColor.RESET + "Leaves the current Pillars of Fortune game."));
        leave.setItemMeta(leaveMeta);

        // Back button
        back2QuickSelect = new ItemStack(Material.BARRIER);
        ItemMeta back2QuickSelectMeta = back2QuickSelect.getItemMeta();
        back2QuickSelectMeta.setDisplayName(ChatColor.RESET + "Back");
        back2QuickSelectMeta.setLore(List.of(ChatColor.RESET + "Navigates to the previous page."));
        back2QuickSelect.setItemMeta(back2QuickSelectMeta);

        // Quick select
        quickSelectInv = Bukkit.createInventory(null, 27, "Game Quick Select");
        ItemStack bridge = new ItemStack(Material.STAINED_CLAY);
        ItemMeta bridgeMeta = bridge.getItemMeta();
        bridgeMeta.setDisplayName(ChatColor.RESET + "Bridge");
        bridgeMeta.setLore(List.of(ChatColor.RESET + "The Bridge game from Hypixel, remade."));
        bridge.setItemMeta(bridgeMeta);
        ItemStack pillarsOfFortune = new ItemStack(Material.STICK);
        ItemMeta pillarsOfFortuneMeta = pillarsOfFortune.getItemMeta();
        pillarsOfFortuneMeta.setDisplayName("Pillars of Fortune");
        pillarsOfFortuneMeta.setLore(List.of(ChatColor.RESET + "The popular POF game."));
        pillarsOfFortune.setItemMeta(pillarsOfFortuneMeta);
        quickSelectInv.setItem(11, bridge);
        quickSelectInv.setItem(15, pillarsOfFortune);
    }
    
    /**
     * Gets the game that is playing in that world or null for no game
     * 
     * @param world The world to check for
     * @return The game that is playing in that world, or null if there is none
     */
    public Game getGame(World world) {
        if (world.hasMetadata("game")) return plugin.getGame(world.getMetadata("game").get(0).asString(), world.getMetadata("id").get(0).asInt());
        else return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Game game = getGame(event.getBlock().getWorld());
        if (game != null) game.onBlockBreak(event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Game game = getGame(event.getBlock().getWorld());
        if (game != null) game.onBlockPlace(event);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Game game = getGame(event.getEntity().getWorld());
        if (game != null) game.onEntityDamageByEntity(event);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Game game = getGame(event.getEntity().getWorld());
        if (game != null) game.onPlayerDeath(event);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getName().equals("world")) player.teleport(world.getSpawnLocation());
        else {
            Game game = getGame(world);
            if (game != null) game.onPlayerJoin(event);
            else player.teleport(Bukkit.getWorld("world").getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Game game = getGame(event.getFrom().getWorld());
        if (game != null) game.onPlayerMove(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Game game = getGame(event.getPlayer().getWorld());
        if (game != null) game.onPlayerQuit(event);
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Game game = getGame(event.getPlayer().getWorld());
        if (game != null) game.onAsyncPlayerChat(event);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Game game = getGame(event.getPlayer().getWorld());
        if (game != null) game.onPlayerRespawn(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (plugin.selectionMenus.containsValue(inv)) {
            event.setCancelled(true);
            // Handle exit slot
            int slot = event.getSlot();
            if (slot == 0) {
                if (currentItem == plugin.exit) player.closeInventory();
                else if (currentItem == back2QuickSelect) {

                }
                return;
            }

            // Get game details
            String gameName;
            byte[] metadata;
            switch (inv.getName()) {
                case "Bridge" -> {
                    gameName = "Bridge";

                    // Calculate team size and use it for metadata
                    ByteArrayDataOutput output = ByteStreams.newDataOutput();
                    int teamSize = switch (slot) {
                        case 10 -> 1;
                        case 12 -> 2;
                        case 14 -> 3;
                        case 16 -> 4;
                        default -> 0;
                    };
                    if (teamSize == 0) return;
                    output.writeInt(teamSize);
                    metadata = output.toByteArray();
                }
                case "Pillars of Fortune" -> {
                    gameName = "PillarsOfFortune";
                    metadata = new byte[0];
                }
                default -> {
                    return;
                }
            }
            
            // Get a game through one of three methods
            getAGame: {
                // Get into an existing game if there is space in one
                Map<Integer, Game> gameList = plugin.games.get(gameName);
                for (Game game : gameList.values()) {
                    if (game.hasSpace() && Arrays.equals(game.getMetadata(), metadata)) {
                        player.teleport(game.getSpawnLocation());
                        break getAGame;
                    }
                }

                // Make a new game in a existing spot if possible
                Function<GameArgs, Game> gameClass = plugin.gameTypes.get(gameName);
                for (int index : gameList.keySet()) {
                    if (gameList.get(index) == null) {
                        Game newGame = gameClass.apply(new GameArgs(plugin, index, gameName, metadata));
                        gameList.put(index, newGame);
                        player.teleport(newGame.getSpawnLocation());
                        break getAGame;
                    }
                }

                // If there are no empty spots, create a new one
                int index = gameList.keySet().size();
                Game newGame = gameClass.apply(new GameArgs(plugin, index, gameName, metadata));
                gameList.put(index, newGame);
                player.teleport(newGame.getSpawnLocation());
            }

            // Add exit button
            player.getInventory().setItem(8, leave);
        } else if (currentItem == plugin.quickSelectionMenu) {
            event.setCancelled(true);
            player.openInventory(quickSelectInv);
        } else if (currentItem == plugin.return2MainLobby) {
            event.setCancelled(true);
            
        } else {
            Game game = getGame(player.getWorld());
            if (game != null) {
                event.setCancelled(true);
                if (currentItem == leave) game.attemptLeave(player);
                else game.onInventoryClick(event);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Game game = getGame(event.getTo().getWorld());
        if (game != null) game.onPlayerTeleport(event);
    }

    private void deleteFolder(File folder) {
        File[] fileList = folder.listFiles();
        if (fileList != null) 
            for (File file : fileList) 
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
        folder.delete();
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        String worldName = event.getWorld().getName();
        if (plugin.isWorld2Delete(worldName)) Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteFolder(new File(plugin.worldContainer, worldName)));
    }
}
