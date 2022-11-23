package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Countdown {

    private Countdown() {
    }

    public static void Then(World world, BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
        Server server = plugin.getServer();
        BukkitScheduler scheduler = server.getScheduler();

        if (!countdown.test(3)) {
            return;
        }
        Players.Within(world, boxes, player -> {
            player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
            player.sendTitle("3", "", 10, 70, 20);
        });

        scheduler.runTaskLater(plugin, () -> {
            if (!countdown.test(2)) {
                return;
            }
            Players.Within(world, boxes, player -> {
                player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
                player.sendTitle("2", "", 10, 70, 20);
            });

            scheduler.runTaskLater(plugin, () -> {
                if (!countdown.test(1)) {
                    return;
                }
                Players.Within(world, boxes, player -> {
                    player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
                    player.sendTitle("1", "", 10, 70, 20);
                });
                scheduler.runTaskLater(plugin, () -> {
                    if (!task.get()) {
                        return;
                    }
                    Players.Within(world, boxes, player -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                        player.sendTitle("START!!!", "", 10, 70, 20);
                    });
                }, delay);
            }, delay);
        }, delay);
    }
}