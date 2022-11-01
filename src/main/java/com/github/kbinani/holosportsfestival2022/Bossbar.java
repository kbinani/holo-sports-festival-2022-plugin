package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

class Bossbar {
    private final JavaPlugin owner;
    private final String id;
    private int value = 0;
    private int max = 100;
    private String color = "white";
    private boolean visible = false;
    private final BoundingBox box;
    private @Nullable BukkitTask timer;
    private String players = "";

    Bossbar(JavaPlugin owner, String id, String title, BoundingBox box) {
        this.owner = owner;
        this.id = id;
        this.box = box;
        clear(id);
        execute(String.format("bossbar add %s \"%s\"", id, title));
    }

    void dispose() {
        clear(id);
        if (timer != null) {
            timer.cancel();;
            timer = null;
        }
    }

    int getMax() {
        return max;
    }

    void setMax(int m) {
        if (m != max) {
            max = m;
            execute(String.format("bossbar set %s max %d", id, max));
        }
    }

    int getValue() {
        return this.value;
    }

    void setValue(int v) {
        if (v != value) {
            value = v;
            execute(String.format("bossbar set %s value %d", id, value));
        }
    }

    String getColor() {
        return color;
    }

    void setColor(String c) {
        if (!c.equals(color)) {
            color = c;
            execute(String.format("bossbar set %s color %s", id, color));
        }
    }

    void setVisible(boolean b) {
        if (b == visible) {
            return;
        }
        visible = b;
        Server server = owner.getServer();
        BukkitScheduler scheduler = server.getScheduler();
        if (visible) {
            updatePlayers();
            if (timer != null) {
                timer.cancel();
            }
            timer = scheduler.runTaskTimer(owner, this::updatePlayers, 20, 20);
        } else {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
        execute(String.format("bossbar set %s visible %s", id, visible ? "true" : "false"));
    }

    private boolean isInField(Player player) {
        Location location = player.getLocation();
        return player.getWorld().getEnvironment() == World.Environment.NORMAL && box.contains(location.getX(), location.getY(), location.getZ());
    }

    private void updatePlayers() {
        Server server = owner.getServer();
        String players = server.getOnlinePlayers().stream().filter(this::isInField).map(Player::getName).sorted().collect(Collectors.joining(","));
        if (!players.equals(this.players)) {
            this.players = players;
            // set players が同じだとコンソールにエラーが出てうるさいので変わった時だけ set players する
            execute(String.format("bossbar set %s players @a[x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f]", id, box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ()));
        }
    }

    private void clear(String id) {
        execute(String.format("bossbar remove %s", id));
    }

    private void execute(String cmd) {
        Server server = owner.getServer();
        server.dispatchCommand(server.getConsoleSender(), cmd);
    }
}