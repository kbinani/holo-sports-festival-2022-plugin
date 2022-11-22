package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public class Players {
    private Players() {
    }

    public static void Within(BoundingBox[] boxes, Consumer<Player> callback) {
        Server server = Bukkit.getServer();
        server.getOnlinePlayers().forEach(player -> {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                return;
            }
            Vector location = player.getLocation().toVector();
            for (BoundingBox box : boxes) {
                if (box.contains(location)) {
                    callback.accept(player);
                    break;
                }
            }
        });
    }

    public static void Within(BoundingBox box, Consumer<Player> callback) {
        Within(new BoundingBox[]{box}, callback);
    }
}