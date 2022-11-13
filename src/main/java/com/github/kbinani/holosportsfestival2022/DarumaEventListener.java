package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DarumaEventListener implements Listener {
    private final JavaPlugin owner;
    private boolean initialized = false;

    enum Status {
        IDLE,
        RUN,
    }

    enum TeamColor {
        RED,
        YELLOW,
        WHITE,
    }

    static class Team {
        private final List<Player> players = new LinkedList<>();
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