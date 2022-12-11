package com.github.kbinani.holosportsfestival2022.relay;

import com.github.kbinani.holosportsfestival2022.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

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
    private final TeamColor color;
    private final org.bukkit.scoreboard.Team scoreboardTeam;

    Team(TeamColor color) {
        this.color = color;
        String name = "holosportsfestival_relay_team_";
        switch (color) {
            case RED -> {
                name += "red";
            }
            case WHITE -> {
                name += "white";
            }
            case YELLOW -> {
                name += "yellow";
            }
        }
        Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.color(NamedTextColor.WHITE);
        this.scoreboardTeam = team;
        updateScoreboardTeam();
    }

    int getPlayerCount() {
        return (int) participants.stream().filter(Player::isOnline).count();
    }

    void add(@Nonnull Player player) {
        participants.add(player);
        scoreboardTeam.addPlayer(player);
        updateScoreboardTeam();
    }

    void remove(@Nonnull Player player) {
        participants.removeIf(it -> it.getUniqueId().equals(player.getUniqueId()));
        scoreboardTeam.removePlayer(player);
        updateScoreboardTeam();
    }

    void clearParticipants() {
        for (Player player : participants) {
            scoreboardTeam.removePlayer(player);
        }
        participants.clear();
        passedCheckPoint.clear();
        updateScoreboardTeam();
    }

    boolean contains(@Nonnull Player player) {
        return participants.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()));
    }

    void pushRunner(@Nonnull Player player) {
        if (!contains(player)) {
            return;
        }
        order.add(player);
        updateScoreboardTeam();
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
        updateScoreboardTeam();
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

    private void updateScoreboardTeam() {
        NamedTextColor textColor = NamedTextColor.WHITE;
        String prefix = "";
        switch (color) {
            case RED -> {
                prefix += "赤組";
                textColor = NamedTextColor.RED;
            }
            case WHITE -> {
                prefix += "白組";
                textColor = NamedTextColor.GRAY;
            }
            case YELLOW -> {
                prefix += "黄組";
                textColor = NamedTextColor.YELLOW;
            }
        }
        if (order.isEmpty()) {
            scoreboardTeam.prefix(Component.text(String.format("%s(参加者%d) ", prefix, getPlayerCount())).color(textColor));
            scoreboardTeam.color(NamedTextColor.WHITE);
        } else {
            scoreboardTeam.prefix(Component.empty());
            scoreboardTeam.color(textColor);
        }
    }
}