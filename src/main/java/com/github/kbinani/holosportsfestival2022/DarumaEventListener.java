package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DarumaEventListener implements Listener {
    private final JavaPlugin owner;
    private boolean initialized = false;
    private Status _status = Status.IDLE;

    enum Status {
        IDLE,
        COUNTDOWN,
        RUN,
    }

    enum TeamColor {
        RED,
        YELLOW,
        WHITE,
    }

    static class Team {
        private final Map<UUID, Player> players = new HashMap<>();

        boolean contains(Player player) {
            return players.containsKey(player.getUniqueId());
        }

        void add(Player player) {
            players.put(player.getUniqueId(), player);
        }

        void remove(Player player) {
            players.remove(player.getUniqueId());
        }
    }

    private final Map<TeamColor, Team> teams = new HashMap<>();

    DarumaEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!initialized) {
            initialized = true;
            resetField();
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Point3i location = new Point3i(block.getLocation());
        if (location.equals(offset(kButtonYellowJoin))) {
            onClickJoin(player, TeamColor.YELLOW);
        } else if (location.equals(offset(kButtonRedJoin))) {
            onClickJoin(player, TeamColor.RED);
        } else if (location.equals(offset(kButtonWhiteJoin))) {
            onClickJoin(player, TeamColor.WHITE);
        } else if (location.equals(offset(kButtonLeave))) {
            onClickLeave(player);
        }
    }

    private void setStatus(Status status) {
        if (_status == status) {
            return;
        }
        _status = status;
        switch (_status) {
            case IDLE:
                setEntranceOpened(true);
                setStartGateOpened(false);
                break;
            case COUNTDOWN:
                setEntranceOpened(false);
                setStartGateOpened(false);
                break;
            case RUN:
                setEntranceOpened(false);
                setStartGateOpened(true);
                break;
        }
    }

    private void onClickJoin(Player player, TeamColor color) {
        if (_status != Status.IDLE) {
            return;
        }
        TeamColor current = getCurrentColor(player);
        if (current == null) {
            Team team = ensureTeam(color);
            team.add(player);
            broadcast("[だるまさんがころんだ] %sが%sにエントリーしました", player.getName(), ToColoredString(color));
        } else {
            broadcast("[だるまさんがころんだ] %sは%sにエントリー済みです", player.getName(), ToColoredString(current));
        }
    }

    static String ToColoredString(TeamColor color) {
        switch (color) {
            case RED:
                return ChatColor.RED + "TEAM RED" + ChatColor.RESET;
            case WHITE:
                return ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
            case YELLOW:
                return ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
        }
        return "";
    }

    private void broadcast(String msg, Object... args) {
        owner.getServer().broadcastMessage(String.format(msg, args));
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg, Object... args) {
        broadcast(msg, args);
    }

    private @Nonnull Team ensureTeam(TeamColor color) {
        Team team = teams.get(color);
        if (team == null) {
            team = new Team();
            teams.put(color, team);
        }
        return team;
    }

    @Nullable
    private TeamColor getCurrentColor(Player player) {
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            if (it.getValue().contains(player)) {
                return it.getKey();
            }
        }
        return null;
    }

    private void onClickLeave(Player player) {
        TeamColor current = getCurrentColor(player);
        if (current == null) {
            return;
        }
        Team team = ensureTeam(current);
        team.remove(player);
        broadcast("[だるまさんがころんだ] %sがエントリー解除しました", player.getName());
    }

    private void resetField() {
        WallSign.Place(offset(kButtonLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonRedJoin), BlockFace.SOUTH, "赤組", "エントリー");
        WallSign.Place(offset(kButtonWhiteJoin), BlockFace.SOUTH, "白組", "エントリー");
        WallSign.Place(offset(kButtonYellowJoin), BlockFace.SOUTH, "黃組", "エントリー");
        setStartGateOpened(false);
        setEntranceOpened(true);
    }

    private void setStartGateOpened(boolean open) {
        stroke(new Point3i(104, -59, -123), new Point3i(144, -59, -123), open ? "air" : "iron_bars");
    }

    private void setEntranceOpened(boolean open) {
        String block = open ? "air" : "iron_bars";
        stroke(new Point3i(103, -58, -118), new Point3i(145, -58, -118), block);
        stroke(new Point3i(144, -59, -118), new Point3i(104, -59, -118), block);
    }

    private void stroke(Point3i from, Point3i to, String block) {
        Point3i f = offset(from);
        Point3i t = offset(to);
        execute("fill %s %s %s %s %s %s %s", f.x, f.y, f.z, t.x, t.y, t.z, block);
    }

    public void execute(String format, Object... args) {
        Server server = owner.getServer();
        server.dispatchCommand(server.getConsoleSender(), String.format(format, args));
    }

    private Point3i offset(Point3i p) {
        // 座標が間違っていたらここでオフセットする
        return new Point3i(p.x, p.y, p.z);
    }

    private static final Point3i kButtonLeave = new Point3i(105, -60, -121);
    private static final Point3i kButtonRedJoin = new Point3i(107, -60, -121);
    private static final Point3i kButtonWhiteJoin = new Point3i(109, -60, -121);
    private static final Point3i kButtonYellowJoin = new Point3i(111, -60, -121);
}