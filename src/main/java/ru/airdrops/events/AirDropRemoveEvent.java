package ru.airdrops.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.airdrops.AirDrop;

/** Вызывается после удаления аирдропа (залутан, исчез по времени, удалён админом, выключение). */
public class AirDropRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AirDrop drop;
    private final AirDrop.RemoveReason reason;

    public AirDropRemoveEvent(AirDrop drop, AirDrop.RemoveReason reason) {
        this.drop = drop;
        this.reason = reason;
    }

    public AirDrop getDrop() { return drop; }
    public AirDrop.RemoveReason getReason() { return reason; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
