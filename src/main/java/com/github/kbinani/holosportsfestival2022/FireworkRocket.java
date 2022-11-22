package com.github.kbinani.holosportsfestival2022;

import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class FireworkRocket {
    private FireworkRocket() {
    }

    public static class Color {
        public static final int LIGHT_BLUE = 6719955;
        public static final int PINK = 14188952;
        public static final int YELLOW = 14602026;
        public static final int WHITE = 15790320;
    }

    public static void Launch(@Nonnull World world, double x, double y, double z, int[] colors, int[] fadeColors, int lifeTime, int type, boolean flicker, boolean trail) {
        FireworkEffect.Type t = FireworkEffect.Type.BALL;
        switch (type) {
            case 0:
                t = FireworkEffect.Type.BALL;
                break;
            case 1:
                t = FireworkEffect.Type.BALL_LARGE;
                break;
            case 4:
                t = FireworkEffect.Type.BURST;
                break;
        }
        final FireworkEffect.Type effectType = t;
        List<org.bukkit.Color> colorList = Arrays.stream(colors).mapToObj(org.bukkit.Color::fromRGB).toList();
        List<org.bukkit.Color> fadeColorList = Arrays.stream(fadeColors).mapToObj(org.bukkit.Color::fromRGB).toList();
        world.spawnEntity(new Location(world, x, y, z), EntityType.FIREWORK, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Firework firework = (Firework) it;
            FireworkMeta meta = firework.getFireworkMeta();
            FireworkEffect effect = FireworkEffect.builder()
                    .flicker(flicker)
                    .trail(trail)
                    .with(effectType)
                    .withColor(colorList)
                    .withFade(fadeColorList)
                    .build();
            meta.addEffect(effect);
            firework.setFireworkMeta(meta);
            firework.setTicksToDetonate(lifeTime);
        });
    }
}