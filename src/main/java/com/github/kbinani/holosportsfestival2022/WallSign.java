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

class WallSign {
    private WallSign() {
    }

    static void Place(Point3i p, BlockFace facing, String line1, String line2, String line3) {
        Server server = Bukkit.getServer();
        World world = server.getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst().orElse(null);
        if (world == null) {
            return;
        }
        int cx = p.x >> 4;
        int cz = p.z >> 4;
        boolean loaded = world.isChunkLoaded(cx, cz);
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
        if (!loaded) {
            world.unloadChunk(cx, cz);
        }
    }

    static void Place(Point3i p, BlockFace facing, String line1) {
        Place(p, facing, line1, "", "");
    }

    static void Place(Point3i p, BlockFace facing, String line1, String line2) {
        Place(p, facing, line1, line2, "");
    }
}