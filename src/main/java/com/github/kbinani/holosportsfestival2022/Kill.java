package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.util.BoundingBox;

public class Kill {
    private Kill() {

    }

    public static void Entities(BoundingBox box, String format, Object... args) {
        Server server = Bukkit.getServer();
        String selectorArg = String.format(format, args);
        String command = String.format("execute if entity @e[%s,%s] run kill @e[%s,%s]", selectorArg, TargetSelector.Of(box), selectorArg, TargetSelector.Of(box));
        server.dispatchCommand(server.getConsoleSender(), command);
    }
}