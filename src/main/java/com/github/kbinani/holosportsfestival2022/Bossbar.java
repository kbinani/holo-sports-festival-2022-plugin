package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

class Bossbar {
    private final JavaPlugin owner;
    private final String id;
    private int value = 0;
    private int max = 100;
    private String color = "white";
    private boolean visible = false;

    Bossbar(JavaPlugin owner, String id, String title) {
        this.owner = owner;
        this.id = id;
        clear(id);
        execute(String.format("bossbar add %s \"%s\"", id, title));
    }

    void dispose() {
        clear(id);
    }

    int getMax() {
        return max;
    }

    void setMax(int m) {
        if (m != max) {
            max = m;
            execute(String.format("bossbar set %s max %d", id, max));
        }
    }

    int getValue() {
        return this.value;
    }

    void setValue(int v) {
        if (v != value) {
            value = v;
            execute(String.format("bossbar set %s value %d", id, value));
        }
    }

    String getColor() {
        return color;
    }

    void setColor(String c) {
        if (!c.equals(color)) {
            color = c;
            execute(String.format("bossbar set %s color %s", id, color));
        }
    }

    void setVisible(boolean b) {
        if (b == visible) {
            return;
        }
        visible = b;
        if (visible) {
            execute(String.format("bossbar set %s players @a", id));
        }
        execute(String.format("bossbar set %s visible %s", id, visible ? "true" : "false"));
    }

    private void clear(String id) {
        execute(String.format("bossbar remove %s", id));
    }

    private void execute(String cmd) {
        Server server = owner.getServer();
        server.dispatchCommand(server.getConsoleSender(), cmd);
    }
}