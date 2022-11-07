package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class Stage {
    protected final Point3i origin;
    protected final StageDelegate delegate;
    private Boolean entranceOpened = null;
    private Boolean exitOpened = null;
    protected static final String kEntityTag = "hololive_sports_festival_2022_enemy";

    // ステージ室内の北西下の角の座標を指定してステージを初期化する
    Stage(Point3i origin, @Nonnull StageDelegate delegate) {
        this.origin = new Point3i(origin);
        this.delegate = delegate;
    }

    void setEntranceOpened(boolean open) {
        if (entranceOpened == null || entranceOpened != open) {
            entranceOpened = open;
            onEntranceOpened(open);
        }
    }

    void setExitOpened(boolean open) {
        if (exitOpened == null || exitOpened != open) {
            exitOpened = open;
            onExitOpened(open);
        }
    }

    // 入り口が開閉された時の処理
    protected abstract void onEntranceOpened(boolean opened);

    // 出口が開閉された時の処理
    protected abstract void onExitOpened(boolean opened);

    // ステージ室内の内法寸法を返す
    abstract Point3i getSize();

    // MOB の出る回数(ステップ数)を返す
    protected abstract int getStepCount();

    abstract void summonMobs(int step);

    abstract Optional<Next> consumeDeadMob(Entity entity);

    void reset() {
        setExitOpened(false);
        setEntranceOpened(false);
        onReset();
    }

    abstract void onReset();

    static class Next {
        final boolean step;
        final boolean stage;

        static Next Step() {
            return new Next(false, true);
        }

        static Next Stage() {
            return new Next(true, false);
        }

        private Next(boolean stage, boolean step) {
            this.stage = stage;
            this.step = step;
        }
    }

    BoundingBox getBounds() {
        Point3i size = getSize();
        return new BoundingBox(origin.x, origin.y, origin.z, origin.x + size.x, origin.y + size.y, origin.z + size.z);
    }

    protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        execute("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block);
    }

    protected void execute(String format, Object... args) {
        delegate.execute(format, args);
    }
}