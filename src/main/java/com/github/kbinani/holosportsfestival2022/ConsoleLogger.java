package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Server;

public class ConsoleLogger {
    private final String message;

    public ConsoleLogger(String message) {
        this.message = message;
    }

    public void log() {
        Server server = Bukkit.getServer();
        server.getLogger().info(message);
    }
}