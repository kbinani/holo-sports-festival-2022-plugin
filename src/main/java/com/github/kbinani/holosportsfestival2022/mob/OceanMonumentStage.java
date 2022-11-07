package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(-7), y(-42), z(-283), kEntityTag);
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(3), y(-48), z(-297), kEntityTag);
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(-3), y(-52), z(-282), kEntityTag);
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(-8), y(-59), z(-285), kEntityTag);
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(-3), y(-59), z(-293), kEntityTag);

                execute("summon guardian %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(9), y(-48), z(-298), kEntityTag);
                execute("summon guardian %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(9), y(-55), z(-289), kEntityTag);
                execute("summon guardian %d %d %d {ArmorItems:[{},{},{},{}],Tags:[\"%s\"]}", x(9), y(-57), z(-294), kEntityTag);
                break;
            case 1:
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],HandItems:[{id:trident,Count:1}],Tags:[\"%s\"]}", x(-3), y(-59), z(-299), kEntityTag);
                execute("summon drowned %d %d %d {ArmorItems:[{},{},{},{}],HandItems:[{id:trident,Count:1}],Tags:[\"%s\"]}", x(5), y(-43), z(-297), kEntityTag);
                execute("effect give @e[type=zombie,x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f] glowing 86400 1 true", box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
                break;
        }
    }

    @Override
    Optional<Next> consumeDeadMob(Entity entity) {
        EntityType type = entity.getType();
        if (type != EntityType.DROWNED && type != EntityType.GUARDIAN) {
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
        } else if (after == 2) {
            return Optional.of(Next.Step());
        }
        return Optional.empty();
    }

    @Override
    void onReset() {
        remainingMobCount = 10;
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