package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;

import javax.annotation.Nonnull;

class WoodlandMansionStage extends Stage {
    WoodlandMansionStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    public void setEntranceOpened(boolean opened) {
        fill(x(1), y(-59), z(-330), x(0), y(-58), z(-330), opened ? "air" : "bedrock");
    }

    @Override
    public void setExitOpened(boolean opened) {
        fill(x(1), y(-59), z(-352), x(0), y(-58), z(-352), opened ? "air" : "bedrock");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(20, 20, 21);
    }

    // 黄色チーム用 woodland mansion ステージの原点: (-9, -59, -351)

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
        return origin.z + (z - (-351));
    }
}
