package ru.airdrops;

import org.bukkit.Material;

/** Редкости аирдропа. Значения по умолчанию; реальные берутся из config.yml (rarities.*). */
public enum Rarity {
    COMMON("&7Обычный", Material.BARREL, 55, 30),
    RARE("&9Редкий", Material.CHEST, 28, 40),
    EPIC("&5Эпический", Material.ENDER_CHEST, 14, 50),
    LEGENDARY("&6Легендарный", Material.BEACON, 3, 60);

    public final String defaultName;
    public final Material defaultBlock;
    public final int defaultWeight;
    public final int defaultDelay;

    Rarity(String defaultName, Material defaultBlock, int defaultWeight, int defaultDelay) {
        this.defaultName = defaultName;
        this.defaultBlock = defaultBlock;
        this.defaultWeight = defaultWeight;
        this.defaultDelay = defaultDelay;
    }
}
