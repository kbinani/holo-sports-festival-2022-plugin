package com.github.kbinani.holosportsfestival2022.boatrace;

import com.github.kbinani.holosportsfestival2022.TriConsumer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

class Team {
    private @Nullable Player shooter;
    private @Nullable Player driver;

    private final Map<Role, PlayerStatus> status = new HashMap<>();

    void setPlayer(Role role, @Nullable Player player) {
        if (role == Role.DRIVER) {
            this.driver = player;
        } else {
            this.shooter = player;
        }
    }

    @Nullable Player getPlayer(Role role) {
        if (role == Role.DRIVER) {
            return driver;
        } else {
            return shooter;
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
        updatePlayerStatus(role, status, (p, r) -> {
        });
    }

    void updatePlayerStatus(Role role, PlayerStatus status, BiConsumer<Player, Role> callback) {
        Player player = getPlayer(role);
        setPlayerStatus(role, status, () -> callback.accept(player, role));
        if (driver != null && shooter != null) {
            Entity driverVehicle = driver.getVehicle();
            Entity shooterVehicle = shooter.getVehicle();
            if (driverVehicle != null && shooterVehicle != null && driverVehicle.getUniqueId().equals(shooterVehicle.getUniqueId())) {
                // ボートの後席に乗っている場合 PlayerMoveEvent が検出できていないので同席していたら status は同じにする
                if (role == Role.DRIVER) {
                    setPlayerStatus(Role.SHOOTER, status, () -> callback.accept(shooter, Role.SHOOTER));
                } else {
                    setPlayerStatus(Role.DRIVER, status, () -> callback.accept(driver, Role.DRIVER));
                }
            }
        }
    }

    private void setPlayerStatus(Role role, PlayerStatus status, Runnable callback) {
        PlayerStatus prev = this.status.get(role);
        this.status.put(role, status);
        if (prev == null || prev.less(status)) {
            callback.run();
        }
    }

    int getRemainingRound() {
        PlayerStatus driverStatus = status.computeIfAbsent(Role.DRIVER, it -> PlayerStatus.IDLE);
        PlayerStatus shooterStatus = status.computeIfAbsent(Role.SHOOTER, it -> PlayerStatus.IDLE);
        int driverRound = 0;
        if (driver != null && driver.isOnline()) {
            driverRound = driverStatus.remainingRound();
        }
        int shooterRound = 0;
        if (shooter != null && shooter.isOnline()) {
            shooterRound = shooterStatus.remainingRound();
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