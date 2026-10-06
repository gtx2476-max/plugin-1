package ru.airdrops;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /airdrops                         - таймер до следующего дропа + координаты + активные дропы
 * /airdrops spawn [РЕДКОСТЬ] [here] - (admin) заспавнить дроп сейчас (here = на месте игрока)
 * /airdrops remove                  - (admin) удалить все активные дропы
 * /airdrops reload                  - (admin) перезагрузить config.yml
 */
public final class AirDropsCommand implements CommandExecutor, TabCompleter {

    private final AirDropManager m;

    public AirDropsCommand(AirDropManager manager) {
        this.m = manager;
    }

    private void out(CommandSender s, String legacy) {
        s.sendMessage(AirDropManager.c(legacy));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            showInfo(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sender.hasPermission("airdrops.admin")) {
            out(sender, m.f(m.msg("no-permission")));
            return true;
        }
        switch (sub) {
            case "reload" -> {
                m.reload();
                out(sender, m.f(m.msg("reloaded")));
            }
            case "remove" -> out(sender, m.f(m.msg("removed"), "count", m.removeAll()));
            case "spawn" -> {
                Rarity rarity = null;
                boolean here = false;
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("here")) {
                        here = true;
                    } else {
                        try {
                            rarity = Rarity.valueOf(args[i].toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                Location loc = (here && sender instanceof Player p) ? p.getLocation() : m.pickLocation();
                AirDrop d = m.spawnDrop(rarity, loc);
                if (d == null) out(sender, m.f(m.msg("spawn-failed")));
            }
            default -> showInfo(sender);
        }
        return true;
    }

    private void showInfo(CommandSender sender) {
        out(sender, m.f(m.msg("info-next"), "time", AirDropManager.formatTime(m.getSecondsUntilNext())));
        Location next = m.getNextLocation();
        if (next != null && m.getPlugin().getConfig().getBoolean("show-next-location", true)) {
            out(sender, m.f(m.msg("info-next-loc"),
                    "x", next.getBlockX(), "z", next.getBlockZ(), "world", next.getWorld().getName()));
        }
        if (m.getDrops().isEmpty()) {
            out(sender, m.f(m.msg("info-none")));
            return;
        }
        out(sender, m.f(m.msg("info-active-header")));
        for (AirDrop d : m.getDrops()) {
            String key = d.isReady() ? "info-active-open" : "info-active-closed";
            out(sender, m.f(m.msg(key), m.ph(d)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        if (!sender.hasPermission("airdrops.admin")) return res;
        if (args.length == 1) {
            for (String s : List.of("spawn", "remove", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) res.add(s);
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("spawn")) {
            for (Rarity r : Rarity.values()) res.add(r.name());
            res.add("here");
        }
        return res;
    }
}
