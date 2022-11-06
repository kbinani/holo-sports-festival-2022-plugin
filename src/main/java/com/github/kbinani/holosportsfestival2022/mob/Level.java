package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

class Level {
    private final List<Stage> stages;
    private final BoundingBox bounds;

    // エントリー解除看板が貼ってあるブロックの座標を原点としてステージ群を初期化する
    Level(Point3i origin, @Nonnull StageDelegate delegate) {
        this.stages = new LinkedList<>();
        // origin = (-9, -59, -254) の時:
        // (-9, -59, -275)
        this.stages.add(new PlainsStage(new Point3i(origin.x, origin.y, origin.z - 21), delegate));
        // (-9, -59, -300)
        this.stages.add(new OceanMonumentStage(new Point3i(origin.x, origin.y, origin.z - 46), delegate));
        // (-9, -59, -325)
        this.stages.add(new NetherStage(new Point3i(origin.x, origin.y, origin.z - 71), delegate));
        // (-9, -59, -351)
        this.stages.add(new WoodlandMansionStage(new Point3i(origin.x, origin.y, origin.z - 97), delegate));
        // (-9, -59, -376)
        this.stages.add(new ShootingStage(new Point3i(origin.x, origin.y, origin.z - 122), delegate));
        // (-10, -60, -412)
        this.stages.add(new FinalStage(new Point3i(origin.x - 1, origin.y - 1, origin.z - 158), delegate));

        this.bounds = new BoundingBox();
        for (Stage stage : stages) {
            BoundingBox box = stage.getBounds();
            this.bounds.union(box);
            stage.setEntranceOpened(false);
            stage.setExitOpened(false);
        }
    }

    int getStageCount() {
        return stages.size();
    }

    Stage getStage(int index) {
        return stages.get(index);
    }

    void reset() {
        for (Stage stage : stages) {
            stage.setExitOpened(false);
            stage.setEntranceOpened(false);
        }
    }
}