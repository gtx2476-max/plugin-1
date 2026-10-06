package ru.airdrops;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Таблицы лута по редкостям. Чем выше редкость — тем ценнее и реже предметы.
 * Элитры и незеритовая броня — только в LEGENDARY и с очень маленьким шансом (BONUS).
 * Чтобы поменять лут — правь этот файл.
 */
public final class LootTables {

    private record Entry(Material material, int min, int max, int weight) {}

    private record Bonus(Material material, double chancePercent) {}

    private static final Map<Rarity, List<Entry>> TABLES = new EnumMap<>(Rarity.class);
    private static final Map<Rarity, int[]> COUNTS = new EnumMap<>(Rarity.class);
    private static final Map<Rarity, List<Bonus>> BONUS = new EnumMap<>(Rarity.class);

    private static Entry e(Material m, int min, int max, int weight) {
        return new Entry(m, min, max, weight);
    }

    static {
        COUNTS.put(Rarity.COMMON, new int[]{6, 10});
        COUNTS.put(Rarity.RARE, new int[]{9, 14});
        COUNTS.put(Rarity.EPIC, new int[]{12, 18});
        COUNTS.put(Rarity.LEGENDARY, new int[]{16, 24});

        TABLES.put(Rarity.COMMON, List.of(
                e(Material.BREAD, 8, 16, 20),
                e(Material.COOKED_BEEF, 6, 12, 20),
                e(Material.IRON_INGOT, 4, 10, 16),
                e(Material.COAL, 8, 20, 14),
                e(Material.GOLD_INGOT, 2, 6, 10),
                e(Material.ARROW, 16, 32, 10),
                e(Material.EXPERIENCE_BOTTLE, 3, 8, 10),
                e(Material.GOLDEN_CARROT, 4, 8, 8),
                e(Material.IRON_SWORD, 1, 1, 4),
                e(Material.IRON_CHESTPLATE, 1, 1, 3),
                e(Material.DIAMOND, 1, 2, 3)
        ));

        TABLES.put(Rarity.RARE, List.of(
                e(Material.COOKED_BEEF, 16, 32, 14),
                e(Material.IRON_INGOT, 8, 16, 16),
                e(Material.GOLD_INGOT, 6, 12, 12),
                e(Material.DIAMOND, 1, 4, 10),
                e(Material.GOLDEN_APPLE, 1, 3, 10),
                e(Material.EXPERIENCE_BOTTLE, 8, 16, 10),
                e(Material.ENDER_PEARL, 2, 5, 8),
                e(Material.EMERALD, 4, 12, 8),
                e(Material.OBSIDIAN, 4, 8, 6),
                e(Material.DIAMOND_SWORD, 1, 1, 3),
                e(Material.DIAMOND_PICKAXE, 1, 1, 3),
                e(Material.DIAMOND_HELMET, 1, 1, 2),
                e(Material.DIAMOND_BOOTS, 1, 1, 2)
        ));

        TABLES.put(Rarity.EPIC, List.of(
                e(Material.DIAMOND, 4, 10, 14),
                e(Material.EMERALD, 8, 20, 12),
                e(Material.GOLDEN_APPLE, 3, 6, 12),
                e(Material.EXPERIENCE_BOTTLE, 16, 32, 10),
                e(Material.ENDER_PEARL, 4, 8, 8),
                e(Material.OBSIDIAN, 8, 16, 6),
                e(Material.DIAMOND_BLOCK, 1, 2, 4),
                e(Material.ANCIENT_DEBRIS, 1, 3, 4),
                e(Material.TOTEM_OF_UNDYING, 1, 1, 3),
                e(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 3),
                e(Material.DIAMOND_CHESTPLATE, 1, 1, 3),
                e(Material.DIAMOND_LEGGINGS, 1, 1, 3),
                e(Material.DIAMOND_SWORD, 1, 1, 3),
                e(Material.NETHERITE_INGOT, 1, 1, 2)
        ));

        TABLES.put(Rarity.LEGENDARY, List.of(
                e(Material.DIAMOND, 8, 16, 14),
                e(Material.EMERALD_BLOCK, 1, 3, 8),
                e(Material.DIAMOND_BLOCK, 2, 4, 8),
                e(Material.EXPERIENCE_BOTTLE, 32, 64, 10),
                e(Material.ENDER_PEARL, 8, 16, 8),
                e(Material.ANCIENT_DEBRIS, 2, 5, 8),
                e(Material.NETHERITE_SCRAP, 2, 5, 6),
                e(Material.ENCHANTED_GOLDEN_APPLE, 2, 4, 7),
                e(Material.TOTEM_OF_UNDYING, 1, 2, 6),
                e(Material.NETHERITE_INGOT, 1, 3, 5),
                e(Material.GOLDEN_APPLE, 6, 12, 8)
        ));

        // Очень редкие бонусы: каждый ролится отдельно, максимум по 1 шт. за дроп.
        BONUS.put(Rarity.LEGENDARY, List.of(
                new Bonus(Material.ELYTRA, 4.0),
                new Bonus(Material.NETHERITE_HELMET, 3.0),
                new Bonus(Material.NETHERITE_CHESTPLATE, 3.0),
                new Bonus(Material.NETHERITE_LEGGINGS, 3.0),
                new Bonus(Material.NETHERITE_BOOTS, 3.0)
        ));
    }

    private LootTables() {}

    public static List<ItemStack> roll(Rarity rarity, Random rnd) {
        List<ItemStack> result = new ArrayList<>();
        List<Entry> table = TABLES.get(rarity);
        int[] range = COUNTS.get(rarity);
        int count = range[0] + rnd.nextInt(range[1] - range[0] + 1);

        int total = 0;
        for (Entry en : table) total += en.weight();

        for (int i = 0; i < count; i++) {
            int r = rnd.nextInt(total);
            for (Entry en : table) {
                r -= en.weight();
                if (r < 0) {
                    ItemStack stack = new ItemStack(en.material());
                    int amount = en.min() + rnd.nextInt(en.max() - en.min() + 1);
                    stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));
                    result.add(stack);
                    break;
                }
            }
        }

        List<Bonus> bonus = BONUS.get(rarity);
        if (bonus != null) {
            for (Bonus b : bonus) {
                if (rnd.nextDouble() * 100.0 < b.chancePercent()) {
                    result.add(new ItemStack(b.material(), 1));
                }
            }
        }
        return result;
    }
}
