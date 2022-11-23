package com.github.kbinani.holosportsfestival2022;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

public interface MainDelegate {
    @Nullable
    CompetitionType mainGetCurrentCompetition(Player player);

    void mainRunTask(Runnable task);

    void mainRunTaskLater(Runnable task, long delay);

    BukkitTask mainRunTaskTimer(Runnable task, long delay, long period);

    @Nonnull
    World mainGetWorld();

    Logger mainGetLogger();

    void mainCountdownThen(BoundingBox[] box, Predicate<Integer> countdown, Supplier<Boolean> task, long delay);

    void mainClearCompetitionItems(Player player);

    void mainUsingChunk(BoundingBox box, Consumer<World> callback);
}