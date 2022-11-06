package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;

import javax.annotation.Nonnull;

class PlainsStage extends Stage {
    PlainsStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    public void setEntranceOpened(boolean opened) {
        fill(x(0), y(-59), z(-255), x(1), y(-58), z(-255), opened ? "air" : "bedrock");
    }

    @Override
    public void setExitOpened(boolean opened) {
        fill(x(1), y(-59), z(-276), x(0), y(-58), z(-276), opened ? "air" : "bedrock");
        fill(x(1), y(-48), z(-276), x(0), y(-47), z(-276), opened ? "air" : "bedrock");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(20, 20, 20);
    }

    // 黄色チーム用 plains ステージの原点: (-9, -59, -275)

    private int x(int x) {
        // 黄色ステージの即値を使って実装するのでオフセットする.
        return origin.x + (x - (-9));
    }

    private int y(int y) {
        // 黄色ステージの即値を使って実装するのでオフセットする.
        return origin.y + (y - (-59));
    }

    private int z(int z) {
        // 黄色ステージの即値を使って実装するのでオフセットする.
        return origin.z + (z - (-275));
    }
}
