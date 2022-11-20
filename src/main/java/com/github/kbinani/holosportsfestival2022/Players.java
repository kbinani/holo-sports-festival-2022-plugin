package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.function.Consumer;

public class Players {
    private Players() {
    }

    public static void Within(BoundingBox box, Consumer<Player> callback) {
        Server server = Bukkit.getServer();
        server.getOnlinePlayers().forEach(player -> {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                return;
            }
            if (box.contains(player.getLocation().toVector())) {
                callback.accept(player);
            }
        });
    }
}