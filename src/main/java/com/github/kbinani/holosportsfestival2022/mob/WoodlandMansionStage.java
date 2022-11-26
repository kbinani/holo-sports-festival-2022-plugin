package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;

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
            case 0 -> {
                summonPillager(-8, -56, -332);
                summonWitch(-6, -59, -349);
                summonPillager(3, -59, -349);
                summonVindicator(9, -59, -339);
                summonWitch(9, -56, -332);
                summonPillager(-7, -49, -342);
                summonVindicator(1, -49, -349);
                summonWitch(5, -49, -332);
            }
            case 1 -> {
                summonRavager(-1, -59, -349);
                addGlowingEffect();
            }
        }
    }

    private void summonRavager(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.RAVAGER, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Ravager ravager = (Ravager) it;
            EntityEquipment equipment = ravager.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            ravager.addScoreboardTag(kEntityTag);
            ravager.addScoreboardTag(stageEntityTag);
            ravager.setLootTable(LootTables.EMPTY.getLootTable());
            ravager.setPersistent(true);
            ravager.setCanPickupItems(false);
        });
    }

    private void summonVindicator(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.VINDICATOR, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Vindicator vindicator = (Vindicator) it;
            EntityEquipment equipment = vindicator.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            equipment.setItemInMainHand(new ItemStack(Material.IRON_AXE));
            vindicator.addScoreboardTag(kEntityTag);
            vindicator.addScoreboardTag(stageEntityTag);
            vindicator.setLootTable(LootTables.EMPTY.getLootTable());
            vindicator.setPersistent(true);
            vindicator.setCanPickupItems(false);
        });
    }

    private void summonWitch(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.WITCH, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Witch witch = (Witch) it;
            EntityEquipment equipment = witch.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            witch.addScoreboardTag(kEntityTag);
            witch.addScoreboardTag(stageEntityTag);
            witch.setLootTable(LootTables.EMPTY.getLootTable());
            witch.setPersistent(true);
            witch.setCanPickupItems(false);
        });
    }

    private void summonPillager(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.PILLAGER, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Pillager pillager = (Pillager) it;
            EntityEquipment equipment = pillager.getEquipment();
            DisableDrop(equipment);
            equipment.clear();
            equipment.setItemInMainHand(new ItemStack(Material.CROSSBOW));
            pillager.addScoreboardTag(kEntityTag);
            pillager.addScoreboardTag(stageEntityTag);
            pillager.setLootTable(LootTables.EMPTY.getLootTable());
            pillager.setPersistent(true);
            pillager.setCanPickupItems(false);
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
        } else if (after == 1) {
            return Optional.of(Result.Step());
        } else {
            return Optional.of(Result.Empty());
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