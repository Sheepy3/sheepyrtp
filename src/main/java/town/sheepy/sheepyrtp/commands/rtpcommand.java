package town.sheepy.sheepyrtp.commands;

import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;

import java.util.Set;

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
        boolean no_structure = true;
        Player player = (Player) sender;
        World world = player.getWorld();
        int RADIUS = 500;
        if(args.length >0) {
            try {
                RADIUS = Integer.parseInt(args[0]);
            }catch(Exception e){
                player.sendMessage("argument provided must be an integer.");
                return true;
            }
        }

        Location target;
        int x,y,z;
        Block surface, ground;
        final int MAX_TRIES = 10_000;
        int tries = 0;
        find_position: do {
            no_structure = true;
            if (++tries > MAX_TRIES) {
                sender.sendMessage("§cCouldn’t find a safe spot, try again later.");
                return true;          // bail out gracefully
            }


            x = ThreadLocalRandom.current().nextInt(-RADIUS, RADIUS + 1);
            z = ThreadLocalRandom.current().nextInt(-RADIUS, RADIUS + 1);
            y = world.getHighestBlockYAt(x, z);
            surface = world.getBlockAt(x,y,z);
            Chunk chunk = surface.getChunk();

            if (surface.isLiquid()){
                ground = surface;
                continue;
            }

            Collection<GeneratedStructure> structures = chunk.getStructures(Structure.VILLAGE_DESERT);
            Set<Structure> blacklisted_structures = Set.of(
                    Structure.VILLAGE_DESERT,
                    Structure.VILLAGE_PLAINS,
                    Structure.VILLAGE_SAVANNA,
                    Structure.VILLAGE_SNOWY,
                    Structure.VILLAGE_TAIGA,
                    Structure.IGLOO,
                    Structure.MANSION,
                    Structure.MONUMENT,
                    Structure.SWAMP_HUT,
                    Structure.PILLAGER_OUTPOST
            );
            for (Structure structure : blacklisted_structures) {
                if (!chunk.getStructures(structure).isEmpty()) {
                    no_structure = false;

                }
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
            !isFarFromTowns(world,ground.getChunk().getX(),ground.getChunk().getZ(),15) ||
            !no_structure
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
