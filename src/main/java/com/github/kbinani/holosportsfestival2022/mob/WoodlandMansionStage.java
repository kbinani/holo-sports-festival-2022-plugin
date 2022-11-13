package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.Optional;

class WoodlandMansionStage extends Stage {
    private int remainingMobCount = 9;

    WoodlandMansionStage(Point3i origin, @Nonnull StageDelegate delegate) {
        super(origin, delegate);
    }

    @Override
    protected void onEntranceOpened(boolean opened) {
        fill(x(1), y(-59), z(-330), x(0), y(-58), z(-330), opened ? "air" : "bedrock");
    }

    @Override
    protected void onExitOpened(boolean opened) {
        fill(x(1), y(-59), z(-352), x(0), y(-58), z(-352), opened ? "air" : "bedrock");
    }

    @Override
    public Point3i getSize() {
        return new Point3i(20, 20, 21);
    }

    @Override
    protected int getStepCount() {
        return 0;
    }

    @Override
    void summonMobs(int step) {
        switch (step) {
            case 0:
                summonPillager(-8, -56, -332);
                summonWitch(-6, -59, -349);
                summonPillager(3, -59, -349);
                summonVindicator(9, -59, -339);
                summonWitch(9, -56, -332);

                summonPillager(-7, -49, -342);
                summonVindicator(1, -49, -349);
                summonWitch(5, -49, -332);
                break;
            case 1:
                execute("summon ravager %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"],DeathLootTable:\"minecraft:empty\"}", x(-1), y(-59), z(-349), kEntityTag);
                BoundingBox box = getBounds();
                execute("effect give @e[tag=%s,x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f] glowing 86400 1 true", kEntityTag, box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
                break;
        }
    }

    private void summonVindicator(int x, int y, int z) {
        execute("summon vindicator %d %d %d {HandItems:[{id:iron_axe,Count:1}],ArmorItems:[{},{},{},{}],Tags:[\"%s\"],DeathLootTable:\"minecraft:empty\"}", x(x), y(y), z(z), kEntityTag);
    }

    private void summonWitch(int x, int y, int z) {
        execute("summon witch %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"],DeathLootTable:\"minecraft:empty\"}", x(x), y(y), z(z), kEntityTag);
    }

    private void summonPillager(int x, int y, int z) {
        execute("summon pillager %d %d %d {HandItems:[{id:crossbow,Count:1}],ArmorItems:[{},{},{},{}],Tags:[\"%s\"],DeathLootTable:\"minecraft:empty\"}", x(x), y(y), z(z), kEntityTag);
    }

    @Override
    Optional<Next> consumeDeadMob(Entity entity) {
        EntityType type = entity.getType();
        switch (type) {
            case PILLAGER:
            case WITCH:
            case VINDICATOR:
            case RAVAGER:
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
        } else if (after == 1) {
            return Optional.of(Next.Step());
        } else {
            return Optional.empty();
        }
    }

    @Override
    void onReset() {
        remainingMobCount = 9;
    }

    @Override
    BossbarValue getBossbarValue() {
        if (remainingMobCount <= 1) {
            return new BossbarValue(remainingMobCount, 1, "WAVE 4 BOSS");
        } else {
            return new BossbarValue(remainingMobCount - 1, 8, "WAVE 4");
        }
    }

    @Override
    String getMessageDisplayString() {
        return "WAVE4";
    }

    @Override
    Point3i getRespawnLocation() {
        return new Point3i(x(1), y(-59), z(-331));
    }

    // 黄色チーム用 woodland mansion ステージの原点: (-9, -59, -351)

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
        return origin.z + (z - (-351));
    }
}
