package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.entity.Player;
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

    Predicate<Player> contains = (player) -> {
      Location location = player.getLocation();
      return box.contains(location.getX(), location.getY(), location.getZ()) && player.getWorld().getEnvironment() == World.Environment.NORMAL;
    };

    String selector = String.format("@a[x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f]", box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());

    if (!countdown.test(3)) {
      return;
    }
    PlayNote(server, contains, Instrument.BIT, new Note(12));
    server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 3));

    scheduler.runTaskLater(plugin, () -> {
      if (!countdown.test(2)) {
        return;
      }
      PlayNote(server, contains, Instrument.BIT, new Note(12));
      server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 2));

      scheduler.runTaskLater(plugin, () -> {
        if (!countdown.test(1)) {
          return;
        }
        PlayNote(server, contains, Instrument.BIT, new Note(12));
        server.dispatchCommand(server.getConsoleSender(), String.format("title %s title %d", selector, 1));
        scheduler.runTaskLater(plugin, () -> {
          if (!task.get()) {
            return;
          }
          PlaySound(server, contains, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
          server.dispatchCommand(server.getConsoleSender(), String.format("title %s title \"START!!!\"", selector));
        }, delay);
      }, delay);
    }, delay);
  }

  public static void Then(BoundingBox box, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task) {
    Then(box, plugin, countdown, task, 20);
  }

  private static void PlayNote(Server server, Predicate<Player> predicate, Instrument instrument, Note note) {
    server.getOnlinePlayers().stream().filter(predicate).forEach(player -> {
      player.playNote(player.getLocation(), instrument, note);
    });
  }

  private static void PlaySound(Server server, Predicate<Player> predicate, Sound sound, float volume, float pitch) {
    server.getOnlinePlayers().stream().filter(predicate).forEach(player -> {
      player.playSound(player.getLocation(), sound, volume, pitch);
    });
  }
}