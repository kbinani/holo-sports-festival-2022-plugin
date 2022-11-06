package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;

import javax.annotation.Nonnull;

class FinalStage extends Stage {
    FinalStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
    }

    @Override
    protected void onExitOpened(boolean opened) {
        fill(x(-2), y(-59), z(-412), x(3), y(-57), z(-412), opened ? "air" : "iron_bars");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(32, 21, 55);
    }

    // 黄色チーム用 final ステージの原点: (-10, -60, -412)

    private int x(int x) {
        // 黄色ステージの即値を使って実装するのでオフセットする.
        return origin.x + (x - (-10));
    }

    private int y(int y) {
        // 黄色ステージの即値を使って実装するのでオフセットする.
        return origin.y + (y - (-60));
    }

    private int z(int z) {
        // 黄色ステージの即値を使って実装するのでオフセットする.
        return origin.z + (z - (-412));
    }
}
