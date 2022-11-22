package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;

import java.util.logging.Logger;

public class ConsoleLogger {
    private final Logger logger;
    private final String message;

    public ConsoleLogger(String message, Logger logger) {
        this.message = message;
        this.logger = logger;
    }

    public void log(String prefix) {
        logger.info(ChatColor.stripColor(prefix + message));
    }

    public void log() {
        log("");
    }
}