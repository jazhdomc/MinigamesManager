package mc.jazhdo;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {
    private final Minigames plugin;

    public GameListener(Minigames plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Gets the game that is playing in that world or null for no game
     * 
     * @param world The world to check for
     * @return The game that is playing in that world, or null if there is none
     */
    private Game getGame(World world) {
        String worldName = world.getName();
        if (worldName.contains("Game")) {
            String[] parts = worldName.split("Game");
            if (parts.length < 2) return null;
            return plugin.getGame(parts[0], Integer.parseInt(parts[1]));
        } else return null;
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
        Game game = getGame(event.getPlayer().getWorld());
        if (game != null) game.onPlayerJoin(event);
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
}
