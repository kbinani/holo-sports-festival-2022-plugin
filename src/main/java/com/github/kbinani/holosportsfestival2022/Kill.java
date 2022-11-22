package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

public class Kill {
    private Kill() {

    }

    public static void EntitiesByType(BoundingBox box, EntityType type) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        world.getNearbyEntities(box).forEach(entity -> {
            if (entity.getType() == type) {
                entity.remove();
            }
        });
    }

    public static void EntitiesByScoreboardTag(BoundingBox box, String scoreboardTag) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        world.getNearbyEntities(box).forEach(entity -> {
            if (entity.getScoreboardTags().contains(scoreboardTag)) {
                entity.remove();
            }
        });
    }

    private static World Overworld() {
        return Bukkit.getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst().orElse(null);
    }
}