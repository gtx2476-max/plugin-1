package ru.airdrops;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class AirDropListener implements Listener {

    private final AirDropManager manager;

    public AirDropListener(AirDropManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        AirDrop drop = manager.getByBlock(block);
        if (drop == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!drop.isReady()) {
            manager.send(p, manager.f(manager.msg("not-ready"), "time",
                    AirDropManager.formatTime(drop.getSecondsUntilOpen())));
            return;
        }
        p.openInventory(drop.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (manager.getByBlock(e.getBlock()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> manager.getByBlock(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> manager.getByBlock(b) != null);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof AirDropHolder holder) {
            AirDrop drop = holder.getDrop();
            if (!drop.isRemoved() && drop.isReady() && drop.isEmpty()) {
                drop.remove(AirDrop.RemoveReason.LOOTED);
            }
        }
    }
}
