package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Server;

public class Kill {
    private Kill() {

    }

    public static void Entities(String format, Object... args) {
        Server server = Bukkit.getServer();
        String selectorArg = String.format(format, args);
        String command = String.format("execute if entity @e[%s] run kill @e[%s]", selectorArg, selectorArg);
        server.dispatchCommand(server.getConsoleSender(), command);
    }
}