package com.github.kbinani.holosportsfestival2022;

import org.bukkit.util.BoundingBox;

public class TargetSelector {
    private TargetSelector() {
    }

    public static String Of(BoundingBox bounds) {
        return String.format("x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f", bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getWidthX(), bounds.getHeight(), bounds.getWidthZ());
    }
}