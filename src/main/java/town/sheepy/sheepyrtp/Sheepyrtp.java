package town.sheepy.sheepyrtp;

import org.bukkit.plugin.java.JavaPlugin;
import town.sheepy.sheepyrtp.commands.rtpcommand;

public final class Sheepyrtp extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("sheepyRTP is now enabled!");
        this.getCommand("rtp")
                .setExecutor(new rtpcommand(this));
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
