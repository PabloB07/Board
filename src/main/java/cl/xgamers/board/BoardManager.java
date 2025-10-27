package cl.xgamers.board;

import fr.mrmicky.fastboard.FastBoard;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardManager {

    private final Board plugin;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private final Map<UUID, Boolean> toggledOff = new HashMap<>();
    private int headerAnimationIndex = 0;
    private int footerAnimationIndex = 0;
    private int tickCounter = 0;

    public BoardManager(Board plugin) {
        this.plugin = plugin;
    }

    public void createBoard(Player player) {
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        updateBoard(player);
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void updateBoard(Player player) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || isToggledOff(player)) return;

        // Update title
        String title = plugin.getConfig().getString("title", "Board");
        board.updateTitle(Hex.colorize(title));

        // Get current header and footer based on animation
        List<String> headers = plugin.getConfig().getStringList("header.lines");
        List<String> footers = plugin.getConfig().getStringList("footer.lines");
        List<String> lines = plugin.getConfig().getStringList("lines");

        String header = "";
        if (!headers.isEmpty()) {
            if (plugin.getConfig().getBoolean("header.animation.enabled", true)) {
                header = Hex.colorize(headers.get(headerAnimationIndex % headers.size()));
            } else {
                header = Hex.colorize(headers.get(0));
            }
        }

        String footer = "";
        if (!footers.isEmpty()) {
            if (plugin.getConfig().getBoolean("footer.animation.enabled", true)) {
                footer = Hex.colorize(footers.get(footerAnimationIndex % footers.size()));
            } else {
                footer = Hex.colorize(footers.get(0));
            }
        }

        // Combine lines
        String[] boardLines = new String[lines.size() + (headers.isEmpty() ? 0 : 1) + (footers.isEmpty() ? 0 : 1)];
        int index = 0;
        if (!headers.isEmpty()) {
            boardLines[index++] = header;
        }
        for (String line : lines) {
            boardLines[index++] = Hex.colorize(replacePlaceholders(line, player));
        }
        if (!footers.isEmpty()) {
            boardLines[index++] = footer;
        }

        board.updateLines(boardLines);
    }

    private String replacePlaceholders(String line, Player player) {
        // Simple placeholder replacement (fallback if PAPI not available)
        line = line.replace("%board_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        line = line.replace("%board_lobby_connected%", String.valueOf(plugin.getServerCount("lobby")));
        line = line.replace("%board_lobby_online%", String.valueOf(plugin.getServerCount("lobby")));
        line = line.replace("%board_lobby_max%", getServerMaxPlayers("lobby"));
        line = line.replace("%board_vanilla_online%", String.valueOf(plugin.getServerCount("vanilla")));
        line = line.replace("%board_vanilla_max%", getServerMaxPlayers("vanilla"));
        line = line.replace("%board_fabric_online%", String.valueOf(plugin.getServerCount("Fabric")));
        line = line.replace("%board_fabric_max%", getServerMaxPlayers("Fabric"));

        // PlaceholderAPI support
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            line = PlaceholderAPI.setPlaceholders(player, line);
        }

        return line;
    }

    private String getServerMaxPlayers(String serverName) {
        for (Object serverObj : plugin.getConfig().getList("servers", java.util.Collections.emptyList())) {
            if (serverObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> server = (java.util.Map<String, Object>) serverObj;
                if (serverName.equals(server.get("name"))) {
                    return String.valueOf(server.get("max_players"));
                }
            }
        }
        return "0";
    }

    public void updateAllBoards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateBoard(player);
        }
        tickCounter++;
        // Update animation indices based on config
        int headerInterval = plugin.getConfig().getInt("header.animation.interval", 20);
        int footerInterval = plugin.getConfig().getInt("footer.animation.interval", 20);
        if (plugin.getConfig().getBoolean("header.animation.enabled", true) && tickCounter % headerInterval == 0) {
            headerAnimationIndex++;
        }
        if (plugin.getConfig().getBoolean("footer.animation.enabled", true) && tickCounter % footerInterval == 0) {
            footerAnimationIndex++;
        }
    }

    public void toggleBoard(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentlyOff = toggledOff.getOrDefault(uuid, false);
        toggledOff.put(uuid, !currentlyOff);
        if (!currentlyOff) {
            // Turning off
            FastBoard board = boards.get(uuid);
            if (board != null) {
                board.updateLines(); // Clear lines
            }
        } else {
            // Turning on
            updateBoard(player);
        }
    }

    public boolean isToggledOff(Player player) {
        return toggledOff.getOrDefault(player.getUniqueId(), false);
    }

    public void reload() {
        plugin.reloadConfig();
        headerAnimationIndex = 0;
        footerAnimationIndex = 0;
        tickCounter = 0;
        updateAllBoards();
    }
}