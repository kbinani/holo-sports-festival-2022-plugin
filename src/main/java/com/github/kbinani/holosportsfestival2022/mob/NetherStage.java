package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import com.github.kbinani.holosportsfestival2022.TargetSelector;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.Optional;

class NetherStage extends Stage {
    private int remainingMobCount = 11;

    NetherStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
        fill(x(1), y(-59), z(-305), x(0), y(-58), z(-305), opened ? "air" : "bedrock");
    }

    @Override
    protected void onExitOpened(boolean opened) {
        fill(x(1), y(-59), z(-326), x(0), y(-58), z(-326), opened ? "air" : "bedrock");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(20, 20, 20);
    }

    @Override
    protected int getStepCount() {
        return 2;
    }

    @Override
    void summonMobs(int step) {
        switch (step) {
            case 0:
                summonZombifiedPiglin(-7, -58, -314);
                summonZombifiedPiglin(-5, -59, -321);
                summonZombifiedPiglin(-1, -59, -324);
                summonZombifiedPiglin(4, -59, -322);
                summonZombifiedPiglin(8, -59, -315);

                summonBlaze(-8, -49, -318);
                summonBlaze(7, -47, -323);
                summonBlaze(7, -53, -315);
                break;
            case 1:
                summonWitherSkeleton(8, -49, -314);
                summonWitherSkeleton(9, -55, -318);

                execute("summon ghast %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\", \"%s\"],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", x(-5), y(-47), z(-312), kEntityTag, stageEntityTag);

                BoundingBox box = getBounds();
                execute("effect give @e[tag=%s,%s] glowing 86400 1 true", kEntityTag, TargetSelector.Of(box));
                break;
        }
    }

    private void summonWitherSkeleton(int x, int y, int z) {
        // 通常x9, クリティカルx1: https://youtu.be/xIjr6Ct_Wlo?t=3554
        // 通常x5, クリティカルx4: https://youtu.be/26cNq-_8NIY?t=1264
        execute("summon wither_skeleton %d %d %d {HandItems:[{id:stone_sword,Count:1}],HandDropChances:[0.0f,0.0f],ArmorItems:[{},{},{},{}],Tags:[\"%s\", \"%s\"],Health:200.0f,Attributes:[{Name:\"generic.max_health\",Base:200.0d},{Name:\"generic.movement_speed\",Base:0.345d}],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", this.x(x), this.y(y), this.z(z), kEntityTag, stageEntityTag);
    }

    private void summonBlaze(int x, int y, int z) {
        execute("summon blaze %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\", \"%s\"],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", this.x(x), this.y(y), this.z(z), kEntityTag, stageEntityTag);
    }

    private void summonZombifiedPiglin(int x, int y, int z) {
        execute("summon zombified_piglin %d %d %d {HandItems:[{id:golden_sword,Count:1}],HandDropChances:[0.0f,0.0f],ArmorItems:[{},{},{},{}],Tags:[\"%s\", \"%s\"],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", this.x(x), this.y(y), this.z(z), kEntityTag, stageEntityTag);
    }

    @Override
    Optional<Result> consumeDeadMob(Entity entity) {
        if (!entity.getScoreboardTags().contains(stageEntityTag)) {
            return Optional.empty();
        }
        int before = remainingMobCount;
        int after = Math.max(0, remainingMobCount - 1);
        if (before == after) {
            return Optional.of(Result.Empty());
        }
        remainingMobCount = after;
        if (after == 0) {
            return Optional.of(Result.Stage());
        } else if (after == 3) {
            return Optional.of(Result.Step());
        } else {
            return Optional.of(Result.Empty());
        }
    }

    @Override
    void onReset() {
        remainingMobCount = 11;
    }

    @Override
    BossbarValue getBossbarValue() {
        if (remainingMobCount <= 3) {
            return new BossbarValue(remainingMobCount, 3, "WAVE 3 BOSS");
        } else {
            return new BossbarValue(remainingMobCount - 3, 8, "WAVE 3");
        }
    }

    @Override
    String getMessageDisplayString() {
        return "WAVE3";
    }

    @Override
    Point3i getRespawnLocation() {
        return new Point3i(x(1), y(-59), z(-306));
    }

    // 黄色チーム用 nether ステージの原点: (-9, -59, -325)

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
        return origin.z + (z - (-325));
    }
}