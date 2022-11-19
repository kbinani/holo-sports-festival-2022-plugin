package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.Optional;

class FinalStage extends Stage {
    private boolean isCreeperSpawned = false;

    FinalStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
        // 入り口は無い
    }

    @Override
    protected void onExitOpened(boolean opened) {
        // ここでは何もしない. 全チームゴールした時に開けたいので Level で管理する
    }

    @Override
    public Point3i getSize() {
        return new Point3i(22, 21, 55);
    }

    @Override
    protected int getStepCount() {
        return 1;
    }

    @Override
    void summonMobs(int step) {
        // クリーパーは湧き場に最初のプレイヤーが侵入してから行う
    }

    void summonCreepers() {
        summonCreeper(4, -59, -378);

        summonCreeper(9, -59, -378);
        summonCreeper(9, -59, -379);
        summonCreeper(9, -59, -380);
        summonCreeper(9, -59, -381);

        summonCreeper(-3, -59, -378);

        summonCreeper(-8, -59, -378);
        summonCreeper(-8, -59, -379);
        summonCreeper(-8, -59, -380);
        summonCreeper(-8, -59, -381);
        isCreeperSpawned = true;
    }

    BoundingBox getCreeperSpawnBounds() {
        return new BoundingBox(x(-10), y(-59), z(-381), x(12), y(-55), z(-377));
    }

    BoundingBox getGoalDetectionBounds() {
        return new BoundingBox(x(-3), y(-58), z(-412), x(5), y(-56), z(-404));
    }

    BoundingBox getCorridorBounds() {
        return new BoundingBox(x(-3), y(-60), z(-404), x(5), y(-55), z(-381));
    }

    private void summonCreeper(int x, int y, int z) {
        execute("summon creeper %d %d %d {Tags:[\"%s\"],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", x(x), y(y), z(z), kEntityTag);
    }

    @Override
    Optional<Result> consumeDeadMob(Entity entity) {
        return Optional.of(Result.Empty());
    }

    @Override
    void onReset() {
        isCreeperSpawned = false;
    }

    @Override
    BossbarValue getBossbarValue() {
        // final stage の bossbar は event listener 側で制御する. ここで返しているのは初期値
        return new BossbarValue(0, 3, "GO TO GOAL !!");
    }

    @Override
    String getMessageDisplayString() {
        return "";
    }

    @Override
    Point3i getRespawnLocation() {
        // shooting ステージと同じ
        return new Point3i(x(1), y(-59), z(-357));
    }

    boolean isCreeperSpawned() {
        return this.isCreeperSpawned;
    }

    boolean containsInBounds(Vector location) {
        return getCreeperSpawnBounds().contains(location) || getCorridorBounds().contains(location);
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
