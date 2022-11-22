package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.Arrays;
import java.util.stream.Collectors;

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
        Server server = Bukkit.getServer();
        String colorsString = Arrays.stream(colors).mapToObj((i) -> String.format("%d", i)).collect(Collectors.joining(","));
        String fadeColorsString = Arrays.stream(fadeColors).mapToObj(i -> String.format("%d", i)).collect(Collectors.joining(","));
        // https://symtm.blog.fc2.com/blog-entry-96.html
        String command = String.format("summon firework_rocket %f %f %f {LifeTime:%d,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:%d,Flicker:%db,Trail:%db,Colors:[I;%s],FadeColors:[I;%s]}],Flight:1}}}}", x, y, z, lifeTime, type, flicker ? 1 : 0, trail ? 1 : 0, colorsString, fadeColorsString);
        server.dispatchCommand(server.getConsoleSender(), command);
    }
}