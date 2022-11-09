package com.github.kbinani.holosportsfestival2022.mob;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

class Team {
    private final List<Player> arrow = new LinkedList<>();
    private final List<Player> sword = new LinkedList<>();
    private final List<Player> finished = new LinkedList<>();

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
        finished.remove(player);
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

    int getPlayerCount() {
        return arrow.size() + sword.size();
    }

    int setFinished(Player player) {
        if (finished.stream().noneMatch(it -> it.getUniqueId().equals(player.getUniqueId()))) {
            finished.add(player);
        }
        return finished.size();
    }

    void reset() {
        arrow.clear();
        sword.clear();
        finished.clear();
    }

    boolean isPlayerFinished(Player player) {
        return finished.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()));
    }

    boolean isCleared() {
        return finished.size() == getPlayerCount();
    }

    void usePlayers(Consumer<Player> callback) {
        for (Player p : arrow) {
            callback.accept(p);
        }
        for (Player p : sword) {
            callback.accept(p);
        }
    }
}
