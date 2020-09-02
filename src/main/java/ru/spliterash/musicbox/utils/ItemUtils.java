package ru.spliterash.musicbox.utils;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ItemUtils {
    private final Enchantment enchantment = XEnchantment.PROTECTION_EXPLOSIONS.parseEnchantment();

    public ItemStack glow(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.addEnchant(enchantment, 9999, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack createStack(XMaterial material, String name, @Nullable List<String> lore) {
        ItemStack stack = material.parseItem(true);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null)
            meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isGlow(ItemStack item) {
        return item.getEnchantments().containsKey(enchantment);
    }

    public ItemStack unGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.removeEnchant(enchantment);
        item.setItemMeta(meta);
        return item;
    }

    public List<XMaterial> getEndWith(String endWith) {
        return Arrays.stream(XMaterial.values())
                .filter(s -> s.name().endsWith(endWith))
                .filter(XMaterial::isSupported)
                .collect(Collectors.toList());
    }

    /**
     * Проверяет два массива с предметами на сходство
     *
     * @param c1 Первый массив с предметами
     * @param c2 Второй массив с предметами
     * @return Одинаковые ли массивы
     */
    public boolean isSimilar(ItemStack[] c1, ItemStack[] c2) {
        if (c1 == null || c2 == null)
            return false;
        if (c1.length != c2.length)
            return false;
        for (int i = 0; i < c1.length; i++) {
            ItemStack i1 = c1[i];
            ItemStack i2 = c2[i];
            if (i1 == null && i2 == null)
                continue;
            if (i1 == i2)
                continue;
            if (i1 == null || i2 == null)
                return false;
            if (!i1.isSimilar(i2))
                continue;
            return false;
        }
        return true;
    }

    /**
     * Убирает пробелы в инвентаря, выставляя всё в 1 линию
     */
    public void groupInventory(Inventory inventory) {
        BukkitUtils.checkPrimary();
        int current = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null)
                continue;
            if (i > current) {
                inventory.setItem(current, stack);
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
            current++;
        }
    }

    /**
     * Сдвигает все ячейки инвентаря на N ячеек
     * Пример для 1
     * 0|1|2|3 -> 3|0|1|2 -> 2|3|0|1 -> 1|2|3|0
     *
     * @param inventory Инвентарь который надо сдвинуть
     * @param shift     На сколько будет сдвинут инвентарь
     */
    public void shiftInventory(Inventory inventory, int shift) {
        int currentIndex, movedIndex;
        ItemStack buffer;
        int size = inventory.getSize();
        int greatest = greatestCommonDivisor(shift, size);
        for (int i = 0; i < greatest; i++) {
            buffer = inventory.getItem(i);
            currentIndex = i;
            if (shift > 0) {
                while (true) {
                    movedIndex = currentIndex + shift;
                    if (movedIndex >= size)
                        movedIndex = movedIndex - size;
                    if (movedIndex == i)
                        break;
                    inventory.setItem(currentIndex, inventory.getItem(movedIndex));
                    currentIndex = movedIndex;
                }
            } else if (shift < 0) {
                while (true) {
                    movedIndex = currentIndex + shift;
                    if (movedIndex < 0)
                        movedIndex = size + movedIndex;
                    if (movedIndex == i)
                        break;
                    inventory.setItem(currentIndex, inventory.getItem(movedIndex));
                    currentIndex = movedIndex;
                }
            }
            inventory.setItem(currentIndex, buffer);
        }
    }

    private int greatestCommonDivisor(int a, int b) {
        if (b == 0)
            return a;
        else
            return greatestCommonDivisor(b, a % b);
    }
}
