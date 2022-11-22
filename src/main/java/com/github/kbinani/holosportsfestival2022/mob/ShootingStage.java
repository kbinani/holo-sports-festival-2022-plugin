package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.ItemBuilder;
import com.github.kbinani.holosportsfestival2022.Kill;
import com.github.kbinani.holosportsfestival2022.Point3i;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.loot.LootTables;

import javax.annotation.Nonnull;
import java.util.List;
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
        World world = delegate.stageGetWorld();
        if (world == null) {
            return;
        }
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.MINECART, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Minecart minecart = (Minecart) it;
            minecart.addScoreboardTag(stageEntityTag);

            world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.SKELETON, CreatureSpawnEvent.SpawnReason.COMMAND, e -> {
                Skeleton skeleton = (Skeleton) e;
                EntityEquipment equipment = skeleton.getEquipment();
                DisableDrop(equipment);
                equipment.clear();
                equipment.setItemInMainHand(new ItemStack(Material.BOW));
                ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = helmet.getItemMeta();
                if (meta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("_ookamimio"));
                    helmet.setItemMeta(skullMeta);
                }
                equipment.setHelmet(helmet);
                skeleton.addScoreboardTag(kEntityTag);
                skeleton.addScoreboardTag(stageEntityTag);
                skeleton.setLootTable(LootTables.EMPTY.getLootTable());
                skeleton.setPersistent(true);

                minecart.addPassenger(skeleton);
            });
        });
    }

    private void summonZombie(int x, int y, int z) {
        World world = delegate.stageGetWorld();
        if (world == null) {
            return;
        }
        world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.MINECART, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
            Minecart minecart = (Minecart) it;
            minecart.addScoreboardTag(stageEntityTag);

            world.spawnEntity(new Location(world, x(x) + 0.5, y(y), z(z) + 0.5), EntityType.ZOMBIE, CreatureSpawnEvent.SpawnReason.COMMAND, e -> {
                Zombie zombie = (Zombie) e;
                zombie.setAdult();
                EntityEquipment equipment = zombie.getEquipment();
                DisableDrop(equipment);
                equipment.clear();
                ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = helmet.getItemMeta();
                if (meta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("sakuramiko35"));
                    helmet.setItemMeta(skullMeta);
                }
                equipment.setHelmet(helmet);
                zombie.addScoreboardTag(kEntityTag);
                zombie.addScoreboardTag(stageEntityTag);
                zombie.setLootTable(LootTables.EMPTY.getLootTable());
                zombie.setPersistent(true);

                minecart.addPassenger(zombie);
            });
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
        } else {
            return Optional.of(Result.Empty());
        }
    }

    @Override
    void onReset() {
        Kill.EntitiesByType(getBounds(), EntityType.DROPPED_ITEM);
        Kill.EntitiesByType(getBounds(), EntityType.ARROW);
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

    @Override
    Point3i getRespawnLocation() {
        return new Point3i(x(1), y(-59), z(-357));
    }

    @Override
    void onStart(List<Player> players) {
        for (Player player : players) {
            PlayerInventory inventory = player.getInventory();
            ItemStack arrow =
                    ItemBuilder.For(Material.ARROW)
                            .amount(1)
                            .customByteTag(MobFightEventListener.kItemTag, (byte) 1)
                            .build();
            ItemStack bow =
                    ItemBuilder.For(Material.BOW)
                            .amount(1)
                            .customByteTag(MobFightEventListener.kItemTag, (byte) 1)
                            .enchant(Enchantment.ARROW_INFINITE, 1)
                            .enchant(Enchantment.DURABILITY, 3)
                            .build();
            inventory.addItem(arrow);
            inventory.addItem(bow);
        }
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