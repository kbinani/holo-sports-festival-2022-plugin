package com.github.kbinani.holosportsfestival2022;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class Kill {
    private Kill() {

    }

    public static void EntitiesByType(World world, BoundingBox box, EntityType type) {
        world.getNearbyEntities(box, it -> it.getType() == type).forEach(Kill::Do);
    }

    public static void EntitiesByScoreboardTag(World world, BoundingBox box, String scoreboardTag) {
        world.getNearbyEntities(box, it -> it.getScoreboardTags().contains(scoreboardTag)).forEach(Kill::Do);
    }

    private static void Do(Entity entity) {
        if (entity instanceof Player player) {
            player.setHealth(0);
        } else {
            entity.remove();
        }
    }
}