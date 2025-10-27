package cl.xgamers.board;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BoardCommand implements CommandExecutor {

    private final Board plugin;

    public BoardCommand(Board plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Toggle board
            plugin.getBoardManager().toggleBoard(player);
            player.sendMessage("Board toggled.");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("board.reload")) {
                    player.sendMessage("You don't have permission to reload the board.");
                    return true;
                }
                plugin.getBoardManager().reload();
                player.sendMessage("Board reloaded.");
                return true;
            } else if (args[0].equalsIgnoreCase("toggle")) {
                plugin.getBoardManager().toggleBoard(player);
                player.sendMessage("Board toggled.");
                return true;
            }
        }

        player.sendMessage("Usage: /board [toggle|reload]");
        return true;
    }
}