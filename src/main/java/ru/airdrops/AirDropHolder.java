package ru.airdrops;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Холдер инвентаря аирдропа — по нему listener понимает, что это наше меню. */
public final class AirDropHolder implements InventoryHolder {
    private final AirDrop drop;
    private Inventory inventory;

    public AirDropHolder(AirDrop drop) {
        this.drop = drop;
    }

    public AirDrop getDrop() {
        return drop;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
