package net.melvinesque.continuum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class GateManager {

	Logger log = Logger.getLogger("Minecraft");

	BukkitScheduler scheduler;
	PluginManager pm;
	Main plugin;

	Set<Gate> set = new HashSet<Gate>();
	Map<String, Gate> map = new HashMap<String, Gate>();

	IntegrityCheck integrityCheck = new IntegrityCheck();
	RedstoneCheck redstoneCheck = new RedstoneCheck();

	GateManager(Main main) {
		plugin = main;
	}

	void enable() {
		pm = plugin.getServer().getPluginManager();
		scheduler = plugin.getServer().getScheduler();
		BlockListener listener = new BlockListener() {
			public void onBlockBreak(BlockBreakEvent event) {
				handleBreak(event, event);
			}
			public void onBlockBurn(BlockBurnEvent event) {
				handleBreak(event, event);
			}
			public void onBlockRedstoneChange(BlockRedstoneEvent event) {
				handleRedstone(event);
			}
			public void onSignChange(SignChangeEvent event) {
				handleSignChange(event);
			}
		};
		pm.registerEvent(Type.BLOCK_BREAK, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.BLOCK_BURN, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.REDSTONE_CHANGE, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.SIGN_CHANGE, listener, Priority.Monitor, plugin);
	}

	void disable() {}


	/* Accessors */

	void add(Gate gate) {
		map.put(gate.getName(), gate);
		if (!set.contains(gate)) {
			gate.dropSigns();
			set.add(gate);
			log.info(gate + " added");
		}
	}

	void create(BlockFace face, Set<Block> fill, Set<Block> ring) {
		String name;
		int i = 0;
		do {
			name = "Gate" + ++i;
		} while (has(name));
		add(new Gate(name, face, fill, ring, plugin));
	}

	Gate get(Block block) {
		return get(block.getLocation());
	}

	Gate get(Location loc) {
		for (Gate gate : set) {
			if (gate.consistsOf(loc)) {
				return gate;
			}
		}
		return null;
	}

	Gate get(String name) {
		return map.get(name);
	}

	boolean has(String name) {
		return map.containsKey(name);
	}

	void remove(Gate gate) {
		for (Gate other : set) {
			Gate target = other.getTarget();
			if (gate.equals(target)) {
				other.setTarget(null);
			}
		}
		map.remove(gate.getName());
		set.remove(gate);
		gate.destroy();
		log.info(gate + " removed");
	}


	/* Events */

	void handleBreak(Cancellable c, BlockEvent b) {
		if (!c.isCancelled()) {
			integrityCheck.add(get(b.getBlock()));
		}
	}

	void handleRedstone(BlockRedstoneEvent event) {
		Block block = event.getBlock();
		BlockFace[] faces = {
			BlockFace.UP,
			BlockFace.DOWN,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST
		};
		for (BlockFace face : faces) {
			redstoneCheck.add(get(block.getFace(face)));
		}
	}

	void handleSignChange(SignChangeEvent e) {
		Block sign = e.getBlock();
		MaterialData data = sign.getState().getData();
		if (data instanceof Sign) {
			Block part = sign.getFace(((Sign)data).getAttachedFace());
			Gate gate = get(part);
			if (gate != null && gate.ringContains(part)) {
				gate.attachSign(sign);
			}
		}
	}

	/* Tasks */

	class Check implements Runnable {

		Set<Gate> gates = new HashSet<Gate>();
		boolean scheduled = false;

		void add(Gate gate) {
			if (gate != null && !gates.contains(gate)) {
				gates.add(gate);
			}
			if (!scheduled && !gates.isEmpty()) {
				scheduler.scheduleSyncDelayedTask(plugin, this);
				scheduled = true;
			}
		}

		public void run() {
			gates.clear();
			scheduled = false;
		}

	}

	class IntegrityCheck extends Check {

		public void run() {
			for (Gate gate : gates) {
				if (!gate.isIntact()) {
					remove(gate);
				}
			}
			super.run();
		}

	}

	class RedstoneCheck extends Check {

		public void run() {
			for (Gate gate : gates) {
				gate.checkPower();
			}
			super.run();
		}

	}

}
