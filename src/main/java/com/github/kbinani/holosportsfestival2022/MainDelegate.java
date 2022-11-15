package com.github.kbinani.holosportsfestival2022;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public interface MainDelegate {
    @Nullable CompetitionType getCurrentCompetition(Player player);
}