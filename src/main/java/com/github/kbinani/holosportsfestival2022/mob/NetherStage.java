package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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

                //TODO: ghast の攻撃でステージが破壊されないようにする
                execute("summon ghast %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(-5), y(-47), z(-312), kEntityTag);

                BoundingBox box = getBounds();
                execute("effect give @e[tag=%s,x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f] glowing 86400 1 true", kEntityTag, box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
                break;
        }
    }

    private void summonWitherSkeleton(int x, int y, int z) {
        execute("summon wither_skeleton %d %d %d {HandItems:[{id:stone_sword,Count:1}],ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", this.x(x), this.y(y), this.z(z), kEntityTag);
    }

    private void summonBlaze(int x, int y, int z) {
        execute("summon blaze %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", this.x(x), this.y(y), this.z(z), kEntityTag);
    }

    private void summonZombifiedPiglin(int x, int y, int z) {
        execute("summon zombified_piglin %d %d %d {HandItems:[{id:golden_sword,Count:1}],ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", this.x(x), this.y(y), this.z(z), kEntityTag);
    }

    @Override
    Optional<Next> consumeDeadMob(Entity entity) {
        EntityType type = entity.getType();
        switch (type) {
            case ZOMBIFIED_PIGLIN:
            case BLAZE:
            case WITHER_SKELETON:
            case GHAST:
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
        } else if (after == 3) {
            return Optional.of(Next.Step());
        } else {
            return Optional.empty();
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