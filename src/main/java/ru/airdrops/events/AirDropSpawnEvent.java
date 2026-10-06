package ru.airdrops.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.airdrops.AirDrop;

/** Вызывается перед появлением аирдропа. Можно отменить или изменить инвентарь drop.getInventory(). */
public class AirDropSpawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AirDrop drop;
    private boolean cancelled;

    public AirDropSpawnEvent(AirDrop drop) { this.drop = drop; }

    public AirDrop getDrop() { return drop; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
