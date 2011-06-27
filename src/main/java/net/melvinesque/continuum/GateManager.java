package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class GateManager {

	List<Gate> gates = new ArrayList<Gate>();
	Map<Gate, IntegrityCheck> integrity = new HashMap<Gate, IntegrityCheck>();
	BukkitScheduler scheduler;
	PluginManager pm;
	Main plugin;
	Player player;

	GateManager(Main main) {
		plugin = main;
	}

	void enable() {
		pm = plugin.getServer().getPluginManager();
		scheduler = plugin.getServer().getScheduler();
		BlockListener listener = new BlockListener() {
			public void onBlockBreak(BlockBreakEvent event) {
				integrity(event, event);
			}
			public void onBlockBurn(BlockBurnEvent event) {
				integrity(event, event);
			}
			public void onBlockRedstoneChange(BlockRedstoneEvent event) {
				redstone(event);
			}
			public void onSignChange(SignChangeEvent event) {
				sign(event);
			}
		};
		pm.registerEvent(Type.BLOCK_BREAK, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.BLOCK_BURN, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.REDSTONE_CHANGE, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.SIGN_CHANGE, listener, Priority.Monitor, plugin);
	}

	void disable() {}

	void add(Gate gate, Player player) {
		gates.add(gate);
		this.player = player;
		player.sendMessage(gates.size() + " gates");
	}
	
	void remove(Gate gate) {
		player.sendMessage("removing gate " + describe(gate));
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
	
	void integrity(Cancellable c, BlockEvent b) {
		if (c.isCancelled()) {
			return;
		}
		Gate gate = getGateAt(b.getBlock().getLocation());
		if (gate == null || integrity.containsKey(gate)) {
			return;
		}
		IntegrityCheck check = new IntegrityCheck(gate);
		scheduler.scheduleSyncDelayedTask(plugin, check);
		integrity.put(gate, check);
	}
	
	void config(Gate gate, String[] lines) {
		if (lines.length > 0) {
			String name = lines[0].trim();
			if (name.length() > 0) {
				player.sendMessage("gate " + describe(gate) + " name " + name);
				gate.setName(name);
			}
		}
		if (lines.length > 1) {
			String destination = lines[1].trim();
			if (destination.length() > 1) {
				player.sendMessage("gate " + describe(gate) + " destination " + destination);
				gate.setDestination(destination);
			}
		}
	}
	
	String describe(Gate gate) {
		String name = gate.getName();
		if (name.length() > 0) {
			return name;
		}
		return Integer.toString(gates.indexOf(gate)); 
	}
	
	void redstone(BlockRedstoneEvent e) {}

	void sign(SignChangeEvent e) {
		Block sign = e.getBlock();
		if (sign.getState() instanceof org.bukkit.block.Sign) {
			org.bukkit.block.Sign state = (org.bukkit.block.Sign)sign.getState();
			if (state.getData() instanceof org.bukkit.material.Sign) {
				org.bukkit.material.Sign data = (org.bukkit.material.Sign)state.getData();
				Block block = sign.getFace(data.getAttachedFace());
				Gate gate = getGateAt(block.getLocation());
				if (gate != null && gate.isOutside(block)) {
					config(gate, e.getLines());
				}
			}
		}
	}
	
	class IntegrityCheck implements Runnable {

		Gate gate;

		IntegrityCheck(Gate gate) {
			this.gate = gate;
		}

		public void run() {
			if (!gate.intact()) {
				remove(gate);
			}
			integrity.remove(gate);
		}

	}

}
