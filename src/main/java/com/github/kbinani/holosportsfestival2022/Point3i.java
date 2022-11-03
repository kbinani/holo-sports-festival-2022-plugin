package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Location;

class Point3i {
    int x;
    int y;
    int z;

    Point3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Point3i(Location l) {
        this.x = l.getBlockX();
        this.y = l.getBlockY();
        this.z = l.getBlockZ();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Point3i)) {
            return false;
        }
        Point3i v = (Point3i) o;
        return v.x == x && v.y == y && v.z == z;
    }
}