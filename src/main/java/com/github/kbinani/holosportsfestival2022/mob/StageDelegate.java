package com.github.kbinani.holosportsfestival2022.mob;

import org.bukkit.World;

import javax.annotation.Nonnull;

public interface StageDelegate {
    @Nonnull
    World stageGetWorld();
}