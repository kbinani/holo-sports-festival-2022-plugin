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

  public static void Then(BoundingBox box, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
    Server server = plugin.getServer();
    BukkitScheduler scheduler = server.getScheduler();

    String selector = String.format("@a[x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f]", box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());

    if (!countdown.test(3)) {
      return;
    }
    Play.Note(server, box, Instrument.BIT, new Note(12));
    server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 3));

    scheduler.runTaskLater(plugin, () -> {
      if (!countdown.test(2)) {
        return;
      }
      Play.Note(server, box, Instrument.BIT, new Note(12));
      server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 2));

      scheduler.runTaskLater(plugin, () -> {
        if (!countdown.test(1)) {
          return;
        }
        Play.Note(server, box, Instrument.BIT, new Note(12));
        server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 1));
        scheduler.runTaskLater(plugin, () -> {
          if (!task.get()) {
            return;
          }
          Play.Sound(server, box, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
          server.dispatchCommand(server.getConsoleSender(), String.format("title %s title \"START!!!\"", selector));
        }, delay);
      }, delay);
    }, delay);
  }

  public static void Then(BoundingBox box, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task) {
    Then(box, plugin, countdown, task, 20);
  }
}