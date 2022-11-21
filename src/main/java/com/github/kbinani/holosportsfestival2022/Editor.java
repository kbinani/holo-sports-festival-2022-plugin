package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;

import javax.annotation.Nullable;

public class Editor {
    private Editor() {
    }

    public static void SetBlock(Point3i p, String block) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        int cx = p.x >> 4;
        int cz = p.z >> 4;
        world.loadChunk(cx, cz);
        Server server = Bukkit.getServer();
        server.dispatchCommand(server.getConsoleSender(), String.format("setblock %d %d %d %s", p.x, p.y, p.z, block));
    }

    public static void Stroke(String block, Point3i... points) {
        for (int i = 0; i < points.length - 1; i++) {
            Point3i from = points[i];
            Point3i to = points[i + 1];
            Fill(from, to, block);
        }
    }

    public static void Fill(Point3i from, Point3i to, String block) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        int cx0 = from.x >> 4;
        int cz0 = from.z >> 4;
        int cx1 = to.x >> 4;
        int cz1 = to.z >> 4;
        if (cx1 < cx0) {
            int t = cx0;
            cx0 = cx1;
            cx1 = t;
        }
        if (cz1 < cz0) {
            int t = cz0;
            cz0 = cz1;
            cz1 = t;
        }
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    world.loadChunk(cx, cz);
                }
            }
        }
        Server server = Bukkit.getServer();
        server.dispatchCommand(server.getConsoleSender(), String.format("fill %d %d %d %d %d %d %s", from.x, from.y, from.z, to.x, to.y, to.z, block));
    }

    public static void WallSign(Point3i p, BlockFace facing, String line1, String line2, String line3) {
        World world = Overworld();
        if (world == null) {
            return;
        }
        int cx = p.x >> 4;
        int cz = p.z >> 4;
        world.loadChunk(cx, cz);
        BlockData blockData = Material.BIRCH_WALL_SIGN.createBlockData("[facing=" + facing.name().toLowerCase() + "]");
        world.setBlockData(p.x, p.y, p.z, blockData);
        Block block = world.getBlockAt(p.x, p.y, p.z);
        BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        sign.setLine(0, "§l[看板を右クリック]§r");
        if (!line1.isEmpty()) {
            sign.setLine(1, line1);
        }
        if (!line2.isEmpty()) {
            sign.setLine(2, line2);
        }
        if (!line3.isEmpty()) {
            sign.setLine(3, line3);
        }
        sign.update();
    }

    public static void WallSign(Point3i p, BlockFace facing, String line1) {
        WallSign(p, facing, line1, "", "");
    }

    public static void WallSign(Point3i p, BlockFace facing, String line1, String line2) {
        WallSign(p, facing, line1, line2, "");
    }

    private static @Nullable World Overworld() {
        Server server = Bukkit.getServer();
        return server.getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst().orElse(null);
    }
}