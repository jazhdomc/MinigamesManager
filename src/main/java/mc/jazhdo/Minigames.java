package mc.jazhdo;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.TextComponent;

public class Minigames extends JavaPlugin {
    private final List<Integer> supportedTeamSizes = List.of(1, 2, 5, 10);
    private final Map<String, Function<GameArgs, Game>> gameTypes = new HashMap<>();
    private final Map<String, Map<Integer, Game>> games = new HashMap<>();
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
                // Validate arguments
                if (args.length == 2) {
                    // Get and validate game class
                    Function<GameArgs, Game> gameClass = gameTypes.get(args[0]);
                    if (gameClass == null) {
                        sendError(player, "Game " + args[0] + " is not a type of game. Allowed types: " + String.join(", ", gameTypes.keySet()));
                        return true;
                    }

                    // Get and validate team size
                    int teamSize;
                    try {
                        teamSize = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sendError(player, "The team size " + args[1] + " is not a valid number. Make sure it's a number.");
                        return true;
                    }
                    if (!supportedTeamSizes.contains(teamSize)) {
                        sendError(player, "The team size is not a valid size. Valid sizes: " + String.join(", ", supportedTeamSizes.stream().map(String::valueOf).toList()));
                        return true;
                    }

                    // Get into an existing game if there is space in one
                    Map<Integer, Game> gameList = games.get(args[0]);
                    for (Game game : gameList.values())
                        if (game.hasSpace()) {
                            player.teleport(game.getSpawnLocation());
                            return true;
                        }

                    // Make a new game in a existing spot if possible
                    for (int index : gameList.keySet())
                        if (gameList.get(index) == null) {
                            Game newGame = gameClass.apply(new GameArgs(plugin, teamSize, index, args[0]));
                            gameList.put(index, newGame);
                            player.teleport(newGame.getSpawnLocation());
                            return true;
                        }

                    // If there are no empty spots, create a new one
                    int index = gameList.keySet().size();
                    Game newGame = gameClass.apply(new GameArgs(plugin, teamSize, index, args[0]));
                    gameList.put(index, newGame);
                    player.teleport(newGame.getSpawnLocation());
                } else 
                    switch (args.length) {
                        case 0 -> sendError(player, "<game> argument required. (/join <game> <team_size>)");
                        case 1 -> sendError(player, "<team_size> argument required. (/join " + args[0] + " <team_size>)");
                        default -> sendError(player, "Too many arguments. Required amount: 2 (/join <game> <team_size>)");
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

    @Override
    public void onEnable() {
        log = getLogger();
        log.log(Level.INFO, "Starting...");

        // Setup config
        saveDefaultConfig();

        // Setup listeners
        getCommand("join").setExecutor(new Commands(this));

        // Setup games
        gameTypes.put("Bridge", BridgeGame::new);
        for (String key : gameTypes.keySet()) games.put(key, new HashMap<>());

        worldContainer = Bukkit.getWorldContainer();
    }
    
    @Override
    public void onDisable() {
        log.log(Level.INFO, "Shutting down...");
    }
}
