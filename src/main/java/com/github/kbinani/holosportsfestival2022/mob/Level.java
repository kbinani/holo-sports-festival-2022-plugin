package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

class Level implements StageDelegate {
    private final Point3i origin;
    private final List<Stage> stages;
    final FinalStage finalStage;
    private final BoundingBox bounds;
    private Progress progress = Progress.Zero();
    private final LevelDelegate delegate;

    // エントリー解除看板が貼ってあるブロックの座標を原点としてステージ群を初期化する
    Level(Point3i origin, @Nonnull LevelDelegate delegate) {
        this.origin = origin;
        this.delegate = delegate;
        this.stages = new LinkedList<>();
        // origin = (-9, -59, -254) の時:
        // (-9, -59, -275)
        this.stages.add(new PlainsStage(new Point3i(origin.x, origin.y, origin.z - 21), this));
        // (-9, -59, -300)
        this.stages.add(new OceanMonumentStage(new Point3i(origin.x, origin.y, origin.z - 46), this));
        // (-9, -59, -325)
        this.stages.add(new NetherStage(new Point3i(origin.x, origin.y, origin.z - 71), this));
        // (-9, -59, -351)
        this.stages.add(new WoodlandMansionStage(new Point3i(origin.x, origin.y, origin.z - 97), this));
        // (-9, -59, -376)
        this.stages.add(new ShootingStage(new Point3i(origin.x, origin.y, origin.z - 122), this));
        // (-10, -60, -412)
        finalStage = new FinalStage(new Point3i(origin.x - 1, origin.y - 1, origin.z - 158), this);
        this.stages.add(finalStage);

        BoundingBox bounds = null;
        for (Stage stage : stages) {
            BoundingBox box = stage.getBounds();
            if (bounds == null) {
                bounds = box;
            } else {
                bounds.union(box);
            }
            stage.setEntranceOpened(false);
            stage.setExitOpened(false);
        }
        this.bounds = bounds == null ? new BoundingBox() : bounds;
    }

    int getStageCount() {
        return stages.size();
    }

    @Nullable
    Stage getStage(int index) {
        if (index < stages.size()) {
            return stages.get(index);
        } else {
            return null;
        }
    }

    BoundingBox getBounds() {
        return bounds.clone();
    }

    void reset() {
        for (Stage stage : stages) {
            stage.reset();
        }
        progress = Progress.Zero();
    }

    @Nonnull
    Progress consumeDeadMob(Entity entity) {
        Stage stage = stages.get(progress.stage);
        Optional<Stage.Next> maybeNext = stage.consumeDeadMob(entity);
        if (maybeNext.isEmpty()) {
            return this.progress;
        }
        Stage.Next next = maybeNext.get();
        if (next.stage) {
            // 次の stage へ
            this.progress = new Progress(progress.stage + 1, 0);
            return this.progress;
        } else if (next.step) {
            // 同一 stage の次の step へ
            this.progress = new Progress(progress.stage, progress.step + 1);
            return progress;
        } else {
            return progress;
        }
    }

    Progress getProgress() {
        return new Progress(progress.stage, progress.step);
    }

    String getTargetSelector() {
        return String.format("@a[x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f]", bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getWidthX(), bounds.getHeight(), bounds.getWidthZ());
    }

    void showTitle(String text, String color) {
        execute("title %s title {\"text\": \"%s\", \"bold\": true, \"color\": \"%s\"}", getTargetSelector(), text, color);
    }

    @Override
    public void execute(String format, Object ...args) {
        delegate.execute(format, args);
    }

    void setExitOpened(boolean opened) {
        execute("fill %s %s %s %s %s %s %s", x(-2), y(-59), z(-412), x(3), y(-57), z(-412), opened ? "air" : "iron_bars");
    }

    Point3i getSafeSpawnLocation() {
        return new Point3i(x(1), y(-59), z(-251));
    }

    // 黄色チーム用 Level 原点: (-9, -59, -254)

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
        return origin.z + (z - (-254));
    }
}