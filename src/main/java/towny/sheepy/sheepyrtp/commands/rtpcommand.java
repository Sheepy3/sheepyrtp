package towny.sheepy.sheepyrtp.commands;

import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.Set;
import org.bukkit.Tag;
public class rtpcommand implements CommandExecutor {
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        Set<Material> bad = Set.of(
            Material.WATER,
            Material.LAVA,
            Material.CACTUS,
            Material.POWDER_SNOW,
            Material.SEAGRASS
        );

        Player player = (Player) sender;
        World world = player.getWorld();
        int RADIUS = 200;

        Location target;
        int x,y,z;
        Block surface, ground;
        final int MAX_TRIES = 10_000;
        int tries = 0;
        do {

            if (++tries > MAX_TRIES) {
                sender.sendMessage("§cCouldn’t find a safe spot, try again later.");
                return true;          // bail out gracefully
            }


            x = ThreadLocalRandom.current().nextInt(-RADIUS, RADIUS + 1);
            z = ThreadLocalRandom.current().nextInt(-RADIUS, RADIUS + 1);
            y = world.getHighestBlockYAt(x, z);
            surface = world.getBlockAt(x,y,z);
            
            if (surface.isLiquid()){
                ground = surface;
                continue;
            }
            ground = surface;
            while(y>world.getMinHeight()
                    && (Tag.LEAVES.isTagged(ground.getType())
                    || !ground.getType().isSolid() )
            ) {
                y--;
                ground= world.getBlockAt(x,y,z);
            }
        }while (bad.contains(ground.getType()) ||
            !world.getBlockAt(x,y+1,z).isEmpty() ||
            !world.getBlockAt(x,y+2,z).isEmpty() ||
                !isFarFromTowns(world,ground.getChunk().getX(),ground.getChunk().getZ(),15)
        );


        target = new Location(
                world,
                x + 0.5,
                y+ 1,      // one above the ground
                z + 0.5
        );



        player.teleport(target);
        player.sendMessage(ground.getType().toString());
        player.sendMessage(ground.getLocation().toString());
        //player.sendMessage("✈ You’ve been RTP’d to " +
         //       target.getBlockX() + ", " +
          //      target.getBlockY() + ", " +
           //     target.getBlockZ());

        // your logic here
        sender.sendMessage("You ran /" + label);
        return true;  // true means “handled” (no usage message)
    }

    private boolean isFarFromTowns(World world, int chunkX, int chunkZ, int safeRadius) {
        TownyAPI api = TownyAPI.getInstance();
        String worldName = world.getName();

        for (int dx = -safeRadius; dx <= safeRadius; dx++) {
            for (int dz = -safeRadius; dz <= safeRadius; dz++) {
                WorldCoord wc = new WorldCoord(worldName, chunkX + dx, chunkZ + dz);
                if (!api.isWilderness(wc)) {        // claimed by some Town
                    return false;                   // too close
                }
            }
        }
        return true;                                // all nearby chunks are wild
    }

}
