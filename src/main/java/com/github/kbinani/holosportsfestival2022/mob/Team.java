package com.github.kbinani.holosportsfestival2022.mob;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

class Team {
    private final List<Player> arrow = new LinkedList<>();
    private final List<Player> sword = new LinkedList<>();

    void add(Player player, Role role) {
        if (getCurrentRole(player) != null) {
            return;
        }
        switch (role) {
            case ARROW:
                if (arrow.size() > 0) {
                    return;
                }
                arrow.add(player);
                break;
            case SWORD:
                if (sword.size() > 2) {
                    return;
                }
                sword.add(player);
                break;
        }
    }

    void remove(Player player) {
        arrow.remove(player);
        sword.remove(player);
    }

    @Nullable
    Role getCurrentRole(Player player) {
        if (arrow.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()))) {
            return Role.ARROW;
        }
        if (sword.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()))) {
            return Role.SWORD;
        }
        return null;
    }
}

