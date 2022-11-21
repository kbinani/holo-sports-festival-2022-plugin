package com.github.kbinani.holosportsfestival2022.daruma;

import org.bukkit.entity.Player;

class Goal {
    final Player player;
    final double tick;

    Goal(Player player, double tick) {
        this.player = player;
        this.tick = tick;
    }
}
