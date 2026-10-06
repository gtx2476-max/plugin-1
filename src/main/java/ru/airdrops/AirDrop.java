package ru.airdrops;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.airdrops.events.AirDropReadyEvent;
import ru.airdrops.events.AirDropRemoveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Один активный аирдроп: блок + голограмма + инвентарь 54 слота (большой сундук). */
public final class AirDrop {

    public enum RemoveReason { LOOTED, EXPIRED, ADMIN, SHUTDOWN }

    private final AirDropManager manager;
    private final int id;
    private final Rarity rarity;
    private final RaritySettings settings;
    private final Location location; // позиция блока сундука
    private final Inventory inventory;

    private TextDisplay hologram;
    private int secondsLeft;   // до открытия
    private int lifeLeft;      // после открытия до исчезновения
    private boolean ready = false;
    private boolean removed = false;

    public AirDrop(AirDropManager manager, int id, Rarity rarity, RaritySettings settings, Location location, Random random) {
        this.manager = manager;
        this.id = id;
        this.rarity = rarity;
        this.settings = settings;
        this.location = location.clone();
        this.secondsLeft = settings.openDelaySeconds();
        this.lifeLeft = manager.getLifetimeAfterOpen();

        AirDropHolder holder = new AirDropHolder(this);
        this.inventory = Bukkit.createInventory(holder, 54,
                AirDropManager.c(manager.f(manager.msg("gui-title"), "id", id)));
        holder.setInventory(inventory);

        List<ItemStack> loot = LootTables.roll(rarity, random);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 54; i++) slots.add(i);
        Collections.shuffle(slots, random);
        for (int i = 0; i < loot.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), loot.get(i));
        }
    }

    /** Ставит блок, голограмму, эффекты приземления. */
    public void place() {
        World w = location.getWorld();
        location.getBlock().setType(settings.block());

        Location holoLoc = location.clone().add(0.5, 1.7, 0.5);
        hologram = w.spawn(holoLoc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setShadowed(true);
            td.setViewRange(2.0f);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        });
        updateHologram();

        w.strikeLightningEffect(location);
        w.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        w.spawnParticle(Particle.FIREWORK, location.clone().add(0.5, 1, 0.5), 60, 0.5, 1.0, 0.5, 0.05);
    }

    /** Вызывается каждую секунду менеджером. */
    public void tick() {
        if (removed) return;

        Location top = location.clone().add(0.5, 2.5, 0.5);
        location.getWorld().spawnParticle(Particle.END_ROD, top, 4, 0.2, 1.0, 0.2, 0.01);

        if (!ready) {
            secondsLeft--;
            if (secondsLeft <= 0) {
                ready = true;
                location.getWorld().playSound(location, Sound.BLOCK_CHEST_OPEN, 2.0f, 1.0f);
                manager.broadcastReady(this);
                Bukkit.getPluginManager().callEvent(new AirDropReadyEvent(this));
            }
        } else {
            lifeLeft--;
            if (isEmpty()) {
                remove(RemoveReason.LOOTED);
                return;
            }
            if (lifeLeft <= 0) {
                remove(RemoveReason.EXPIRED);
                return;
            }
        }
        updateHologram();
    }

    private void updateHologram() {
        if (hologram == null || !hologram.isValid()) return;
        String line2 = ready
                ? manager.f(manager.msg("holo-ready"), "time", AirDropManager.formatTime(lifeLeft))
                : manager.f(manager.msg("holo-closed"), "time", AirDropManager.formatTime(secondsLeft));
        hologram.text(AirDropManager.c(settings.name() + " &fаирдроп\n" + line2));
    }

    public boolean isEmpty() {
        for (ItemStack it : inventory.getContents()) {
            if (it != null && it.getType() != Material.AIR) return false;
        }
        return true;
    }

    public void remove(RemoveReason reason) {
        if (removed) return;
        removed = true;
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
            viewer.closeInventory();
        }
        if (hologram != null && hologram.isValid()) hologram.remove();
        Block b = location.getBlock();
        if (b.getType() == settings.block()) b.setType(Material.AIR);
        manager.unregister(this);
        manager.broadcastRemoved(this, reason);
        Bukkit.getPluginManager().callEvent(new AirDropRemoveEvent(this, reason));
    }

    public boolean isAt(Block block) {
        return block.getWorld().equals(location.getWorld())
                && block.getX() == location.getBlockX()
                && block.getY() == location.getBlockY()
                && block.getZ() == location.getBlockZ();
    }

    public int getId() { return id; }
    public Rarity getRarity() { return rarity; }
    public RaritySettings getSettings() { return settings; }
    public Location getLocation() { return location.clone(); }
    public Inventory getInventory() { return inventory; }
    public boolean isReady() { return ready; }
    public boolean isRemoved() { return removed; }
    public int getSecondsUntilOpen() { return Math.max(0, secondsLeft); }
}
