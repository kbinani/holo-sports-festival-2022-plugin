package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;

import java.util.logging.Logger;

public class ConsoleLogger {
    private final Logger logger;
    private final String message;
    private final String prefix;

    public ConsoleLogger(String message, String prefix, Logger logger) {
        this.message = message;
        this.logger = logger;
        this.prefix = prefix;
    }

    public void log() {
        if (prefix.isEmpty()) {
            logger.info(ChatColor.stripColor(message));
        } else if (message.startsWith(prefix)) {
            String body = message.substring(prefix.length());
            if (body.startsWith(" ")) {
                logger.info(ChatColor.stripColor(message));
            } else {
                logger.info(ChatColor.stripColor(prefix + " " + body));
            }
        } else {
            if (message.startsWith(" ")) {
                logger.info(ChatColor.stripColor(prefix + message));
            } else {
                logger.info(ChatColor.stripColor(prefix + " " + message));
            }
        }
    }
}