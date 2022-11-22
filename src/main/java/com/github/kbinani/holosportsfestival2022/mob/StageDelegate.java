package com.github.kbinani.holosportsfestival2022.mob;

import org.bukkit.World;

import javax.annotation.Nullable;

public interface StageDelegate {
    void stageExecute(String format, Object ...args);
    @Nullable
    World stageGetWorld();
}
