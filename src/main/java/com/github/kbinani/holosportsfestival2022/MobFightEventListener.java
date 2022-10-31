package com.github.kbinani.holosportsfestival2022;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MobFightEventListener implements Listener {
    private final JavaPlugin owner;

    MobFightEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

    }
}