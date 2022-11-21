package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;

public class Bossbar {
    private final MainDelegate delegate;
    private final String id;
    private int value = 0;
    private int max = 100;
    private BarColor color = BarColor.WHITE;
    private boolean visible = false;
    private final BoundingBox box;
    private @Nullable BukkitTask timer;
    private String players = "";
    private String name;
    private BossBar instance;

    public Bossbar(MainDelegate delegate, String id, String name, BoundingBox box) {
        this.delegate = delegate;
        this.id = id;
        this.box = box;
        this.name = name;
        this.instance = Bukkit.getServer().createBossBar(NamespacedKey.minecraft(id), name, color, BarStyle.SOLID);
    }

    public int getMax() {
        return max;
    }

    public void setMax(int m) {
        if (m != max) {
            max = m;
            this.instance.setProgress(value / (double) max);
        }
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int v) {
        if (v != value) {
            value = v;
            this.instance.setProgress(value / (double) max);
        }
    }

    public BarColor getColor() {
        return color;
    }

    public void setColor(BarColor c) {
        if (!c.equals(color)) {
            color = c;
            instance.setColor(color);
        }
    }

    public void setVisible(boolean b) {
        if (b == visible) {
            return;
        }
        visible = b;
        if (visible) {
            updatePlayers();
            if (timer != null) {
                timer.cancel();
            }
            timer = delegate.mainRunTaskTimer(this::updatePlayers, 20, 20);
        } else {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
        instance.setVisible(visible);
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        if (!v.equals(name)) {
            name = v;
            instance.setTitle(name);
        }
    }

    private boolean isInField(Player player) {
        Location location = player.getLocation();
        return player.getWorld().getEnvironment() == World.Environment.NORMAL && box.contains(location.getX(), location.getY(), location.getZ());
    }

    private void updatePlayers() {
        Server server = Bukkit.getServer();
        instance.removeAll();
        server.getOnlinePlayers().stream().filter(this::isInField).forEach(instance::addPlayer);
    }
}