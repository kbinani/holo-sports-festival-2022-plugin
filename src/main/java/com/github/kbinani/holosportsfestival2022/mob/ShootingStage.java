package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;

import javax.annotation.Nonnull;
import java.util.Optional;

class ShootingStage extends Stage {
    private int remainingMobCount = 16;

    ShootingStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
        fill(x(1), y(-59), z(-356), x(0), y(-58), z(-356), opened ? "air" : "bedrock");
    }

    @Override
    protected void onExitOpened(boolean opened) {
        fill(x(1), y(-59), z(-377), x(0), y(-58), z(-377), opened ? "air" : "bedrock");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(20, 20, 20);
    }

    @Override
    protected int getStepCount() {
        return 1;
    }

    @Override
    void summonMobs(int step) {
        // 1F
        summonZombie(-9, -58, -376);
        summonZombie(-7, -58, -376);

        summonZombie(8, -58, -357);
        summonSkeleton(10, -58, -357);

        // 2F
        summonZombie(-7, -53, -376);
        summonZombie(-7, -53, -368);
        summonSkeleton(-9, -53, -376);

        summonZombie(8, -53, -357);
        summonSkeleton(10, -53, -376);

        // 3F
        summonZombie(-7, -48, -376);
        summonZombie(-7, -48, -368);
        summonSkeleton(-9, -48, -376);
        summonSkeleton(-9, -48, -368);

        summonZombie(8, -48, -357);
        summonZombie(8, -48, -366);
        summonSkeleton(10, -48, -357);
    }

    private void summonSkeleton(int x, int y, int z) {
        execute("summon minecart %d %d %d {Passengers:[{id:skeleton,Tags:[\"%s\"],HandItems:[{id:bow,Count:1}],ArmorItems:[{},{},{},{id:player_head,Count:1,tag:{SkullOwner:\"_ookamimio\"}}]}],Tags:[\"%s\"]}", x(x), y(y), z(z), kEntityTag, kEntityTag);
    }

    private void summonZombie(int x, int y, int z) {
        execute("summon minecart %d %d %d {Passengers:[{id:zombie,Tags:[\"%s\"],ArmorItems:[{},{},{},{id:player_head,Count:1,tag:{SkullOwner:\"sakuramiko35\"}}]}],Tags:[\"%s\"]}", x(x), y(y), z(z), kEntityTag, kEntityTag);
    }

    @Override
    Optional<Next> consumeDeadMob(Entity entity) {
        switch (entity.getType()) {
            case ZOMBIE:
            case SKELETON:
                break;
            default:
                return Optional.empty();
        }
        int before = remainingMobCount;
        int after = Math.max(0, remainingMobCount - 1);
        if (before == after) {
            return Optional.empty();
        }
        remainingMobCount = after;
        if (after == 0) {
            return Optional.of(Next.Stage());
        } else {
            return Optional.empty();
        }
    }

    @Override
    void onReset() {
        remainingMobCount = 16;
    }

    @Override
    BossbarValue getBossbarValue() {
        return new BossbarValue(remainingMobCount, 16, "WAVE FINAL");
    }

    @Override
    String getMessageDisplayString() {
        return "WAVE FINAL";
    }

    // 黄色チーム用 shooting ステージの原点: (-9, -59, -376)

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
        return origin.z + (z - (-376));
    }
}