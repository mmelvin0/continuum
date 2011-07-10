package net.melvinesque.continuum;

import java.util.logging.Logger;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;


public class Vortex {

	Logger log = Logger.getLogger("Minecraft");
	
	BukkitScheduler scheduler;
	Gate gate;
	Main plugin;
	boolean active;

	Vortex(Gate gate, Main plugin) {
		this(gate, false, plugin);
		scheduler = plugin.getServer().getScheduler();
		PluginManager pm = plugin.getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_MOVE, new EventHorizon(), Priority.Highest, plugin);
		pm.registerEvent(Type.BLOCK_FROMTO, new FlowControl(), Priority.Highest, plugin);
	}

	Vortex(Gate gate, boolean active, Main plugin) {
		this.gate = gate;
		this.active = active;
		this.plugin = plugin;
	}

	void activate() {
		if (!active) {
			for (Block block : gate.getFill()) {
				block.setType(Material.STATIONARY_WATER);
			}
			scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					deactivate();
				}
			}, 20 * 38);
			active = true;
		}
	}

	void deactivate() {
		if (active) {
			for (Block block : gate.getFill()) {
				block.setType(Material.AIR);
			}
			active = false;
		}
	}

	class EventHorizon extends PlayerListener {

		public void onPlayerMove(PlayerMoveEvent event) {
			if (active && gate.target != null && gate.fillContains(event.getTo())) {
				Player player = event.getPlayer();
				Location to = gate.target.getArrivalLocation(player);
				if (player.teleport(to)) {
					World world = to.getWorld();
					Chunk chunk = world.getChunkAt(to);
					world.refreshChunk(chunk.getX(), chunk.getZ());
					event.setFrom(to);
					event.setTo(to);
					event.setCancelled(true);					
				}
			}
		}

	}

	class FlowControl extends BlockListener {

		public void onBlockFromTo(BlockFromToEvent event) {
			if (active && gate.getFill().contains(event.getBlock())) {
				event.setCancelled(true);
			}
		}

	}

}
