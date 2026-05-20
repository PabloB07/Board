package cl.xgamers.board;

import fr.mrmicky.fastboard.FastBoard;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardManager {

    private final Board plugin;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private final Map<UUID, Boolean> toggledOff = new HashMap<>();
    private int titleAnimationIndex = 0;
    private int headerAnimationIndex = 0;
    private int footerAnimationIndex = 0;
    private long elapsedTicks = 0;

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

        board.updateTitle(Hex.colorize(getAnimatedLine("title", titleAnimationIndex, "Board")));

        List<String> headers = plugin.getConfig().getStringList("header.lines");
        List<String> footers = plugin.getConfig().getStringList("footer.lines");
        List<String> lines = plugin.getConfig().getStringList("lines");

        String header = getAnimatedLine("header", headerAnimationIndex, "");
        String footer = getAnimatedLine("footer", footerAnimationIndex, "");

        // FastBoard: index 0 = bottom, last index = top (below title)
        int extraLines = (footers.isEmpty() ? 0 : 1) + (headers.isEmpty() ? 0 : 1);
        String[] boardLines = new String[lines.size() + extraLines];
        int index = 0;

        if (!footers.isEmpty()) {
            boardLines[index++] = footer.isEmpty() ? "" : footer;
        }
        for (String line : lines) {
            boardLines[index++] = Hex.colorize(replacePlaceholders(line, player));
        }
        if (!headers.isEmpty()) {
            boardLines[index++] = header.isEmpty() ? "" : header;
        }

        board.updateLines(boardLines);
    }

    private String getAnimatedLine(String section, int animationIndex, String fallback) {
        List<String> sectionLines = getSectionLines(section);
        if (sectionLines.isEmpty()) {
            return fallback;
        }
        if (!shouldAnimate(section)) {
            return Hex.colorize(sectionLines.getFirst());
        }
        return Hex.colorize(sectionLines.get(animationIndex % sectionLines.size()));
    }

    private List<String> getSectionLines(String section) {
        if ("title".equals(section)) {
            if (plugin.getConfig().isString("title")) {
                String singleTitle = plugin.getConfig().getString("title");
                return singleTitle != null && !singleTitle.isBlank()
                        ? List.of(singleTitle)
                        : Collections.emptyList();
            }
            List<String> titleLines = plugin.getConfig().getStringList("title.lines");
            if (!titleLines.isEmpty()) {
                return titleLines;
            }
            String legacyTitle = plugin.getConfig().getString("title", "Board");
            return legacyTitle != null && !legacyTitle.isBlank()
                    ? List.of(legacyTitle)
                    : Collections.emptyList();
        }
        return plugin.getConfig().getStringList(section + ".lines");
    }

    private boolean shouldAnimate(String section) {
        if (!plugin.getConfig().getBoolean("animations.enabled", true)) {
            return false;
        }
        List<String> sectionLines = getSectionLines(section);
        if (sectionLines.size() < 2) {
            return false;
        }
        return plugin.getConfig().getBoolean(section + ".animation.enabled", false);
    }

    private String replacePlaceholders(String line, Player player) {
        line = line.replace("%board_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        line = line.replace("%board_lobby_connected%", String.valueOf(plugin.getServerCount("lobby")));
        line = line.replace("%board_lobby_online%", String.valueOf(plugin.getServerCount("lobby")));
        line = line.replace("%board_lobby_max%", getServerMaxPlayers("lobby"));
        line = line.replace("%board_vanilla_online%", String.valueOf(plugin.getServerCount("vanilla")));
        line = line.replace("%board_vanilla_max%", getServerMaxPlayers("vanilla"));
        line = line.replace("%board_fabric_online%", String.valueOf(plugin.getServerCount("fabric")));
        line = line.replace("%board_fabric_max%", getServerMaxPlayers("fabric"));

        for (Object serverObj : plugin.getConfig().getList("servers", Collections.emptyList())) {
            if (serverObj instanceof Map<?, ?> server) {
                Object nameObj = server.get("name");
                if (nameObj == null) continue;
                String serverName = String.valueOf(nameObj);
                String key = serverName.toLowerCase();
                line = line.replace("%board_" + key + "_online%", String.valueOf(plugin.getServerCount(serverName)));
                line = line.replace("%board_" + key + "_max%", getServerMaxPlayers(serverName));
                line = line.replace("%board_" + key + "_connected%", String.valueOf(plugin.getServerCount(serverName)));
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            line = PlaceholderAPI.setPlaceholders(player, line);
        }

        return line;
    }

    private String getServerMaxPlayers(String serverName) {
        for (Object serverObj : plugin.getConfig().getList("servers", Collections.emptyList())) {
            if (serverObj instanceof Map<?, ?> server) {
                if (serverName.equals(String.valueOf(server.get("name")))) {
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

        int updateInterval = plugin.getConfig().getInt("animations.interval", 20);
        elapsedTicks += updateInterval;
        advanceAnimations();
    }

    private void advanceAnimations() {
        if (!plugin.getConfig().getBoolean("animations.enabled", true)) {
            return;
        }

        if (shouldAnimate("title") && elapsedTicks % getAnimationInterval("title") == 0) {
            titleAnimationIndex++;
        }
        if (shouldAnimate("header") && elapsedTicks % getAnimationInterval("header") == 0) {
            headerAnimationIndex++;
        }
        if (shouldAnimate("footer") && elapsedTicks % getAnimationInterval("footer") == 0) {
            footerAnimationIndex++;
        }
    }

    private int getAnimationInterval(String section) {
        int interval = plugin.getConfig().getInt(section + ".animation.interval", 20);
        return Math.max(1, interval);
    }

    public void toggleBoard(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentlyOff = toggledOff.getOrDefault(uuid, false);
        toggledOff.put(uuid, !currentlyOff);
        if (!currentlyOff) {
            FastBoard board = boards.get(uuid);
            if (board != null) {
                board.updateLines();
            }
        } else {
            updateBoard(player);
        }
    }

    public boolean isToggledOff(Player player) {
        return toggledOff.getOrDefault(player.getUniqueId(), false);
    }

    public void reload() {
        plugin.reloadConfig();
        titleAnimationIndex = 0;
        headerAnimationIndex = 0;
        footerAnimationIndex = 0;
        elapsedTicks = 0;
        plugin.rescheduleBoardUpdates();
        updateAllBoards();
    }
}
