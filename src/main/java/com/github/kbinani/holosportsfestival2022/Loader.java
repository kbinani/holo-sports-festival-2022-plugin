package com.github.kbinani.holosportsfestival2022;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Loader {
    private Loader() {
    }

    public static void UsingChunk(World world, BoundingBox box, JavaPlugin plugin, Consumer<World> callback) {
        int cx0 = ((int) Math.floor(box.getMinX())) >> 4;
        int cz0 = ((int) Math.floor(box.getMinZ())) >> 4;
        int cx1 = ((int) Math.ceil(box.getMaxX())) >> 4;
        int cz1 = ((int) Math.ceil(box.getMaxZ())) >> 4;
        Set<Point> added = new HashSet<>();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (world.addPluginChunkTicket(cx, cz, plugin)) {
                    added.add(new Point(cx, cz));
                }
            }
        }
        callback.accept(world);
        for (Point p : added) {
            world.removePluginChunkTicket(p.x, p.y, plugin);
        }
    }
}