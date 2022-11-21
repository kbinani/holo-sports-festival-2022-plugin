package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Countdown {

    private Countdown() {
    }

    public static void Then(BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
        Server server = plugin.getServer();
        BukkitScheduler scheduler = server.getScheduler();

        if (!countdown.test(3)) {
            return;
        }
        for (BoundingBox box : boxes) {
            Play.Note(server, box, Instrument.BIT, new Note(12));
            Players.Within(box, player -> {
                player.sendTitle("3", "", 10, 70, 20);
            });
        }

        scheduler.runTaskLater(plugin, () -> {
            if (!countdown.test(2)) {
                return;
            }
            for (BoundingBox box : boxes) {
                Play.Note(server, box, Instrument.BIT, new Note(12));
                Players.Within(box, player -> {
                    player.sendTitle("2", "", 10, 70, 20);
                });
            }

            scheduler.runTaskLater(plugin, () -> {
                if (!countdown.test(1)) {
                    return;
                }
                for (BoundingBox box : boxes) {
                    Play.Note(server, box, Instrument.BIT, new Note(12));
                    Players.Within(box, player -> {
                        player.sendTitle("1", "", 10, 70, 20);
                    });
                }
                scheduler.runTaskLater(plugin, () -> {
                    if (!task.get()) {
                        return;
                    }
                    for (BoundingBox box : boxes) {
                        Play.Sound(server, box, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                        Players.Within(box, player -> {
                            player.sendTitle("START!!!", "", 10, 70, 20);
                        });
                    }
                }, delay);
            }, delay);
        }, delay);
    }

    public static void Then(BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task) {
        Then(boxes, plugin, countdown, task, 20);
    }
}