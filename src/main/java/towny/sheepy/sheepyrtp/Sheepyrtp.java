package towny.sheepy.sheepyrtp;

import org.bukkit.plugin.java.JavaPlugin;
import towny.sheepy.sheepyrtp.commands.rtpcommand;

public final class Sheepyrtp extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("sheepyRTP is now enabled!");
        this.getCommand("rtp")
                .setExecutor(new rtpcommand());
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
