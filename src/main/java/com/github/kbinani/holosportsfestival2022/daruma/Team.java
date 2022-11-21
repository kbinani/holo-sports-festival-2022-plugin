package com.github.kbinani.holosportsfestival2022.daruma;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

class Team {
    private final Map<UUID, Player> players = new HashMap<>();

    boolean contains(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    void add(Player player) {
        players.put(player.getUniqueId(), player);
    }

    void remove(Player player) {
        players.remove(player.getUniqueId());
    }

    int getPlayerCount() {
        return players.size();
    }

    void eachPlayer(Consumer<Player> callback) {
        players.values().forEach(callback);
    }
}
