package com.github.kbinani.holosportsfestival2022;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class ItemBuilder {
    private final ItemStack item;

    public static ItemBuilder For(Material material) {
        return new ItemBuilder(material);
    }

    private ItemBuilder(Material material) {
        item = new ItemStack(material, 1);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder displayName(String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder customByteTag(String name, byte value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(NamespacedKey.minecraft(name), PersistentDataType.BYTE, value);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder potion(PotionType type) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potion = (PotionMeta) meta;
            PotionData data = new PotionData(type);
            potion.setBasePotionData(data);
            item.setItemMeta(potion);
        }
        return this;
    }

    public ItemBuilder firework(FireworkEffect effect) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof FireworkMeta) {
            FireworkMeta firework = (FireworkMeta) meta;
            firework.addEffect(effect);
            item.setItemMeta(firework);
        }
        return this;
    }

    public ItemStack build() {
        return this.item.clone();
    }
}