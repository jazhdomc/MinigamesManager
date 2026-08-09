package mc.jazhdo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class BridgeGame extends Game {
    private enum State { WAITING, PLAYTIME, SCORES }
    private State currentState = State.WAITING;
    private Scoreboard scoreboard;
    private Objective objective;
    private BukkitTask timer;
    private final BossBar bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Time Left: 00:00", BarColor.RED, BarStyle.SOLID);

    public BridgeGame(GameArgs args) {
        super(args);
    }
    
    @Override
    public void attemptLeave(Player player) {
        player.teleport(Bukkit.getWorld("world").getSpawnLocation());
        handlePlayerLeave(player);
    }

    @Override
    public boolean hasSpace() {
        return currentState == State.WAITING && world.getPlayers().size() < (teamSize * 2);
    }

    @Override
    public void start() {
        new BukkitRunnable() {
            Integer countdownTimer = null, waitingTick = 0;

            @Override
            public void run() {
                // Whether to countdown or to wait
                if (countdownTimer == null) {
                    // Check if players are enough (>= 2)
                    if (world.getPlayers().size() >= teamSize * 2) {
                        countdownTimer = 3;
                    } else {
                        String dots[] = {".", "..", "..."};
                        for (Player p : world.getPlayers()) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.YELLOW + "Waiting for players" + dots[waitingTick] + " (" + world.getPlayers().size() + "/" + Integer.toString(teamSize * 2) + ")"));
                        if (waitingTick == 2) waitingTick = 0;
                        else waitingTick++;
                    }
                } else {
                    // Check if players choose to leave during the countdown
                    if (world.getPlayers().size() < teamSize * 2) {
                        waitingTick = 0;
                        currentState = State.WAITING;
                        broadcast(ChatColor.RED + "Not enough players.");
                        return;
                    }

                    // Set to playing or just update countdown
                    if (countdownTimer == 0) {
                        currentState = State.PLAYTIME;
                        setupGame();
                        timer = new BukkitRunnable() {
                            int gameLength = gameConfig.getInt("game-seconds-length"), gameTimer = gameLength, winningScore = gameConfig.getInt("winning-score");

                            {
                                // Setup
                                bossBar.setTitle(ChatColor.GOLD + "Time Left: " + Integer.toString(gameTimer/60) + ":" + Integer.toString(gameTimer%60));
                                bossBar.setProgress((double) gameTimer / (double) gameLength);
                                for (Player p : world.getPlayers()) bossBar.addPlayer(p);
                            }

                            @Override
                            public void run() {
                                // Check for wins
                                int redScore = scores.get("Red"), blueScore = scores.get("Blue");
                                if (redScore != blueScore) {
                                    if (redScore >= winningScore) endGame("Red");
                                    else if (blueScore >= winningScore) endGame("Blue");
                                }

                                // Handle game timer
                                if (gameTimer > 0) {
                                    gameTimer--;
                                    String seconds = Integer.toString(gameTimer%60), minutes = Integer.toString(gameTimer/60);
                                    if (seconds.length() < 2) seconds = "0" + seconds;
                                    if (minutes.length() < 2) minutes = "0" + minutes;
                                    bossBar.setTitle(ChatColor.GOLD + "Time Left: " + minutes + ":" + seconds);
                                    bossBar.setProgress((double) gameTimer / (double) gameLength);
                                } else endGame("Stalling");
                            }
                        }.runTaskTimer(plugin, 0l, 20l);
                        this.cancel();
                    } else {
                        for (Player p : world.getPlayers()) {
                            p.sendTitle(ChatColor.GREEN + Integer.toString(countdownTimer), ChatColor.GRAY + "Last chance to leave..", 5, 18, 7);
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        }
                        countdownTimer--;
                    }
                }
            }
        }.runTaskTimer(plugin, 0l, 20l);
    }

    private void broadcast(String msg) {
        BaseComponent[] component = TextComponent.fromLegacyText(msg);
        for (Player p : world.getPlayers()) p.spigot().sendMessage(component);
    }

    private void setupGame() {
        // Assign Teams
        List<Player> players = new ArrayList<>(world.getPlayers());
        Collections.shuffle(players);
        List<String> red = new ArrayList<>(), blue = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            boolean redTeam = (i < players.size() / 2);
            String team = redTeam ? "Red" : "Blue";
            Player player = players.get(i);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.valueOf(team.toUpperCase()) + "You are on Team " + team + "!"));
            (redTeam ? red : blue).add(player.getName());
            respawnPlayer(player, redTeam);
        }
        teams.put("Red", red);
        teams.put("Blue", blue);

        // Initialize scores
        scores.put("Red", 0);
        scores.put("Blue", 0);

        // Reset and setup scoreboard
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(worldName, "dummy");
        objective.setDisplayName(ChatColor.GOLD + "The Bridge");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        new BukkitRunnable() {
            int count = 2;
            String[] messages = {"Ready!", "Set!"};

            @Override
            public void run() {
                if (count > 0) {
                    for (Player p : world.getPlayers()) {
                        p.sendTitle(ChatColor.YELLOW + messages[2 - count], "", 5, 18, 7);
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                } else {
                    for (Player p : world.getPlayers()) {
                        p.sendTitle(ChatColor.GREEN + "Go!", ChatColor.WHITE + "Bridge to the other side!", 5, 30, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_LAUNCH, 1f, 1f);
                    }

                    // Remove Red and Blue cages
                    removeCages(new Location(world, gameConfig.getInt("red-cage-location.x"), gameConfig.getInt("red-cage-location.y"), gameConfig.getInt("red-cage-location.z")));
                    removeCages(new Location(world, gameConfig.getInt("blue-cage-location.x"), gameConfig.getInt("blue-cage-location.y"), gameConfig.getInt("blue-cage-location.z")));

                    // Show starting score
                    updateScoreboard();
                    this.cancel();
                }
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Set gamemode
        for (Player p : world.getPlayers()) p.setGameMode(GameMode.SURVIVAL);
    }

    private void removeCages(Location center) {
        int xHalfLength = gameConfig.getInt("cage-size.x-half-length"), height = gameConfig.getInt("cage-size.height"), zHalfLength = gameConfig.getInt("cage-size.z-half-length");
        // For each block in the cube
        for (int x = -xHalfLength; x <= xHalfLength; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = -zHalfLength; z <= zHalfLength; z++) {
                    Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (Math.abs(block.getTypeId() - 127) == 32) block.setType(Material.AIR);
                }
            }
        }
    }

    private void updateScoreboard() {
        objective.getScore(ChatColor.RED + "Red").setScore(scores.get("Red"));
        objective.getScore(ChatColor.BLUE + "Blue").setScore(scores.get("Blue"));
        for (Player p : world.getPlayers()) p.setScoreboard(scoreboard);
    }

    public String getTeam(String playerName) {
        for (String player : teams.get("Red")) if (player.equals(playerName)) return "Red";
        for (String player : teams.get("Blue")) if (player.equals(playerName)) return "Blue";
        return null;
    }

    private void endGame(String winner) {
        currentState = State.SCORES;
        timer.cancel();
        for (Player p : world.getPlayers()) {
            p.sendTitle((winner.equals("Red") ? ChatColor.RED : (winner.equals("Blue") ? ChatColor.BLUE : ChatColor.GRAY)) + winner + " Wins!", ChatColor.GOLD + "Good Game!", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Reset game for all players
            bossBar.removeAll();
            for (Player p : world.getPlayers()) {
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().clear();
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                p.teleport(Bukkit.getWorld("world").getSpawnLocation());
            }
        }, 100L);
        resetWorld();
    }

    private void resetWorld() {
        Bukkit.unloadWorld(world, false);
        plugin.deleteOnUnload(worldName);
    }

    public void respawnPlayer(Player player) {
        String team = getTeam(player.getName());
        if (team == null) return;
        respawnPlayer(player, team.equals("Red"));
    }

    public void respawnPlayer(Player player, boolean redTeam) {
        // Reset health and food and saturation
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);

        // Reset player location
        Location spawn = redTeam ?
        new Location(world, gameConfig.getDouble("red.x"), gameConfig.getDouble("red.y"), gameConfig.getDouble("red.z"), (float) gameConfig.getDouble("red.yaw"), (float) gameConfig.getDouble("red.pitch")) :
        new Location(world, gameConfig.getDouble("blue.x"), gameConfig.getDouble("blue.y"), gameConfig.getDouble("blue.z"), (float) gameConfig.getDouble("blue.yaw"), (float) gameConfig.getDouble("blue.pitch"));
        player.teleport(spawn);

        // Clear Inventory to prevent stacking items in inventory's back slots
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();

        // Clear effets like gapple
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        player.removePotionEffect(PotionEffectType.REGENERATION);

        // Give Starting Inventory
        playerInventory.setItem(0, new ItemStack(Material.IRON_SWORD));
        ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.addEnchant(Enchantment.DIG_SPEED, 5, true);
        pickaxe.setItemMeta(pickaxeMeta);
        playerInventory.setItem(1, pickaxe);
        playerInventory.setItem(2, new ItemStack(Material.IRON_AXE));
        playerInventory.setItem(3, new ItemStack(Material.BOW));
        playerInventory.setItem(4, new ItemStack(Material.COOKED_BEEF, 8));
        playerInventory.setItem(5, new ItemStack(Material.GOLDEN_APPLE, 8));
        short clayColor = redTeam ? (short) 14 : (short) 11;
        for (int i = 6; i < 9; i++) playerInventory.setItem(i, new ItemStack(Material.STAINED_CLAY, 64, clayColor));
        playerInventory.setItem(9, new ItemStack(Material.ARROW, 64));
        playerInventory.setItem(10, new ItemStack(Material.ARROW, 64));
    }

    // ----- Event Listeners -----

    private void handlePlayerLeave(Player player) {
        // Only handle player quits if its playtime when quits matter
        if (currentState != State.WAITING) player.getInventory().clear();
        if (currentState != State.PLAYTIME || getTeam(player.getName()) == null) return;

        // Remove from teams list to check teams size
        String playerName = player.getName(), team = getTeam(playerName);
        List<String> playerlist = teams.get(team);
        playerlist.remove(playerName);
        teams.put(team, playerlist);

        // Check if there are still enough players to play
        List<Player> remaining = world.getPlayers();
        remaining.remove(player);
        if (currentState == State.PLAYTIME && (teams.get("Red").isEmpty() || teams.get("Blue").isEmpty())) {
            broadcast(ChatColor.RED + "Not enough players on a team, ending game.");
            if (!remaining.isEmpty()) endGame((getTeam(remaining.get(0).getName())));
            else endGame("Nobody");
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if you actually move
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        // Simulate void
        Player player = event.getPlayer();
        if (event.getTo().getY() < gameConfig.getInt("void-y")) {
            player.setHealth(0);
            return;
        }

        // Check portal touches and only if its playtime
        if (currentState == State.PLAYTIME) {
            Location loc = event.getTo();
            if (loc.getBlockY() <= gameConfig.getInt("portal-y")) {
                // Check touching portal
                Block feet = loc.getBlock();
                Block below = loc.clone().subtract(0, 1, 0).getBlock();
                if (feet.getType() == Material.ENDER_PORTAL || below.getType() == Material.ENDER_PORTAL) {
                    // Get and verify team
                    String playerName = player.getName(), team = getTeam(playerName);
                    if (team == null) {
                        plugin.sendError(player, "Jumping into the portal with no team gives nothing.");
                        return;
                    }

                    // Increment scores, send broadcasts and respawn the players
                    if (!team.equals((gameConfig.getString("center.axis").equals("Z") ? loc.getBlockZ() : loc.getBlockX()) < gameConfig.getInt("center.value") ? "Red" : "Blue")) {
                        if (team.equals("Red")) {
                            scores.compute("Red", (key, val) -> val + 1);
                            broadcast(ChatColor.RED + playerName + " scored for Team Red! " + ChatColor.RED + scores.get("Red") + ChatColor.WHITE + " - " + ChatColor.BLUE + scores.get("Blue"));
                            for (Player p : world.getPlayers()) respawnPlayer(p);
                        } else if (team.equals("Blue")) {
                            scores.compute("Blue", (key, val) -> val + 1);
                            broadcast(ChatColor.BLUE + playerName + " scored for Team Blue! " + ChatColor.RED + scores.get("Red") + ChatColor.WHITE + " - " + ChatColor.BLUE + scores.get("Blue"));
                            for (Player p : world.getPlayers()) respawnPlayer(p);
                        }
                    } else {
                        broadcast(playerName + " has tried to scored in their own portal! Shame on them!");
                        respawnPlayer(player);
                    }

                    // Display score when someone scores
                    String title = ChatColor.RED + Integer.toString(scores.get("Red")) + ChatColor.WHITE + " - " + ChatColor.BLUE + Integer.toString(scores.get("Blue")), subtitle = ChatColor.valueOf(team.toUpperCase()) + player.getName() + " scored!";
                    for (Player p : world.getPlayers()) p.sendTitle(title, subtitle, 5, 30, 10);
                    updateScoreboard();
                }
            }
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.getDrops().clear();

        Player player = event.getEntity();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentState != State.WAITING) {
                player.spigot().respawn();
                respawnPlayer(player);
            } else {
                player.spigot().respawn();
                player.teleport(spawnLoc);
            }
        }, 1L);
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check if a game is currently in progress
        Player player = event.getPlayer();
        if (currentState != State.WAITING) plugin.sendError(player, "A game is already in progress!");
        else {
            if (world.getPlayers().size() > teamSize * 2) {
                plugin.sendError(player, "This game already has enough players.");
                player.teleport(Bukkit.getWorld("world").getSpawnLocation());
            }
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            if (currentState != State.PLAYTIME) event.setCancelled(true);
            Player attacker;
            switch (event.getDamager()) {
                case Player player -> attacker = player;
                case Arrow arrow -> {
                    if (arrow.getShooter() instanceof Player player) attacker = player;
                    else return;
                }
                default -> {
                    return;
                }
            }

            if (getTeam(victim.getName()).equals(getTeam(attacker.getName()))) event.setCancelled(true);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().getY() > 100) event.setCancelled(true);
        if (Math.abs(event.getBlock().getLocation().getZ()) > 20) event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getY() > 100) cancelAndSendErrorOnActionBar(event, "You cannot build this high up!");
        else if (Math.abs(event.getBlock().getZ()) > 20) cancelAndSendErrorOnActionBar(event, "You cannot build in this area!");
        else if (Math.abs(event.getBlock().getX()) > 4) cancelAndSendErrorOnActionBar(event, "You cannot build this far out!");
    }

    private void cancelAndSendErrorOnActionBar(BlockPlaceEvent event, String error) {
        event.setCancelled(true);
        event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + error));
    }
}
