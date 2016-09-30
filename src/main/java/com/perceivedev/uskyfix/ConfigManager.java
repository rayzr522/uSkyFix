/**
 * 
 */
package com.perceivedev.uskyfix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.YamlConfiguration;

import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.api.IslandLevel;

/**
 * @author Rayzr
 *
 */
public class ConfigManager {

    public static final int CONFIG_VERSION = 2;
    private List<Location>  signs          = new ArrayList<Location>();

    private uSkyFix         plugin;
    private String          prefix;

    public String           permissionSignPlace;
    public String           permissionSignBreak;
    public String           permissionCommand;

    public String           messageSignPlaced;
    public String           messageSignBroken;
    public String           messageNoPermission;
    public String           messageError;
    public String           messageVersion;
    public String           messageCommandUsage;
    public String           messageConfigReloaded;
    public String           messageRefreshed;

    public ConfigManager(uSkyFix plugin) {
        this.plugin = plugin;

    }

    @SuppressWarnings("unchecked")
    public void load() {

        plugin.reloadConfig();

        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        YamlConfiguration config = getConfig("signs.yml");
        if (config.contains("signs")) {
            List<?> list = config.getList("signs");
            if (list.size() > 0 && !Location.class.isAssignableFrom(list.get(0).getClass())) {
                return;
            }
            signs = (List<Location>) config.getList("signs");
        }

        config = getConfig("config.yml");
        if (!config.contains("config-version") || config.getInt("config-version") != CONFIG_VERSION) {
            plugin.saveResource("config.yml", true);
            config = getConfig("config.yml");
        }

        prefix = config.getString("chat-prefix");

        permissionSignPlace = config.getString("permissions.place-sign");
        permissionSignBreak = config.getString("permissions.break-sign");
        permissionCommand = config.getString("permissions.command");

        messageSignPlaced = msg(config, "sign-placed");
        messageSignBroken = msg(config, "sign-broken");
        messageNoPermission = msg(config, "no-permission");
        messageError = msg(config, "error");
        messageVersion = msg(config, "version");
        messageCommandUsage = msg(config, "command-usage");
        messageConfigReloaded = msg(config, "config-reloaded");
        messageRefreshed = msg(config, "refreshed");

    }

    private String msg(YamlConfiguration config, String path) {
        return ChatColor.translateAlternateColorCodes('&', prefix + config.getString("messages." + path));
    }

    public void save() {

        YamlConfiguration config = getConfig("signs.yml");

        config.set("signs", signs);

        saveConfig(config, "signs.yml");

    }

    public void saveConfig(YamlConfiguration config, String path) {

        try {
            // This doesn't do anything if it already exists, so no need to
            // check if it exists
            getFile(path).createNewFile();
            config.save(getFile(path));
        } catch (IOException e) {
            System.err.println("Failed to save config file to path '" + path + "'");
            e.printStackTrace();
        }

    }

    public YamlConfiguration getConfig(String path) {
        return YamlConfiguration.loadConfiguration(getFile(path));
    }

    public File getFile(String path) {
        return new File(plugin.getDataFolder(), path.replace("/", File.separator));
    }

    /**
     * @return the plugin
     */
    public uSkyFix getPlugin() {
        return plugin;
    }

    /**
     * @return the signs
     */
    public List<Location> getSigns() {
        return signs;
    }

    /**
     * @param signs the signs to set
     */
    public void setSigns(List<Location> signs) {
        this.signs = signs;
    }

    /**
     * @param signs the signs to set
     */
    public void setSigns(Location... signs) {
        setSigns(Arrays.asList(signs));
    }

    /**
     * @param sign the sign to add
     */
    public boolean addSign(Location sign) {
        if (signs.contains(sign) || !(sign.getBlock().getState() instanceof Sign)) {
            return false;
        }
        return signs.add(sign);
    }

    /**
     * @param sign the sign to remove
     */
    public boolean removeSign(Location sign) {
        if (!(sign.getBlock().getState() instanceof Sign)) {
            return false;
        }
        return signs.remove(sign);
    }

    /**
     * Updates all signs
     */
    public void update() {
        uSkyBlock.getInstance().getIslandLogic().generateTopTen(Bukkit.getConsoleSender());
        Iterator<Location> iterator = signs.iterator();
        while (iterator.hasNext()) {
            Location loc = iterator.next();
            if (!update(loc)) {
                signs.remove(loc);
            }
        }
    }

    /**
     * @param location
     */
    public boolean update(Location loc) {
        if (!Signs.isLeaderboardSign(loc.getBlock(), this)) {
            signs.remove(loc);
            return false;
        }
        Block block = loc.getBlock();

        Sign sign = (Sign) block.getState();

        int place = Signs.getPlace(block);

        if (place < 0) {
            Signs.setLines(sign, place, "None", "");
            return true;
        }

        IslandLevel level = uSkyBlock.getAPI().getTopTen().size() >= place ? uSkyBlock.getAPI().getTopTen().get(place - 1) : null;

        if (level == null) {
            Signs.setLines(sign, place, "None", "");
            return true;
        }

        Signs.setLines(sign, place, level.getLeaderName(), String.format("%.2f", level.getScore()));

        Block head = block.getRelative(BlockFace.UP);
        if (head != null && head.getType() == Material.SKULL) {
            Skull skull = (Skull) head.getState();
            if (skull.getSkullType() == SkullType.PLAYER) {
                skull.setOwner(level.getLeaderName());
                skull.update();
            }
        }

        return true;

    }

    /**
     * @param sign
     * @return
     */
    public boolean isSign(Block sign) {
        return isSign(sign.getLocation());
    }

    /**
     * @param sign
     * @return
     */
    public boolean isSign(Location sign) {
        return signs.contains(sign);
    }

}
