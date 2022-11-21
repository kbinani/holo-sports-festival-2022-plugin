package com.github.kbinani.holosportsfestival2022.boatrace;

import com.github.kbinani.holosportsfestival2022.TriConsumer;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

class Team {
    private @Nullable Player shooter;
    private @Nullable Player driver;

    private static int RemainingRound(PlayerStatus s) {
        switch (s) {
            case IDLE:
            case STARTED:
            case CLEARED_CHECKPOINT1:
                return 2;
            case CLEARED_START_LINE1:
            case CLEARED_CHECKPOINT2:
                return 1;
            case FINISHED:
                return 0;
        }
        return 2;
    }

    private final Map<Role, PlayerStatus> status = new HashMap<>();

    void setPlayer(Role role, @Nullable Player player) {
        if (role == Role.DRIVER) {
            this.driver = player;
        } else {
            this.shooter = player;
        }
    }

    @Nullable
    Role getCurrentRole(@Nonnull Player player) {
        if (driver != null && driver.getUniqueId().equals(player.getUniqueId())) {
            return Role.DRIVER;
        }
        if (shooter != null && shooter.getUniqueId().equals(player.getUniqueId())) {
            return Role.SHOOTER;
        }
        return null;
    }

    int getPlayerCount() {
        // 一人でもメンバーが居れば準備済み扱いにする
        int result = 0;
        if (driver != null && driver.isOnline()) {
            result++;
        }
        if (shooter != null && shooter.isOnline()) {
            result++;
        }
        return result;
    }

    @Nullable
    PlayerStatus getPlayerStatus(Player player) {
        Role role = getCurrentRole(player);
        if (role == null) {
            return null;
        }
        return status.getOrDefault(role, PlayerStatus.IDLE);
    }

    void updatePlayerStatus(Role role, PlayerStatus status) {
        this.status.put(role, status);
        if (status == PlayerStatus.FINISHED) {
            setPlayer(role, null);
        }
    }

    int getRemainingRound() {
        PlayerStatus driverStatus = status.computeIfAbsent(Role.DRIVER, it -> PlayerStatus.IDLE);
        PlayerStatus shooterStatus = status.computeIfAbsent(Role.SHOOTER, it -> PlayerStatus.IDLE);
        int driverRound = 0;
        if (driver != null && driver.isOnline()) {
            driverRound = RemainingRound(driverStatus);
        }
        int shooterRound = 0;
        if (shooter != null && shooter.isOnline()) {
            shooterRound = RemainingRound(shooterStatus);
        }
        return Math.max(driverRound, shooterRound);
    }

    void eachPlayer(TriConsumer<Player, Role, PlayerStatus> consumer) {
        if (driver != null) {
            consumer.accept(driver, Role.DRIVER, status.getOrDefault(Role.DRIVER, PlayerStatus.IDLE));
        }
        if (shooter != null) {
            consumer.accept(shooter, Role.SHOOTER, status.getOrDefault(Role.SHOOTER, PlayerStatus.IDLE));
        }
    }
}