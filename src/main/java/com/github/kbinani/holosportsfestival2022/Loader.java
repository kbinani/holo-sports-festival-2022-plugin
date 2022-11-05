package com.github.kbinani.holosportsfestival2022;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;

class Loader {
    private Loader() {
    }

    static void LoadChunk(World world, BoundingBox box) {
        int cx0 = ((int) Math.floor(box.getMinX())) >> 4;
        int cz0 = ((int) Math.floor(box.getMinZ())) >> 4;
        int cx1 = ((int) Math.ceil(box.getMaxX())) >> 4;
        int cz1 = ((int) Math.ceil(box.getMaxZ())) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                world.loadChunk(cx, cz);
            }
        }
    }
}