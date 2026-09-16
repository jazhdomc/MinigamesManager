package mc.jazhdo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
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
        leaveMeta.setLore(List.of(ChatColor.RESET + "Leaves the current minigame."));
        leave.setItemMeta(leaveMeta);

        // Back button (unused)
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
        quickSelectInv.setItem(11, bridge);
        ItemStack pillarsOfFortune = new ItemStack(Material.STICK);
        ItemMeta pillarsOfFortuneMeta = pillarsOfFortune.getItemMeta();
        pillarsOfFortuneMeta.setDisplayName("Pillars of Fortune");
        pillarsOfFortuneMeta.setLore(List.of(ChatColor.RESET + "The popular POF game."));
        pillarsOfFortune.setItemMeta(pillarsOfFortuneMeta);
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

    /**
     * A newer version of getGame() that takes in the function to run if the world is a game world
     * 
     * @param world The world to check if it is a game world for
     * @param lambdaFunction The function to run if the world is a game world
     */
    public void runIfNonNullGetGame(World world, Consumer<Game> lambdaFunction) {
        Game game = getGame(world);
        if (game != null) lambdaFunction.accept(game);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        runIfNonNullGetGame(event.getBlock().getWorld(), g -> g.onBlockBreak(event));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        runIfNonNullGetGame(event.getBlock().getWorld(), g -> g.onBlockPlace(event));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getWorld() == Bukkit.getWorld("world")) event.setCancelled(true);
        runIfNonNullGetGame(event.getEntity().getWorld(), g -> g.onEntityDamageByEntity(event));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        runIfNonNullGetGame(event.getEntity().getWorld(), g -> g.onPlayerDeath(event));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Teleport player to the lobby no matter what world they join (game joining is done with teleportations)
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        plugin.resetPlayer2Lobby(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        runIfNonNullGetGame(event.getFrom().getWorld(), g -> g.onPlayerMove(event));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        runIfNonNullGetGame(event.getPlayer().getWorld(), g -> g.onPlayerQuit(event));
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        runIfNonNullGetGame(event.getPlayer().getWorld(), g -> g.onAsyncPlayerChat(event));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        runIfNonNullGetGame(event.getPlayer().getWorld(), g -> g.onPlayerRespawn(event));
    }

    // Inventory clicks for GUIs specifically
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (plugin.selectionMenus.containsValue(inv)) {
            // Handle exit slot
            int slot = event.getSlot();
            if (slot == 0) {
                if (currentItem.isSimilar(plugin.exit)) player.closeInventory();
                else if (currentItem.isSimilar(back2QuickSelect)) {
                    // Unused item
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
        } else if (currentItem.isSimilar(plugin.quickSelectionMenu)) {
            if (event.getClick() != ClickType.NUMBER_KEY) player.openInventory(quickSelectInv);
        } else if (inv.equals(quickSelectInv)) {
            if (event.getClick() != ClickType.NUMBER_KEY) {
                switch (event.getSlot()) {
                    case 11 -> player.openInventory(plugin.selectionMenus.get("Bridge"));
                    case 15 -> player.openInventory(plugin.selectionMenus.get("PillarsOfFortune"));
                }
            }
        } else if (currentItem.isSimilar(plugin.return2MainLobby)) {
            if (event.getClick() != ClickType.NUMBER_KEY) player.chat("/hub");
        } else if (currentItem.isSimilar(leave)) {
            if (event.getClick() != ClickType.NUMBER_KEY) runIfNonNullGetGame(player.getWorld(), g -> g.leaveGame(player));
        } else return;
        event.setCancelled(true);
    }

    // Hotbar menu button interaction
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        Player player = event.getPlayer();
        if (itemStack.isSimilar(plugin.quickSelectionMenu)) player.openInventory(quickSelectInv);
        else if (itemStack.isSimilar(plugin.return2MainLobby)) player.chat("/hub");
        else if (itemStack.isSimilar(leave)) runIfNonNullGetGame(player.getWorld(), g -> g.leaveGame(player));
        else return;
        event.setCancelled(true);
    }

    // Checks whether or not the containedItem item is found within the itemStackList list
    private boolean itemStackListContains(List<ItemStack> itemStackList, ItemStack containedItem) {
        for (ItemStack itemStack : itemStackList) if (itemStack.isSimilar(containedItem)) return true;
        return false;
    }

    // Item hand swap to catch item slot changes with menu buttons
    @EventHandler 
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        List<ItemStack> forbiddenSwitchList = List.of(plugin.quickSelectionMenu, plugin.return2MainLobby, leave);
        if (itemStackListContains(forbiddenSwitchList, event.getMainHandItem()) || itemStackListContains(forbiddenSwitchList, event.getOffHandItem())) event.setCancelled(true);
    }

    // Catch item drops if they are menu buttons
    @EventHandler 
    public void onPlayerItemDrop(PlayerDropItemEvent event) {
        List<ItemStack> forbiddenSwitchList = List.of(plugin.quickSelectionMenu, plugin.return2MainLobby, leave);
        if (itemStackListContains(forbiddenSwitchList, event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        runIfNonNullGetGame(event.getTo().getWorld(), g -> g.onPlayerTeleport(event));
    }

    private void deleteFolder(File folder) {
        File[] fileList = folder.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
            }
        }
        folder.delete();
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        String worldName = event.getWorld().getName();
        if (plugin.isWorld2Delete(worldName)) {
            plugin.scheduler.runTaskAsynchronously(plugin, () -> {
                deleteFolder(new File(plugin.worldContainer, worldName));
                String[] parts = worldName.split("Game");
                plugin.scheduler.runTask(plugin, () -> plugin.games.get(parts[0]).put(Integer.parseInt(parts[1]), null));
            });
        }
    }
}
