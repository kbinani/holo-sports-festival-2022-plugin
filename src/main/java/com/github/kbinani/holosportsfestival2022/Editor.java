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

import javax.annotation.Nonnull;

public class Editor {
    private Editor() {
    }

    public static void SetBlock(@Nonnull World world, Point3i p, String block) {
        int cx = p.x >> 4;
        int cz = p.z >> 4;
        world.loadChunk(cx, cz);
        Server server = Bukkit.getServer();
        BlockData blockData = null;
        try {
            blockData = server.createBlockData(block);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return;
        }
        world.setBlockData(p.x, p.y, p.z, blockData);
    }

    public static void Fill(@Nonnull World world, Point3i from, Point3i to, String blockDataString) {
        Load(world, from, to);
        Server server = Bukkit.getServer();
        BlockData blockData = null;
        try {
            blockData = server.createBlockData(blockDataString);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return;
        }
        int x0 = Math.min(from.x, to.x);
        int y0 = Math.min(from.y, to.y);
        int z0 = Math.min(from.z, to.z);
        int x1 = Math.max(from.x, to.x);
        int y1 = Math.max(from.y, to.y);
        int z1 = Math.max(from.z, to.z);
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    world.setBlockData(x, y, z, blockData);
                }
            }
        }
    }

    private static void Load(@Nonnull World world, Point3i from, Point3i to) {
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
    }

    public static void WallSign(@Nonnull World world, Point3i p, BlockFace facing, String line1, String line2, String line3) {
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

    public static void WallSign(@Nonnull World world, Point3i p, BlockFace facing, String line1) {
        WallSign(world, p, facing, line1, "", "");
    }

    public static void WallSign(@Nonnull World world, Point3i p, BlockFace facing, String line1, String line2) {
        WallSign(world, p, facing, line1, line2, "");
    }
}