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

    public record Title(String title, String subtitle) {
    }

    public static class TitleSet {
        public Title three;
        public Title two;
        public Title one;
        public Title zero;

        public TitleSet(Title three, Title two, Title one, Title zero) {
            this.three = three;
            this.two = two;
            this.one = one;
            this.zero = zero;
        }

        public static TitleSet Default() {
            var three = new Title("3", "");
            var two = new Title("2", "");
            var one = new Title("1", "");
            var zero = new Title("START!!!", "");
            return new TitleSet(three, two, one, zero);
        }
    }

    public static void Then(World world, BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
        Then(world, boxes, plugin, countdown, task, delay, TitleSet.Default());
    }

    public static void Then(World world, BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay, TitleSet titleSet) {
        Server server = plugin.getServer();
        BukkitScheduler scheduler = server.getScheduler();

        if (!countdown.test(3)) {
            return;
        }
        Players.Within(world, boxes, player -> {
            player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
            player.sendTitle(titleSet.three.title, titleSet.three.subtitle, 10, 70, 20);
        });

        scheduler.runTaskLater(plugin, () -> {
            if (!countdown.test(2)) {
                return;
            }
            Players.Within(world, boxes, player -> {
                player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
                player.sendTitle(titleSet.two.title, titleSet.two.subtitle, 10, 70, 20);
            });

            scheduler.runTaskLater(plugin, () -> {
                if (!countdown.test(1)) {
                    return;
                }
                Players.Within(world, boxes, player -> {
                    player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
                    player.sendTitle(titleSet.one.title, titleSet.one.subtitle, 10, 70, 20);
                });
                scheduler.runTaskLater(plugin, () -> {
                    if (!task.get()) {
                        return;
                    }
                    Players.Within(world, boxes, player -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                        player.sendTitle(titleSet.zero.title, titleSet.zero.subtitle, 10, 70, 20);
                    });
                }, delay);
            }, delay);
        }, delay);
    }
}