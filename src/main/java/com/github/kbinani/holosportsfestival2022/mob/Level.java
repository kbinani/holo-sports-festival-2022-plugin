package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Editor;
import com.github.kbinani.holosportsfestival2022.FireworkRocket;
import com.github.kbinani.holosportsfestival2022.Players;
import com.github.kbinani.holosportsfestival2022.Point3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
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
    private final BoundingBox boundsExceptFinalStage;

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

        BoundingBox boundsExceptFinalStage = null;
        for (Stage stage : stages) {
            BoundingBox box = stage.getBounds();
            if (boundsExceptFinalStage == null) {
                boundsExceptFinalStage = box;
            } else {
                boundsExceptFinalStage.union(box);
            }
        }
        if (boundsExceptFinalStage == null) {
            boundsExceptFinalStage = new BoundingBox();
        }
        this.boundsExceptFinalStage = boundsExceptFinalStage;

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

    boolean containsInBounds(Vector location) {
        if (boundsExceptFinalStage.contains(location)) {
            return true;
        }
        return finalStage.containsInBounds(location);
    }

    void reset() {
        for (Stage stage : stages) {
            stage.reset();
        }
        progress = Progress.Zero();
    }

    boolean consumeDeadMob(Entity entity) {
        Stage stage = stages.get(progress.stage);
        Optional<Stage.Result> maybeNext = stage.consumeDeadMob(entity);
        if (maybeNext.isEmpty()) {
            return false;
        }
        Stage.Result next = maybeNext.get();
        if (next.stage) {
            // 次の stage へ
            this.progress = new Progress(progress.stage + 1, 0);
            return true;
        } else if (next.step) {
            // 同一 stage の次の step へ
            this.progress = new Progress(progress.stage, progress.step + 1);
            return true;
        } else {
            return true;
        }
    }

    Progress getProgress() {
        return new Progress(progress.stage, progress.step);
    }

    void showTitle(String text, Color color) {
        Component title = Component.text(text, Style.style(TextColor.color(color.asRGB()), TextDecoration.BOLD));
        Component subtitle = Component.empty();
        Title t = Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000)));
        Players.Within(bounds, player -> {
            player.showTitle(t);
        });
    }

    @Nullable
    @Override
    public World stageGetWorld() {
        return delegate.levelGetWorld();
    }

    void setExitOpened(boolean opened) {
        Editor.Fill(new Point3i(x(-2), y(-59), z(-412)), new Point3i(x(3), y(-57), z(-412)), opened ? "air" : "iron_bars");
    }

    Point3i getSafeSpawnLocation() {
        return new Point3i(x(1), y(-59), z(-251));
    }

    void launchFireworkRockets(int color) {
        World world = delegate.levelGetWorld();
        if (world == null) {
            return;
        }
        FireworkRocket.Launch(world, x(-3) + 0.5, y(-49) + 0.5, z(-407) + 0.5, new int[]{color}, new int[]{color}, 20, 1, false, false);
        FireworkRocket.Launch(world, x(-2) + 0.5, y(-47) + 0.5, z(-407) + 0.5, new int[]{color}, new int[]{color}, 20, 1, false, false);
        FireworkRocket.Launch(world, x(-1) + 0.5, y(-46) + 0.5, z(-407) + 0.5, new int[]{color}, new int[]{color}, 20, 1, false, false);
        FireworkRocket.Launch(world, x(2) + 0.5, y(-46) + 0.5, z(-407) + 0.5, new int[]{color}, new int[]{color}, 20, 1, false, false);
        FireworkRocket.Launch(world, x(3) + 0.5, y(-47) + 0.5, z(-407) + 0.5, new int[]{color}, new int[]{color}, 20, 1, false, false);
        FireworkRocket.Launch(world, x(4) + 0.5, y(-49) + 0.5, z(-407) + 0.5, new int[]{color}, new int[]{color}, 20, 1, false, false);
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