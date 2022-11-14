package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Editor;
import com.github.kbinani.holosportsfestival2022.Point3i;
import com.github.kbinani.holosportsfestival2022.TargetSelector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.List;
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

    abstract Optional<Result> consumeDeadMob(Entity entity);

    // bossbar の表示パラメータ
    abstract BossbarValue getBossbarValue();

    // チャット欄に表示する stage 名
    abstract String getMessageDisplayString();

    // 死んだ時のリスポーン位置
    abstract Point3i getRespawnLocation();

    void onStart(List<Player> players) {
    }

    void reset() {
        setExitOpened(false);
        setEntranceOpened(false);
        execute("kill @e[tag=%s,%s]", kEntityTag, TargetSelector.Of(getBounds()));
        onReset();
    }

    abstract void onReset();

    static class Result {
        final boolean step;
        final boolean stage;

        static Result Step() {
            return new Result(false, true);
        }

        static Result Stage() {
            return new Result(true, false);
        }

        static Result Empty() {
            return new Result(false, false);
        }

        private Result(boolean stage, boolean step) {
            this.stage = stage;
            this.step = step;
        }
    }

    BoundingBox getBounds() {
        Point3i size = getSize();
        return new BoundingBox(origin.x, origin.y, origin.z, origin.x + size.x, origin.y + size.y, origin.z + size.z);
    }

    protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        Editor.Fill(new Point3i(x1, y1, z1), new Point3i(x2, y2, z2), block);
    }

    protected void execute(String format, Object... args) {
        delegate.execute(format, args);
    }
}