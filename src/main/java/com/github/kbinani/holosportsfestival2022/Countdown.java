package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Countdown {

    private Countdown() {
    }

    public static void Then(BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
        Server server = plugin.getServer();
        BukkitScheduler scheduler = server.getScheduler();

        List<String> selectors = new ArrayList<>();
        for (BoundingBox boundingBox : boxes) {
            String format = String.format("@a[%s]", TargetSelector.Of(boundingBox));
            selectors.add(format);
        }

        if (!countdown.test(3)) {
            return;
        }
        for (BoundingBox box : boxes) {
            Play.Note(server, box, Instrument.BIT, new Note(12));
        }
        for (String selector : selectors) {
            server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 3));
        }

        scheduler.runTaskLater(plugin, () -> {
            if (!countdown.test(2)) {
                return;
            }
            for (BoundingBox box : boxes) {
            Play.Note(server, box, Instrument.BIT, new Note(12));
            }
            for (String selector : selectors) {
                server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 2));
            }

            scheduler.runTaskLater(plugin, () -> {
                if (!countdown.test(1)) {
                    return;
                }
                for (BoundingBox box : boxes) {
                    Play.Note(server, box, Instrument.BIT, new Note(12));
                }
                for (String selector : selectors) {
                    server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 1));
                }
                scheduler.runTaskLater(plugin, () -> {
                    if (!task.get()) {
                        return;
                    }
                    for (BoundingBox box : boxes) {
                        Play.Sound(server, box, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                    }
                    for (String selector : selectors) {
                        server.dispatchCommand(server.getConsoleSender(), String.format("title %s title \"START!!!\"", selector));
                    }
                }, delay);
            }, delay);
        }, delay);
    }

    public static void Then(BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task) {
        Then(boxes, plugin, countdown, task, 20);
    }
}