package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;

public abstract class Stage {
    protected final Point3i origin;
    protected final StageDelegate delegate;

    // ステージ室内の北西下の角の座標を指定してステージを初期化する
    Stage(Point3i origin, @Nonnull StageDelegate delegate) {
        this.origin = new Point3i(origin);
        this.delegate = delegate;
    }

    // 入り口を開閉する
    abstract void setEntranceOpened(boolean opened);

    // 出口を開閉する
    abstract void setExitOpened(boolean opened);

    // ステージ室内の内法寸法を返す
    abstract Point3i getSize();

    BoundingBox getBounds() {
        Point3i size = getSize();
        return new BoundingBox(origin.x, origin.y, origin.z, origin.x + size.x, origin.y + size.y, origin.z + size.z);
    }

    protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        delegate.execute("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block);
    }
}