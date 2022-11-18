package com.github.kbinani.holosportsfestival2022;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface MainDelegate {
    @Nullable
    CompetitionType getCurrentCompetition(Player player);

    void execute(String format, Object... args);

    void runTask(Runnable task);

    void runTaskLater(Runnable task, long delay);

    BukkitTask runTaskTimer(Runnable task, long delay, long period);

    @Nullable
    World getWorld();

    void info(String format, Object... args);

    void countdownThen(BoundingBox box, Predicate<Integer> countdown, Supplier<Boolean> task, long delay);
}