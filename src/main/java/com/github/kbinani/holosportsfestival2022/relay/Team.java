package com.github.kbinani.holosportsfestival2022.relay;

import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class Team {
    private final Set<Player> participants = new HashSet<>();
    private final List<Player> order = new LinkedList<>();
    private final List<Player> passedCheckPoint = new LinkedList<>();

    int getPlayerCount() {
        return (int) participants.stream().filter(Player::isOnline).count();
    }

    void add(@Nonnull Player player) {
        participants.add(player);
    }

    void remove(@Nonnull Player player) {
        participants.removeIf(it -> it.getUniqueId().equals(player.getUniqueId()));
    }

    void clearParticipants() {
        participants.clear();
        passedCheckPoint.clear();
    }

    boolean contains(@Nonnull Player player) {
        return participants.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()));
    }

    void pushRunner(@Nonnull Player player) {
        if (!contains(player)) {
            return;
        }
        order.add(player);
    }

    @Nullable
    Player getCurrentRunner() {
        if (order.isEmpty()) {
            return null;
        }
        return order.stream().skip(order.size() - 1).findFirst().orElse(null);
    }

    void pushPassedCheckPoint(@Nonnull Player player) {
        if (!contains(player)) {
            return;
        }
        passedCheckPoint.add(player);
    }

    boolean isRunnerPassedCheckPoint(@Nonnull Player player) {
        return passedCheckPoint.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()));
    }

    void clearOrder() {
        order.clear();
    }

    int getOrderLength() {
        return order.size();
    }

    void clearPassedCheckPoint() {
        passedCheckPoint.clear();
    }

    void eachPlayer(Consumer<Player> callback) {
        participants.forEach(callback);
    }
}
