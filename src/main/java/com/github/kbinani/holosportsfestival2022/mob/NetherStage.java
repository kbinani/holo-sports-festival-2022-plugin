package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;

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
            case 0 -> {
                summonZombifiedPiglin(-7, -58, -314);
                summonZombifiedPiglin(-5, -59, -321);
                summonZombifiedPiglin(-1, -59, -324);
                summonZombifiedPiglin(4, -59, -322);
                summonZombifiedPiglin(8, -59, -315);
                summonBlaze(-8, -49, -318);
                summonBlaze(7, -47, -323);
                summonBlaze(7, -53, -315);
            }
            case 1 -> {
                summonWitherSkeleton(8, -49, -314);
                summonWitherSkeleton(9, -55, -318);
                summonGhast(-5, -47, -312);
                addGlowingEffect();
            }
        }
    }

    private void summonGhast(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.GHAST, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Ghast ghast = (Ghast) it;
            EntityEquipment equipment = ghast.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            ghast.addScoreboardTag(kEntityTag);
            ghast.addScoreboardTag(stageEntityTag);
            ghast.setLootTable(LootTables.EMPTY.getLootTable());
            ghast.setPersistent(true);
        });
    }

    private void summonWitherSkeleton(int x, int y, int z) {
        // 通常x9, クリティカルx1: https://youtu.be/xIjr6Ct_Wlo?t=3554
        // 通常x5, クリティカルx4: https://youtu.be/26cNq-_8NIY?t=1264
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.WITHER_SKELETON, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            WitherSkeleton witherSkeleton = (WitherSkeleton) it;
            EntityEquipment equipment = witherSkeleton.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            equipment.setItemInMainHand(new ItemStack(Material.STONE_SWORD));
            witherSkeleton.addScoreboardTag(kEntityTag);
            witherSkeleton.addScoreboardTag(stageEntityTag);
            witherSkeleton.setLootTable(LootTables.EMPTY.getLootTable());
            witherSkeleton.setPersistent(true);
            AttributeInstance maxHealth = witherSkeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(200);
            }
            witherSkeleton.setHealth(200);
            AttributeInstance movementSpeed = witherSkeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.setBaseValue(0.345);
            }
        });
    }

    private void summonBlaze(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.BLAZE, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Blaze blaze = (Blaze) it;
            EntityEquipment equipment = blaze.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            blaze.addScoreboardTag(kEntityTag);
            blaze.addScoreboardTag(stageEntityTag);
            blaze.setLootTable(LootTables.EMPTY.getLootTable());
            blaze.setPersistent(true);
        });
    }

    private void summonZombifiedPiglin(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.ZOMBIFIED_PIGLIN, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            PigZombie zombifiedPiglin = (PigZombie) it;
            zombifiedPiglin.setAdult();
            EntityEquipment equipment = zombifiedPiglin.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            equipment.setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
            zombifiedPiglin.addScoreboardTag(kEntityTag);
            zombifiedPiglin.addScoreboardTag(stageEntityTag);
            zombifiedPiglin.setLootTable(LootTables.EMPTY.getLootTable());
            zombifiedPiglin.setPersistent(true);
        });
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