package com.thefancychiken.ezhomes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class EzHomes extends JavaPlugin {
    private HashMap<UUID, List<Location>> homeLocations = new HashMap<>();
    private File homesFile;
    private FileConfiguration homesConfig;
    Yaml yaml = new Yaml();
    InputStream input = this.getClass().getClassLoader().getResourceAsStream("settings.yml");
    Map<Int, Object> obj = yaml.load(inputStream);

    @Override
    public void onEnable() {
        this.getCommand("sethome").setExecutor(new SetHomeCommand());
        this.getCommand("home").setExecutor(new HomeCommand());
        this.getCommand("homes").setExecutor(new ViewHomes());
        createHomesFile();
        loadHomes();
        getLogger().info("EzHomes Enabled");
    }

    @Override
    public void onDisable() {
        saveHomes();
        getLogger().info("EzHomes Disabled");
    }

    private void createHomesFile() {
        homesFile = new File(getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            homesFile.getParentFile().mkdirs();
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void loadHomes() {
        for (String key : homesConfig.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            List<?> locationsList = homesConfig.getList(key);
            List<Location> locations = new ArrayList<>();
            for (Object obj : locationsList) {
                if (obj instanceof Location) {
                    locations.add((Location) obj);
                }
            }
            if (!locations.isEmpty()) {
                homeLocations.put(playerId, locations);
            }
        }
    }

    private void saveHomes() {
        for (UUID playerId : homeLocations.keySet()) {
            List<Location> locations = homeLocations.get(playerId);
            homesConfig.set(playerId.toString(), locations);
        }

        try {
            homesConfig.save(homesFile);
            getLogger().info("Saved homes to file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class SetHomeCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                
                if(args.length < 1){
                    player.sendMessage(ChatColor.RED + "Please provide a name for your home");
                    return false;
                }
                String homeName = args[0];
                List<Location> homes = homeLocations.getOrDefault(player.getUniqueId(), new ArrayList<>());
                homes.add(player.getLocation());
                homeLocations.put(player.getUniqueId(), homes);
                saveHomes();
                player.sendMessage(ChatColor.GREEN + "Home " + homeName + " set!");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return false;
            }
        }
    }

    private class ViewHomes implements CommandExecutor{
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
            if(sender instanceof Player){
                Player player = (Player) sender;
                Location homes[] = homeLocations.get(player.getUniqueId());
            }
        }
    }

    private class HomeCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                List<Location> homes = homeLocations.get(player.getUniqueId());
                if(args.length < 1){
                    player.sendMessage(ChatColor.RED + "Please provide the name of the home you would like to teleport to");
                }
                Location home = homeLocations.get(player.getUniqueId());
                if (home != null) {
                    final Location startLocation = player.getLocation();
                    player.sendMessage(ChatColor.GRAY + "Stay still for 5 seconds to teleport home.");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (startLocation.getBlock().equals(player.getLocation().getBlock())) {
                                player.teleport(home);
                                player.sendMessage(ChatColor.GREEN + "Teleported to home!");
                            } else {
                                player.sendMessage(ChatColor.RED + "Movement detected, teleportation cancelled.");
                            }
                        }
                    }.runTaskLater(EzHomes.this, 100);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "No home set.");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return false;
            }
        }
    }
}
