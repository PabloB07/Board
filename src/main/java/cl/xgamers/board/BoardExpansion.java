package cl.xgamers.board;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BoardExpansion extends PlaceholderExpansion {

    private final Board plugin;

    public BoardExpansion(Board plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "board";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AeroSama";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        switch (identifier.toLowerCase()) {
            case "online":
                return String.valueOf(plugin.getServer().getOnlinePlayers().size());
            case "lobby_connected":
                return String.valueOf(plugin.getServerCount("lobby"));
            case "lobby_online":
                return String.valueOf(plugin.getServerCount("lobby"));
            case "lobby_max":
                return getServerMaxPlayers("lobby");
            case "vanilla_online":
                return String.valueOf(plugin.getServerCount("vanilla"));
            case "vanilla_max":
                return getServerMaxPlayers("vanilla");
            case "fabric_online":
                return String.valueOf(plugin.getServerCount("fabric"));
            case "fabric_max":
                return getServerMaxPlayers("fabric");
            default:
                // Check for dynamic server placeholders
                if (identifier.endsWith("_online")) {
                    String serverName = identifier.substring(0, identifier.length() - 7); // Remove "_online"
                    return String.valueOf(plugin.getServerCount(serverName));
                } else if (identifier.endsWith("_max")) {
                    String serverName = identifier.substring(0, identifier.length() - 4); // Remove "_max"
                    return getServerMaxPlayers(serverName);
                }
                return null;
        }
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
}