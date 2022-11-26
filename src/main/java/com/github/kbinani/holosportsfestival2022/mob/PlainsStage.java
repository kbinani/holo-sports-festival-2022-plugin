package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;

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
        if (after == 2) {
            return Optional.of(Result.Step());
        } else if (after == 0) {
            return Optional.of(Result.Stage());
        } else {
            return Optional.of(Result.Empty());
        }
    }

    @Override
    void summonMobs(int step) {
        switch (step) {
            case 0 -> {
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
            }
            case 1 -> {
                // 1F
                summonZombieBoss(0, -59, -274);
                // 2F
                summonZombieBoss(-8, -48, -268);
                addGlowingEffect();
            }
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
        }
    }

    private void summonZombie(int x, int y, int z, boolean baby) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.ZOMBIE, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Zombie zombie = (Zombie) it;
            if (baby) {
                zombie.setBaby();
            } else {
                zombie.setAdult();
            }
            EntityEquipment equipment = zombie.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            zombie.addScoreboardTag(kEntityTag);
            zombie.addScoreboardTag(stageEntityTag);
            zombie.setLootTable(LootTables.EMPTY.getLootTable());
            zombie.setPersistent(true);
        });
    }

    private void summonZombieBoss(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.ZOMBIE, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Zombie zombie = (Zombie) it;
            zombie.setAdult();
            EntityEquipment equipment = zombie.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            equipment.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            zombie.addScoreboardTag(kEntityTag);
            zombie.addScoreboardTag(stageEntityTag);
            zombie.setLootTable(LootTables.EMPTY.getLootTable());
            zombie.setPersistent(true);
            AttributeInstance maxHealth = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(200);
            }
            zombie.setHealth(200);
            AttributeInstance movementSpeed = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.setBaseValue(0.345);
            }
        });
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