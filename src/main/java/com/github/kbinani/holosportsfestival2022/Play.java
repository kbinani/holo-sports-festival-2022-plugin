package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.util.BoundingBox;

public class Play {
    public static void Note(Server server, BoundingBox box, Instrument instrument, Note note) {
        server.getOnlinePlayers().forEach(player -> {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                return;
            }
            if (box.contains(player.getLocation().toVector())) {
                player.playNote(player.getLocation(), instrument, note);
            }
        });
    }

    public static void Sound(Server server, BoundingBox box, Sound sound, float volume, float pitch) {
        server.getOnlinePlayers().forEach(player -> {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                return;
            }
            if (box.contains(player.getLocation().toVector())) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        });
    }
}