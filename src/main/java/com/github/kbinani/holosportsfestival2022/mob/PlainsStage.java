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
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(-8), y(-58), z(-264), kEntityTag);
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(-7), y(-59), z(-271), kEntityTag);
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(4), y(-59), z(-274), kEntityTag);
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(10), y(-56), z(-275), kEntityTag);
                // 2F
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(-4), y(-48), z(-265), kEntityTag);
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],IsBaby:1b,Tags:[\"%s\"]}", x(-8), y(-48), z(-269), kEntityTag);
                break;
            case 1:
                // 1F
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(0), y(-59), z(-274), kEntityTag);
                // 2F
                execute("summon zombie %d %d %d {ArmorItems:[{},{},{},{id:leather_helmet,Count:1}],Tags:[\"%s\"]}", x(-8), y(-48), z(-268), kEntityTag);
                execute("effect give @e[type=zombie,x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f] glowing 86400 1 true", box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
                break;
        }
    }


    @Override
    void onReset() {
        remainingMobCount = 8;
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