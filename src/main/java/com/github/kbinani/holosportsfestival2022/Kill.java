package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class Kill {
    private Kill() {

    }

    public static void EntitiesByType(BoundingBox box, EntityType type) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        world.getNearbyEntities(box, it -> it.getType() == type).forEach(Kill::Do);
    }

    public static void EntitiesByScoreboardTag(BoundingBox box, String scoreboardTag) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        world.getNearbyEntities(box, it -> it.getScoreboardTags().contains(scoreboardTag)).forEach(Kill::Do);
    }

    private static World Overworld() {
        return Bukkit.getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst().orElse(null);
    }

    private static void Do(Entity entity) {
        if (entity instanceof Player player) {
            player.setHealth(0);
        } else {
            entity.remove();
        }
    }
}