package com.tr.Comfort;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TRComfort extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Set<String> forbiddenWords = new HashSet<>();
    private boolean enableDetection;
    private boolean logToConsole;
    private boolean replaceWords;
    private boolean warnPlayer;
    private String replacement;
    private String warnMessage;
    private String comfortFormat;

    @Override
    public void onEnable() {
        // 创建/加载配置文件
        createConfig();
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册命令
        getCommand("trc").setExecutor(this);
        
        getLogger().info(ChatColor.GREEN + "TRComfort 插件已启用！加载违禁词: " + forbiddenWords.size() + "个");
    }

    private void createConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        reloadConfigValues();
    }

    private void reloadConfigValues() {
        forbiddenWords.clear();
        forbiddenWords.addAll(config.getStringList("banned-words"));
        enableDetection = config.getBoolean("enable", true);
        logToConsole = config.getBoolean("log-to-console", true);
        replaceWords = config.getBoolean("replace-banned-words", true);
        warnPlayer = config.getBoolean("warn-player", true);
        replacement = config.getString("replacement-string", "***");
        warnMessage = ChatColor.translateAlternateColorCodes('&', 
                    config.getString("warn-message", "&c请注意用语文明！"));
        comfortFormat = ChatColor.translateAlternateColorCodes('&', 
                    config.getString("comfort-format", "&6[安慰] &f{message}"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enableDetection) return;
        Player player = event.getPlayer();
        
        // 检查权限豁免
        if (player.hasPermission("trcomfort.exempt")) return;
        
        String message = event.getMessage();
        String lowerMessage = message.toLowerCase();
        
        // 检测违禁词
        String detectedWord = null;
        for (String word : forbiddenWords) {
            if (lowerMessage.contains(word.toLowerCase())) {
                detectedWord = word;
                break;
            }
        }
        
        if (detectedWord != null) {
            // 控制台警告
            if (logToConsole) {
                String logMsg = String.format("[TRComfort] 玩家 %s 说了违禁词: '%s' | 完整内容: '%s'",
                        player.getName(), detectedWord, message);
                getLogger().warning(logMsg);
            }
            
            // 玩家提示
            if (warnPlayer) {
                player.sendMessage(warnMessage);
            }
            
            // 替换违禁词
            if (replaceWords) {
                String newMessage = message.replaceAll("(?i)" + detectedWord, replacement);
                event.setMessage(newMessage);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("trc")) {
            // 重载配置
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("trcomfort.reload")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
                    return true;
                }
                reloadConfigValues();
                sender.sendMessage(ChatColor.GREEN + "配置已重新加载！");
                return true;
            }
            
            // 安慰命令
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "用法: /trc <玩家> <内容>");
                sender.sendMessage(ChatColor.GRAY + "或使用 /trc reload 重载配置");
                return false;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "玩家 " + args[0] + " 不在线或不存在");
                return true;
            }

            // 组合安慰消息
            StringBuilder comfortMsg = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                comfortMsg.append(args[i]).append(" ");
            }
            
            String formattedMsg = comfortFormat.replace("{message}", comfortMsg.toString().trim());
            target.sendMessage(formattedMsg);
            sender.sendMessage(ChatColor.GREEN + "已向 " + target.getName() + " 发送安慰消息");
            return true;
        }
        return false;
    }
}
