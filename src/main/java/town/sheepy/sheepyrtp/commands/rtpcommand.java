package town.sheepy.sheepyrtp.commands;

import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class rtpcommand implements CommandExecutor {
    private final JavaPlugin plugin;
    public rtpcommand(JavaPlugin plugin) { this.plugin = plugin; }

    private static final int    MAX_TRIES      = 10_000;
    private static final int    SAFE_CHUNK_BUF = 15;          // Towny gap
    private static final int    CHUNK_TRIES_PER_TICK = 50;    // main‑thread load

    private static final Set<Material> BAD_SURFACE = Set.of(
            Material.WATER, Material.LAVA,
            Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.KELP, Material.KELP_PLANT
    );
    private static final Set<Material> BAD_GROUND = Set.of(
            Material.WATER, Material.LAVA,
            Material.CACTUS, Material.POWDER_SNOW
    );

    /* ───────────── /rtp handler ───────────── */
    @Override public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        final String whisper = (args.length >= 2) ? args[1] : null;
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Must be a player."); return true;
        }

        int radius = 10_000;              // default
        if (args.length > 0) try { radius = Integer.parseInt(args[0]); }
        catch (NumberFormatException e) { p.sendMessage("§cRadius must be a number."); return true; }

        tryFindAsync(p, radius, 0, whisper);
        return true;
    }

    /* ───────── asynchronous search loop ───────── */
    private void tryFindAsync(Player p, int radius, int attempts, String whisper) {
        if (attempts >= MAX_TRIES) {
            p.sendMessage("§cCouldn't find a safe spot, try again later."); return;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int x = rng.nextInt(-radius, radius + 1);
        int z = rng.nextInt(-radius, radius + 1);
        World w = p.getWorld();
        int cx = x >> 4, cz = z >> 4;                   // chunk coords

        /* A ‑‑ async chunk load */
        w.getChunkAtAsync(cx, cz, true).thenAcceptAsync(chunk -> {
            /* B ‑‑ schedule validation on main */
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (validateChunkSpot(w, x, z, SAFE_CHUNK_BUF)) {
                    Location tp = calcTeleportLocation(w, x, z);
                    p.teleport(tp);
                    if (whisper != null) {
                        String cmd = "msg " + p.getName() + " " + whisper;
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd);
                    }else {
                        p.sendMessage("✈ RTP'd to " + tp.getBlockX() + ", " + tp.getBlockY() + ", " + tp.getBlockZ());
                    }
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> tryFindAsync(p, radius, attempts + 1,whisper),
                            2L);  // recurse / retry
                }
            });
        });
    }

    /* ───────── main‑thread validation ───────── */
    /* 1 ─ validation */
    private boolean validateChunkSpot(World w, int x, int z, int safeChunks) {

        int y = findGroundY(w, x, z);                 // <-- use helper
        Block ground = w.getBlockAt(x, y, z);

        /* reject bad ground */
        if (BAD_GROUND.contains(ground.getType())
                || Tag.LOGS.isTagged(ground.getType()))
            return false;

        /* head‑room */
        if (!w.getBlockAt(x, y + 1, z).isEmpty()) return false;
        if (!w.getBlockAt(x, y + 2, z).isEmpty()) return false;

        /* height range */
        if (y <= 63 || y > 150) return false;

        /* Towny buffer */
        if (!isFarFromTowns(w, ground.getChunk().getX(), ground.getChunk().getZ(), safeChunks))
            return false;

        return true;            // OK!
    }

    /* 2 ─ final teleport location */
    private Location calcTeleportLocation(World w, int x, int z) {
        int y = findGroundY(w, x, z);                 // <-- same helper
        return new Location(w, x + 0.5, y + 1, z + 0.5);
    }

    /* 3 ─ the shared helper */
    private int findGroundY(World w, int x, int z) {
        int y = w.getHighestBlockYAt(x, z);
        Block b = w.getBlockAt(x, y, z);

        while (y > w.getMinHeight()
                && (Tag.LEAVES.isTagged(b.getType())
                || !b.getType().isSolid())) {
            y--;
            b = w.getBlockAt(x, y, z);
        }
        return y;   // first solid, non‑leaf block
    }

    /* ───────── Towny helper (unchanged) ───────── */
    private boolean isFarFromTowns(World w, int cx, int cz, int buf) {
        var api = TownyAPI.getInstance();
        String wn = w.getName();
        for (int dx = -buf; dx <= buf; dx++)
            for (int dz = -buf; dz <= buf; dz++)
                if (!api.isWilderness(new WorldCoord(wn, cx + dx, cz + dz)))
                    return false;
        return true;
    }
}

