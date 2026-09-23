package mc.jazhdo;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class TntRun extends Game {
    private enum State {
        QUEUEING,
        PLAYING,
        ENDGAME
    }
    private State state = State.QUEUEING;
    private final HashSet<Player> alive = new HashSet<>(), dead = new HashSet<>();
    private final BossBar bossBar;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private int countdown = 60;
    private BukkitTask gameLoop = null;

    public TntRun(GameArgs args) {
        super(args);

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("Tnt Run", "dummy");
        objective.getScore(ChatColor.GREEN + "Alive").setScore(0);
        objective.getScore(ChatColor.RED + "Dead").setScore(0);
        bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Waiting for players.", BarColor.RED, BarStyle.SEGMENTED_20);
    }

    @Override
    public boolean hasSpace() {
        return state == State.QUEUEING && alive.size() < 12;
    }

    @Override
    public String getMap() {
        List<String> maps = gameConfig.getStringList("maps");
        return maps.get((new Random()).nextInt(0, maps.size()));
    }

    @Override
    public void start() {
        enterWaitingLoop();
    }
    private void enterWaitingLoop() {
        new BukkitRunnable() {
            int dots = 1;

            @Override 
            public void run() {
                if (alive.size() < 2) {
                    dots++;
                    if (dots == 4) dots = 1;
                    bossBar.setTitle(ChatColor.GOLD + "Waiting for players" + ".".repeat(dots));
                } else {
                    // Start 60s countdown
                    bossBar.setTitle(ChatColor.GOLD + "Game starting in 60s");
                    enterCountdownLoop();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0l, 20l);
    }
    private void enterCountdownLoop() {
        new BukkitRunnable() {
            @Override 
            public void run() {
                if (alive.size() > 1) {
                    countdown--;
                    if (countdown == 0) {
                        // Remove floor to start
                        int startX = gameConfig.getInt("floor-remove.corner1.x"), startY = gameConfig.getInt("floor-remove.corner1.y"), startZ = gameConfig.getInt("floor-remove.corner1.z"),
                        endX = gameConfig.getInt("floor-remove.corner2.x"), endY = gameConfig.getInt("floor-remove.corner2.y"), endZ = gameConfig.getInt("floor-remove.corner2.z");
                        for (int x = startX; x <= endX; x++) {
                            for (int y = startY; y <= endY; y++) {
                                for (int z = startZ; z <= endZ; z++) world.getBlockAt(x, y, z).setType(Material.AIR, false);
                            }
                        }

                        // Setup new state
                        state = State.PLAYING;
                        bossBar.setTitle(ChatColor.GOLD + "Game duration: 00:00");
                        for (Player p : world.getPlayers()) p.setGameMode(GameMode.SURVIVAL);
                    
                        // Start game loop
                        gameLoop = new BukkitRunnable() {
                            int stopwatch = 0;

                            @Override
                            public void run() {
                                stopwatch++;
                                String minutes = Integer.toString(stopwatch / 60), seconds = Integer.toString(stopwatch % 60);
                                bossBar.setTitle(ChatColor.GOLD + "Game duration: " + (minutes.length() < 2 ? ("0" + minutes) : minutes) + ":" + (seconds.length() < 2 ? ("0" + seconds) : seconds));
                            }
                        }.runTaskTimer(plugin, 20l, 20l);
                        this.cancel();
                    } else {
                        // Countdown and milestone alerts
                        bossBar.setTitle(ChatColor.BLUE + "Game starting in " + Integer.toString(countdown) + " second" + (countdown != 1 ? "s" : ""));
                        if (countdown % 10 == 0) broadcast(ChatColor.BLUE + "Game starting in " + Integer.toString(countdown) + " seconds.");
                        else if (countdown == 1) broadcast(ChatColor.BLUE + "Game starting in 1 second.");
                        else if (countdown < 10) broadcast(ChatColor.BLUE + "Game starting in " + Integer.toString(countdown) + " seconds.");
                    }
                } else {
                    broadcast(ChatColor.RED + "Not enough players, resetting countdown.");
                    bossBar.setTitle(ChatColor.GOLD + "Waiting for players.");
                    this.cancel();
                    enterWaitingLoop();
                }
            }
        }.runTaskTimer(plugin, 20l, 20l);
    }

    // Join / Leaves
    @Override 
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Only check for world changes
        if (event.getFrom().getWorld() == event.getTo().getWorld()) return;

        // Update playercount
        Player player = event.getPlayer();
        alive.add(player);

        // Join message
        broadcast(ChatColor.YELLOW + "Player " + player.getName() + " has joined. " + ChatColor.AQUA + "(" + ChatColor.GOLD + Integer.toString(alive.size()) + "/12" + ChatColor.AQUA + ")");

        // Update displays
        bossBar.addPlayer(player);
        updateScoreboard();
        player.setScoreboard(scoreboard);

        // Set initial player state
        player.setGameMode(GameMode.ADVENTURE);
    }
    @Override
    public void attemptLeave(Player player) {
        // Update playercount
        if (state == State.QUEUEING) alive.remove(player);
        else {
            if (alive.contains(player)) alive.remove(player);
            else dead.remove(player);
        }

        // Leave message
        broadcast(ChatColor.YELLOW + "Player " + player.getName() + " has left.");

        // Update displays
        bossBar.removePlayer(player);
        updateScoreboard();
    }
    @Override 
    public void onPlayerQuit(PlayerQuitEvent event) {
        attemptLeave(event.getPlayer());
        if (state == State.PLAYING) checkEndGame();
    }
    
    // Tnt (and sand) deletion system
    @Override 
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only fire on block changes (head movements count in this event)
        Location from = event.getFrom(), to = event.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;

        if (state == State.PLAYING && event.getPlayer().isOnGround()) {
            plugin.scheduler.runTaskLater(plugin, () -> {
                // Remove sand and tnt below the player after a delay to give them time to jump off
                to.clone().add(0, -1, 0).getBlock().setType(Material.AIR, false);
                to.clone().add(0, -2, 0).getBlock().setType(Material.AIR, false);
            }, 10l);
        }
    }

    private void broadcast(String msg) {
        for (Player p : world.getPlayers()) p.sendMessage(ChatColor.GOLD + "[Tnt Run] " + ChatColor.WHITE + msg);
    }
    private void broadcastExcluding(String msg, Player excluded) {
        List<Player> playerlist = world.getPlayers();
        playerlist.remove(excluded);
        for (Player player : playerlist) player.sendMessage(msg);
    }

    private void updateScoreboard() {
        objective.getScore(ChatColor.GREEN + "Alive").setScore(alive.size());
        objective.getScore(ChatColor.RED + "Dead").setScore(dead.size());
    }

    // End game
    private void checkEndGame() {
        if (alive.size() < 2) {
            if (alive.isEmpty()) endGame(null);
            else endGame(alive.iterator().next());
        }
    }
    private void endGame(Player winner) {
        state = State.ENDGAME;
        if (gameLoop != null && !gameLoop.isCancelled()) gameLoop.cancel();
        if (winner != null) {
            broadcastExcluding(ChatColor.GREEN + winner.getName() + " has won Tnt Run!", winner);
            winner.sendMessage(ChatColor.GREEN + "You have won Tnt Run!");
        } else broadcast(ChatColor.GREEN + "Nobody has won Tnt Run. It was a tie.");
        new BukkitRunnable() {
            private final BossBar lobbyTeleportationCounter = Bukkit.createBossBar("Teleporting to the lobby in 10", BarColor.GREEN, BarStyle.SEGMENTED_20);
            private int countdown = 11;

            {
                for (Player player : world.getPlayers()) lobbyTeleportationCounter.addPlayer(player);
            }

            @Override 
            public void run() {
                if (countdown == 1) {
                    lobbyTeleportationCounter.removeAll();
                    bossBar.removeAll();
                    for (Player player : world.getPlayers()) plugin.resetPlayer2Lobby(player);
                    resetWorld();
                } else {
                    countdown--;
                    lobbyTeleportationCounter.setTitle("Teleporting to the lobby in " + Integer.toString(countdown));
                }
            }
        }.runTaskTimer(plugin, 0l, 20l);
    }

    // Rebirth management
    @Override 
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Death message
        Player player = event.getEntity();
        event.setDeathMessage(null);
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        String deathMessage = switch (lastDamage.getCause()) {
            case FALL -> "fall damage";
            case ENTITY_ATTACK -> {
                Player killer = player.getKiller();
                yield killer == null ? "a entity" : killer.getName();
            }
            case VOID -> "the void";
            case LIGHTNING -> "a lightning strike";
            case STARVATION -> "starvation";
            case CRAMMING -> "entity cramming";
            default -> null;
        };
        broadcast(ChatColor.DARK_RED + "Player " + ChatColor.RED + player.getName() + ChatColor.DARK_RED + " has died" + (deathMessage == null ? "." : (" to " + ChatColor.GREEN + deathMessage + ".")));

        if (state == State.QUEUEING) event.setKeepInventory(true);
        plugin.scheduler.runTaskLater(plugin, () -> player.spigot().respawn(), 1l);
    }
    @Override 
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Use vanilla respawn mechanics
        event.setRespawnLocation(spawnLoc);
        
        // Reset gamemodes
        plugin.scheduler.runTaskLater(plugin, () -> {
            event.getPlayer().setGameMode(state == State.QUEUEING ? GameMode.ADVENTURE : GameMode.SPECTATOR);
            if (state == State.PLAYING) {
                Player player = event.getPlayer();
                alive.remove(player);
                dead.add(player);
                checkEndGame();
                updateScoreboard();
            }
        }, 1l);
    }

    // Damage managers
    @Override 
    public void onEntityDamage(EntityDamageEvent event) {
        // Cancel fall damage (to prevent death by just falling)
        if (event.getCause() == DamageCause.FALL) event.setCancelled(true);
        else if (state == State.QUEUEING) event.setCancelled(true);
    }
    @Override 
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Cancel queueing attacks
        if (state == State.QUEUEING) event.setCancelled(true);
    }
}
