package ru.airdrops;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.airdrops.events.AirDropSpawnEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Главный менеджер. Публичный API для других плагинов:
 *   getDrops(), getSecondsUntilNext(), getNextLocation(), spawnDrop(rarity, loc), removeAll().
 */
public final class AirDropManager {

    private final AirDropsPlugin plugin;
    private final Random random = new Random();
    private final Map<Integer, AirDrop> drops = new LinkedHashMap<>();
    private final Map<Rarity, RaritySettings> settings = new EnumMap<>(Rarity.class);

    private BukkitTask task;
    private int nextId = 1;
    private int secondsUntilNext;
    private Location nextLocation;

    public AirDropManager(AirDropsPlugin plugin) {
        this.plugin = plugin;
        loadSettings();
    }

    // ---------- lifecycle ----------

    public void start() {
        scheduleNext();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (AirDrop d : new ArrayList<>(drops.values())) d.remove(AirDrop.RemoveReason.SHUTDOWN);
    }

    public void reload() {
        plugin.reloadConfig();
        loadSettings();
    }

    private void loadSettings() {
        FileConfiguration cfg = plugin.getConfig();
        settings.clear();
        for (Rarity r : Rarity.values()) {
            ConfigurationSection s = cfg.getConfigurationSection("rarities." + r.name());
            String name = r.defaultName;
            Material block = r.defaultBlock;
            int weight = r.defaultWeight;
            int delay = r.defaultDelay;
            if (s != null) {
                name = s.getString("name", name);
                Material m = Material.matchMaterial(s.getString("block", block.name()));
                if (m != null && m.isBlock()) block = m;
                weight = Math.max(0, s.getInt("weight", weight));
                delay = Math.max(1, s.getInt("open-delay-seconds", delay));
            }
            settings.put(r, new RaritySettings(name, block, weight, delay));
        }
    }

    // ---------- main loop ----------

    private void tick() {
        for (AirDrop d : new ArrayList<>(drops.values())) d.tick();

        secondsUntilNext--;
        int pre = plugin.getConfig().getInt("pre-announce-seconds", 60);
        if (pre > 0 && secondsUntilNext == pre) {
            Bukkit.broadcast(c(f(msg("pre-announce"), "time", formatTime(pre))));
        }
        if (secondsUntilNext <= 0) {
            boolean skip = plugin.getConfig().getBoolean("only-if-players-online", true)
                    && Bukkit.getOnlinePlayers().isEmpty();
            if (!skip) {
                Location loc = nextLocation != null ? nextLocation : pickLocation();
                spawnDrop(null, loc);
            }
            scheduleNext();
        }
    }

    private void scheduleNext() {
        secondsUntilNext = Math.max(1, plugin.getConfig().getInt("interval-minutes", 7) * 60);
        nextLocation = pickLocation();
    }

    // ---------- spawning ----------

    /** Спавнит аирдроп. rarity == null -> случайная по весам. Возвращает null, если не получилось/отменено. */
    public AirDrop spawnDrop(Rarity rarity, Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        Rarity r = rarity != null ? rarity : rollRarity();
        AirDrop drop = new AirDrop(this, nextId++, r, settings.get(r), loc, random);

        AirDropSpawnEvent event = new AirDropSpawnEvent(drop);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        drops.put(drop.getId(), drop);
        drop.place();

        for (String line : plugin.getConfig().getStringList("messages.spawned")) {
            Bukkit.broadcast(c(f(line, ph(drop))));
        }
        return drop;
    }

    private Rarity rollRarity() {
        int total = 0;
        for (RaritySettings s : settings.values()) total += s.weight();
        if (total <= 0) return Rarity.COMMON;
        int r = random.nextInt(total);
        for (Rarity rar : Rarity.values()) {
            r -= settings.get(rar).weight();
            if (r < 0) return rar;
        }
        return Rarity.COMMON;
    }

    /** Случайная позиция (над землёй) в кольце min-radius..max-radius вокруг центра. */
    public Location pickLocation() {
        FileConfiguration cfg = plugin.getConfig();
        World w = Bukkit.getWorld(cfg.getString("world", "world"));
        if (w == null) {
            if (Bukkit.getWorlds().isEmpty()) return null;
            w = Bukkit.getWorlds().get(0);
        }
        double min = cfg.getDouble("min-radius", 300);
        double max = cfg.getDouble("max-radius", 3000);
        if (min > max) { double t = min; min = max; max = t; }
        double cx = cfg.getDouble("center-x", 0);
        double cz = cfg.getDouble("center-z", 0);

        for (int i = 0; i < 25; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = min + random.nextDouble() * (max - min);
            int x = (int) Math.round(cx + Math.cos(ang) * dist);
            int z = (int) Math.round(cz + Math.sin(ang) * dist);
            if (!w.getWorldBorder().isInside(new Location(w, x, 64, z))) continue;
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block ground = w.getBlockAt(x, y, z);
            if (ground.isLiquid() || ground.getType() == Material.AIR) continue;
            return new Location(w, x, y + 1, z);
        }
        return null;
    }

    // ---------- registry ----------

    void unregister(AirDrop d) {
        drops.remove(d.getId());
    }

    public AirDrop getByBlock(Block block) {
        for (AirDrop d : drops.values()) if (d.isAt(block)) return d;
        return null;
    }

    public Collection<AirDrop> getDrops() {
        return Collections.unmodifiableCollection(drops.values());
    }

    public int removeAll() {
        int n = drops.size();
        for (AirDrop d : new ArrayList<>(drops.values())) d.remove(AirDrop.RemoveReason.ADMIN);
        return n;
    }

    public int getSecondsUntilNext() { return secondsUntilNext; }
    public Location getNextLocation() { return nextLocation == null ? null : nextLocation.clone(); }
    public int getLifetimeAfterOpen() { return plugin.getConfig().getInt("lifetime-after-open-seconds", 600); }
    public AirDropsPlugin getPlugin() { return plugin; }

    // ---------- broadcasts ----------

    void broadcastReady(AirDrop d) {
        Bukkit.broadcast(c(f(msg("ready"), ph(d))));
    }

    void broadcastRemoved(AirDrop d, AirDrop.RemoveReason reason) {
        if (reason == AirDrop.RemoveReason.LOOTED) {
            Bukkit.broadcast(c(f(msg("looted"), ph(d))));
        } else if (reason == AirDrop.RemoveReason.EXPIRED) {
            Bukkit.broadcast(c(f(msg("expired"), ph(d))));
        }
    }

    // ---------- helpers ----------

    public String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public String f(String template, Object... kv) {
        String s = template.replace("{prefix}", plugin.getConfig().getString("messages.prefix", ""));
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return s;
    }

    /** Плейсхолдеры дропа: id, rarity, x, y, z, world, delay, time. */
    public Object[] ph(AirDrop d) {
        Location l = d.getLocation();
        return new Object[]{
                "id", d.getId(),
                "rarity", d.getSettings().name(),
                "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(),
                "world", l.getWorld().getName(),
                "delay", d.getSettings().openDelaySeconds(),
                "time", formatTime(d.getSecondsUntilOpen())
        };
    }

    public static Component c(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }

    public static String formatTime(int seconds) {
        seconds = Math.max(0, seconds);
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public void send(Player p, String legacy) {
        p.sendMessage(c(legacy));
    }
}
