package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Kill;
import com.github.kbinani.holosportsfestival2022.Point3i;
import com.github.kbinani.holosportsfestival2022.TargetSelector;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.Optional;

class OceanMonumentStage extends Stage {
    int remainingMobCount = 10;

    OceanMonumentStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
        fill(x(1), y(-59), z(-280), x(0), y(-58), z(-280), opened ? "air" : "bedrock");
    }

    @Override
    protected void onExitOpened(boolean opened) {
        fill(x(0), y(-59), z(-301), x(0), y(-58), z(-301), opened ? "warped_wall_sign[facing=east]" : "bedrock");
        fill(x(1), y(-59), z(-301), x(1), y(-58), z(-301), opened ? "warped_wall_sign[facing=west]" : "bedrock");
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
        BoundingBox box = getBounds();
        switch (step) {
            case 0:
                summonDrowned(-7, -42, -283);
                summonDrowned(3, -48, -297);
                summonDrowned(-3, -52, -282);
                summonDrowned(-8, -59, -285);
                summonDrowned(-3, -59, -293);

                summonGuardian(9, -48, -298);
                summonGuardian(9, -55, -289);
                summonGuardian(9, -57, -294);
                break;
            case 1:
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],HandItems:[{id:trident,Count:1}],HandDropChances:[0.0f,0.0f],Tags:[\"%s\",\"%s\"],Health:200.0f,Attributes:[{Name:\"generic.max_health\",Base:200.0d}],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", x(-3), y(-59), z(-299), kEntityTag, stageEntityTag);
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],HandItems:[{id:trident,Count:1}],HandDropChances:[0.0f,0.0f],Tags:[\"%s\",\"%s\"],Health:200.0f,Attributes:[{Name:\"generic.max_health\",Base:200.0d}],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", x(5), y(-43), z(-297), kEntityTag, stageEntityTag);
                execute("effect give @e[tag=%s,%s] glowing 86400 1 true", kEntityTag, TargetSelector.Of(box));
                break;
        }
    }

    private void summonDrowned(int x, int y, int z) {
        execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\",\"%s\"],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", x(x), y(y), z(z), kEntityTag, stageEntityTag);
    }

    private void summonGuardian(int x, int y, int z) {
        execute("summon guardian %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\",\"%s\"],DeathLootTable:\"minecraft:empty\",PersistenceRequired:1b}", x(x), y(y), z(z), kEntityTag, stageEntityTag);
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
        } else if (after == 2) {
            return Optional.of(Result.Step());
        }
        return Optional.of(Result.Empty());
    }

    @Override
    void onReset() {
        Kill.Entities(getBounds(), "type=trident");
        remainingMobCount = 10;
    }

    @Override
    BossbarValue getBossbarValue() {
        if (remainingMobCount <= 2) {
            return new BossbarValue(remainingMobCount, 2, "WAVE 2 BOSS");
        } else {
            return new BossbarValue(remainingMobCount - 2, 8, "WAVE 2");
        }
    }

    @Override
    String getMessageDisplayString() {
        return "WAVE2";
    }

    @Override
    Point3i getRespawnLocation() {
        return new Point3i(x(1), y(-59), z(-281));
    }

    // 黄色チーム用 ocean monument ステージの原点: (-9, -59, -300)

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
        return origin.z + (z - (-300));
    }
}