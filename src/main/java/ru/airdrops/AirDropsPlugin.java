package ru.airdrops;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AirDropsPlugin extends JavaPlugin {

    private static AirDropsPlugin instance;
    private AirDropManager manager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        manager = new AirDropManager(this);
        manager.start();

        getServer().getPluginManager().registerEvents(new AirDropListener(manager), this);

        PluginCommand command = getCommand("airdrops");
        if (command != null) {
            AirDropsCommand handler = new AirDropsCommand(manager);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        getLogger().info("AirDrops включен. Следующий дроп через " + manager.getSecondsUntilNext() + " сек.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
    }

    /** API для других плагинов. */
    public static AirDropsPlugin getInstance() { return instance; }

    public AirDropManager getManager() { return manager; }
}
