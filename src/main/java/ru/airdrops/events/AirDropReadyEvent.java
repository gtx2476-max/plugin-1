package ru.airdrops.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.airdrops.AirDrop;

/** Вызывается, когда таймер закончился и сундук можно открывать. */
public class AirDropReadyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AirDrop drop;

    public AirDropReadyEvent(AirDrop drop) { this.drop = drop; }

    public AirDrop getDrop() { return drop; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
