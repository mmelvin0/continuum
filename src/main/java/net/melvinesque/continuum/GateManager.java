package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class GateManager {

	List<Gate> gates = new ArrayList<Gate>();
	Map<Gate,Check> checks = new HashMap<Gate,Check>();
	BukkitScheduler scheduler;
	PluginManager pm;
	Main plugin;
	Player player;

	/**
	 * @todo Watch for signs attached to gates
	 */
	GateManager(Main main) {
		plugin = main;
	}

	void enable() {
		pm = plugin.getServer().getPluginManager();
		scheduler = plugin.getServer().getScheduler();
		BlockListener listener = new BlockListener() {
			public void onBlockBreak(BlockBreakEvent event) {
				check(event, event);
			}
			public void onBlockBurn(BlockBurnEvent event) {
				check(event, event);
			}
		};
		pm.registerEvent(Type.BLOCK_BREAK, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.BLOCK_BURN, listener, Priority.Monitor, plugin);
	}

	void disable() {}

	void add(Gate gate, Player player) {
		gates.add(gate);
		this.player = player;
		player.sendMessage(gates.size() + " gates");
	}
	
	void remove(Gate gate) {
		gates.remove(gate);
		player.sendMessage(gates.size() + " gates");
	}
	
	Gate getGateAt(Location location) {
		for (Gate gate : gates) {
			if (gate.contains(location)) {
				return gate;
			}
		}
		return null;
	}
	
	void check(Cancellable c, BlockEvent b) {
		if (c.isCancelled()) {
			return;
		}
		Gate gate = getGateAt(b.getBlock().getLocation());
		if (gate == null || checks.containsKey(gate)) {
			return;
		}
		Check check = new Check(gate);
		scheduler.scheduleSyncDelayedTask(plugin, check);
		checks.put(gate, check);
	}
	
	class Check implements Runnable {

		Gate gate;

		Check(Gate gate) {
			this.gate = gate;
		}

		public void run() {
			if (!gate.intact()) {
				remove(gate);
			}
			checks.remove(gate);
		}

	}

}
