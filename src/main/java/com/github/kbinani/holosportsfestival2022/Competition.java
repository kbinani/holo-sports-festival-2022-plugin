package com.github.kbinani.holosportsfestival2022;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;

public interface Competition extends Listener {
    boolean isJoined(Player player);
    void clearCompetitionItems(Player player);
    @Nonnull
    CompetitionType competitionGetType();
}