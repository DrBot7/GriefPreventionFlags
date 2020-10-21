package me.ryanhamshire.GPFlags;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

class SendPlayerMessageTask implements Runnable {

    private final CommandSender player;
    private final ChatColor color;
    private final String message;

    SendPlayerMessageTask(CommandSender player, ChatColor color, String message) {
        this.player = player;
        this.color = color;
        this.message = message;
    }

    @Override
    public void run() {
        if (player == null) {
            GPFlags.addLogEntry(color + message);
            return;
        }

        GPFlags.sendMessage(this.player, this.color, this.message);
    }

}
