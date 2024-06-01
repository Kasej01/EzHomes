package ezhomes;

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
import org.bukkit.configuration.ConfigurationSection;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class EzHomes extends JavaPlugin {
    private Map<UUID, Map<String, Location>> homeLocations = new HashMap<>();
    private File homesFile;
    private FileConfiguration homesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();  // Save the default config from resources if not present
        reloadConfig();  // Load the configuration

        // Setup commands and other initialization
        this.getCommand("sethome").setExecutor(new SetHomeCommand());
        this.getCommand("home").setExecutor(new HomeCommand());
        this.getCommand("homes").setExecutor(new ViewHomes());
        this.getCommand("delhome").setExecutor(new DelHome());
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
            ConfigurationSection homesSection = homesConfig.getConfigurationSection(key);
            HashMap<String, Location> homes = new HashMap<>();
            if (homesSection != null) {
                for (String homeName : homesSection.getKeys(false)) {
                    Location location = homesSection.getLocation(homeName);
                    if (location != null) {
                        homes.put(homeName, location);
                    }
                }
            }
            homeLocations.put(playerId, homes);
        }
    }


    private void saveHomes() {
        for (UUID playerId : homeLocations.keySet()) {
            Map<String, Location> homes = homeLocations.get(playerId);
            for (HashMap.Entry<String, Location> entry : homes.entrySet()) {
                homesConfig.set(playerId.toString() + "." + entry.getKey(), entry.getValue());
            }
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
                Map<String, Location> homes = homeLocations.getOrDefault(player.getUniqueId(), new HashMap<>());
                if(homes.size() < getConfig().getInt("settings.maxHomes")){
                    homes.put(homeName, player.getLocation());
                    homeLocations.put(player.getUniqueId(), homes);
                    saveHomes();
                    player.sendMessage(ChatColor.GREEN + "Home " + homeName + " set!");
                    return true;
                }
                else{
                    player.sendMessage(ChatColor.RED + "You already have the maximum amount of homes allowed (" + getConfig().getInt("settings.maxHomes") + ")");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return false;
            }
        }
    }


    private class ViewHomes implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Map<String, Location> homes = homeLocations.get(player.getUniqueId());
                if (homes != null && !homes.isEmpty()) {
                    StringBuilder homeList = new StringBuilder(ChatColor.GREEN + "Homes:\n");
                    for (Map.Entry<String, Location> entry : homes.entrySet()) {
                        homeList.append(ChatColor.YELLOW).append(entry.getKey())
                                .append("   ");
                    }
                    sender.sendMessage(homeList.toString());
                } else {
                    sender.sendMessage(ChatColor.RED + "You have no homes set.");
                }
                return true;
            }
            return false;
        }
    }

    private class DelHome implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
            if(sender instanceof Player){
                Player player = (Player) sender;
                if(args.length < 1){
                    player.sendMessage(ChatColor.RED + "Please provide the name of the home you would like to delete");
                    return true;
                }

                Map<String, Location> homes = homeLocations.get(player.getUniqueId());
                String homeName = args[0];
                if(homes == null || !homes.containsKey(homeName)){
                    player.sendMessage(ChatColor.RED + "Could not find home " + homeName);
                    return true;  // Returning true because the command was processed but unsuccessful
                }

                // Remove the specified home from the memory map
                homes.remove(homeName);
                if(homes.isEmpty()){
                    homeLocations.remove(player.getUniqueId());
                } else {
                    homeLocations.put(player.getUniqueId(), homes);
                }

                // Call the function to delete the home from the config
                delHomeFromConfig(player.getUniqueId(), homeName);

                player.sendMessage(ChatColor.GREEN + "Home " + homeName + " deleted!");
                return true;
            }
            // Command was not executed because the sender is not a player
            return false; 
        }

        private void delHomeFromConfig(UUID playerId, String homeName) {
            // Check if the player's homes exist in the config
            if (homesConfig.contains(playerId.toString() + "." + homeName)) {
                // Remove the specific home entry from the configuration
                homesConfig.set(playerId.toString() + "." + homeName, null);
                // Save the config to file
                try {
                    homesConfig.save(homesFile);
                    getLogger().info("Updated homes configuration file.");
                } catch (IOException e) {
                    getLogger().severe("Could not save the homes configuration file: " + e.getMessage());
                }
            }
        }

    }



    private class HomeCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length < 1) {
                    player.sendMessage(ChatColor.RED + "Please provide the name of the home you would like to teleport to");
                    return false;
                }

                String homeName = args[0];
                Map<String, Location> homes = homeLocations.get(player.getUniqueId());
                if (homes == null || !homes.containsKey(homeName)) {
                    player.sendMessage(ChatColor.RED + "No home set with the name: " + homeName);
                    return false;
                }
                final Location home = homes.get(homeName);
                int delay = getConfig().getInt("settings.teleportDelay");

                final Location startLocation = player.getLocation();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (startLocation.getBlock().equals(player.getLocation().getBlock())) {
                            player.teleport(home);
                            player.sendMessage(ChatColor.GREEN + "Teleported to home " + homeName + "!");
                        } else {
                            player.sendMessage(ChatColor.RED + "Movement detected, teleportation cancelled.");
                        }
                    }
                }.runTaskLater(EzHomes.this, delay);
                player.sendMessage(ChatColor.GRAY + "Stay still for " + (delay / 20) + " seconds to teleport home.");
                return true;
            }
            return false;
        }
    }


}
