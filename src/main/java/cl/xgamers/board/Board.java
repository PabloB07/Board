package cl.xgamers.board;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Board extends JavaPlugin implements Listener, PluginMessageListener {

    private BoardManager boardManager;
    private BukkitTask updateTask;
    private final Map<String, Integer> serverCounts = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        boardManager = new BoardManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Register command
        getCommand("board").setExecutor(new BoardCommand(this));

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new BoardExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registrado correctamente.");
        }

        // Register plugin messaging
        getServer().getMessenger().registerOutgoingPluginChannel(this, "serverconnector:main");
        getServer().getMessenger().registerIncomingPluginChannel(this, "serverconnector:main", this);

        // Create boards for online players
        for (Player player : getServer().getOnlinePlayers()) {
            boardManager.createBoard(player);
        }

        scheduleBoardUpdates();

        // Request server counts periodically
        getServer().getScheduler().runTaskTimer(this, this::requestServerCounts, 0L, 600L); // Every 30 seconds
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (updateTask != null) {
            updateTask.cancel();
        }
        // Unregister channels
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        // Remove all boards
        for (Player player : getServer().getOnlinePlayers()) {
            boardManager.removeBoard(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boardManager.createBoard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        boardManager.removeBoard(event.getPlayer());
    }

    public BoardManager getBoardManager() {
        return boardManager;
    }

    public void scheduleBoardUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        int interval = Math.max(1, getConfig().getInt("animations.interval", 20));
        updateTask = getServer().getScheduler().runTaskTimer(this, boardManager::updateAllBoards, 0L, interval);
    }

    public void rescheduleBoardUpdates() {
        scheduleBoardUpdates();
    }

    private void requestServerCounts() {
        // Send request to Velocity for server counts
        if (!getServer().getOnlinePlayers().isEmpty()) {
            try (ByteArrayOutputStream b = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(b)) {
                out.writeUTF("GetServerCounts"); // Subchannel for request
                // Send to a random online player (Velocity will handle it)
                Player player = getServer().getOnlinePlayers().iterator().next();
                player.sendPluginMessage(this, "serverconnector:main", b.toByteArray());
                getLogger().info("Sent server count request to Velocity via " + player.getName());
            } catch (IOException e) {
                getLogger().warning("Error sending server count request: " + e.getMessage());
            }
        } else {
            getLogger().warning("No online players to send server count request");
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals("serverconnector:main")) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();
            if ("ServerCount".equals(subchannel)) {
                String server = in.readUTF();
                int count = in.readInt();
                serverCounts.put(server, count);
                getLogger().info("Received server count for " + server + ": " + count);
            } else {
                getLogger().info("Received unknown subchannel: " + subchannel);
            }
        } catch (IOException e) {
            getLogger().warning("Error reading plugin message: " + e.getMessage());
        }
    }

    public int getServerCount(String server) {
        return serverCounts.getOrDefault(server, 0);
    }
}
