package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.function.Consumer;

public class Countdown {

  private Countdown() {
  }

  static void Then(JavaPlugin plugin, Consumer<Integer> countdown, Runnable task) {
    Server server = plugin.getServer();
    BukkitScheduler scheduler = server.getScheduler();
    countdown.accept(3);
    scheduler.runTaskLater(plugin, () -> {
      countdown.accept(2);
      scheduler.runTaskLater(plugin, () -> {
        countdown.accept(1);
        scheduler.runTaskLater(plugin, task, 20);
      }, 20);
    }, 20);
  }
}