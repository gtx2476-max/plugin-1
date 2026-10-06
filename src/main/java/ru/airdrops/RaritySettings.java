package ru.airdrops;

import org.bukkit.Material;

/** Настройки одной редкости, прочитанные из config.yml. */
public record RaritySettings(String name, Material block, int weight, int openDelaySeconds) {
}
