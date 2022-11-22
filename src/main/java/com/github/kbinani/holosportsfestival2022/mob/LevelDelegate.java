package com.github.kbinani.holosportsfestival2022.mob;

import org.bukkit.World;

import javax.annotation.Nullable;

public interface LevelDelegate {
    void levelExecute(String format, Object... args);
    @Nullable
    World levelGetWorld();
}