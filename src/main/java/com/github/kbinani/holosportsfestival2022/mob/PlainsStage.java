package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.Optional;

class PlainsStage extends Stage {
    int remainingMobCount = 0;

    PlainsStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
        fill(x(0), y(-59), z(-255), x(1), y(-58), z(-255), opened ? "air" : "bedrock");
    }

    @Override
    protected void onExitOpened(boolean opened) {
        fill(x(1), y(-59), z(-276), x(0), y(-58), z(-276), opened ? "air" : "bedrock");
        fill(x(1), y(-48), z(-276), x(0), y(-47), z(-276), opened ? "air" : "bedrock");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(20, 20, 20);
    }

    @Override
    protected int getStepCount() {
        // 通常
        // BOSS
        return 2;
    }

    @Override
    Optional<Next> consumeDeadMob(Entity entity) {
        if (entity.getType() != EntityType.ZOMBIE) {
            return Optional.empty();
        }
        int before = remainingMobCount;
        int after = Math.max(0, remainingMobCount - 1);
        if (before == after) {
            return Optional.empty();
        }
        remainingMobCount = after;
        if (after == 2) {
            return Optional.of(Next.Step());
        } else if (after == 0) {
            return Optional.of(Next.Stage());
        } else {
            return Optional.empty();
        }
    }

    @Override
    void summonMobs(int step) {
        BoundingBox box = getBounds();
        switch (step) {
            case 0:
                // 1F
                summonZombie(-8, -58, -264, false);
                summonZombie(-7, -59, -271, false);
                summonZombie(4, -59, -274, false);
                summonZombie(10, -56, -275, false);
                summonZombie(9, -59, -261, true);
                summonZombie(7, -58, -267, false);
                // 2F
                summonZombie(-4, -48, -265, false);
                summonZombie(-8, -48, -269, true);
                break;
            case 1:
                // 1F
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:diamond_helmet,Count:1}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],Tags:[\"%s\"],Health:200.0f,Attributes:[{Name:\"generic.max_health\",Base:200.0d},{Name:\"generic.movement_speed\",Base:0.345d}],DeathLootTable:\"minecraft:empty\"}", x(0), y(-59), z(-274), kEntityTag);
                // 2F
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:diamond_helmet,Count:1}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],Tags:[\"%s\"],Health:200.0f,Attributes:[{Name:\"generic.max_health\",Base:200.0d},{Name:\"generic.movement_speed\",Base:0.345d}],DeathLootTable:\"minecraft:empty\"}", x(-8), y(-48), z(-268), kEntityTag);
                execute("effect give @e[tag=%s,x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f] glowing 86400 1 true", kEntityTag, box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
                // BOSS 戦の様子 (60fps)
                // https://www.youtube.com/watch?v=TiSgN3lvfrM
                // ==================================================
                // BOSS 1/2
                // --------------------------------------------------
                // frame    critical    note    tick from last attack
                // --------------------------------------------------
                // 2428     false
                // 2525     false               32.3
                // 2584     false               19.7
                // 2641     false               19
                // 2692     false               17
                // 2824     false               44
                // 2888     false               21.3
                // 2923     false               11.7
                // 2971     false               16
                // 3022     false               17
                // 3082     false       killed  20
                // =======================
                // BOSS 2/2
                // -----------------------
                // frame  critical  note
                // -----------------------
                // 3670   false
                // 3703   false
                // 3738   false
                // 3772   false
                // 3808   N/A       fall from high place
                // 4585   false
                // 4660   false
                // 4732   false
                // 4789   false
                // 4897   false
                // 4993   false
                // 5044   false
                // 5092   false     killed
                break;
        }
    }

    private void summonZombie(int x, int y, int z, boolean baby) {
        execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],IsBaby:%db,Tags:[\"%s\"],DeathLootTable:\"minecraft:empty\"}", x(x), y(y), z(z), baby ? 1 : 0, kEntityTag);
    }

    @Override
    void onReset() {
        remainingMobCount = 10;
    }

    @Override
    BossbarValue getBossbarValue() {
        if (remainingMobCount <= 2) {
            return new BossbarValue(remainingMobCount, 2, "WAVE 1 BOSS");
        } else {
            return new BossbarValue(remainingMobCount - 2, 8, "WAVE 1");
        }
    }

    @Override
    String getMessageDisplayString() {
        return "WAVE1";
    }

    @Override
    Point3i getRespawnLocation() {
        return new Point3i(x(1), y(-59), z(-256));
    }

    // 黄色チーム用 plains ステージの原点: (-9, -59, -275)

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
        return origin.z + (z - (-275));
    }
}