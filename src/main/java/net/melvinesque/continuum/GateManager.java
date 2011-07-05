package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class GateManager {

	Logger log = Logger.getLogger("Minecraft");

	BukkitScheduler scheduler;
	PluginManager pm;
	Main plugin;

	List<Gate> list = new ArrayList<Gate>();
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
				handleSign(event);
			}
		};
		pm.registerEvent(Type.BLOCK_BREAK, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.BLOCK_BURN, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.REDSTONE_CHANGE, listener, Priority.Monitor, plugin);
		pm.registerEvent(Type.SIGN_CHANGE, listener, Priority.Monitor, plugin);
	}

	void disable() {}

	void add(Gate gate) {
		map.put(gate.getName(), gate);
		if (!list.contains(gate)) {
			gate.dropSigns();
			list.add(gate);
			log.info(gate + " added");
		}
	}

	void create(BlockFace face, List<Block> fill, List<Block> ring) {
		String name;
		int i = 0;
		do {
			name = "Gate" + ++i;
		} while (has(name));
		add(new Gate(name, face, fill, ring, this));
	}

	Gate get(Block block) {
		return get(block.getLocation());
	}

	Gate get(Location location) {
		for (Gate gate : list) {
			if (gate.rectangleContains(location)) {
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
		for (Gate other : list) {
			Gate target = other.getTarget();
			if (gate.equals(target)) {
				other.setTarget(null);
			}
		}
		map.remove(gate.getName());
		list.remove(gate);
		gate.dropSigns();
		log.info(gate + " removed");
	}

	void configure(Gate gate, String[] lines) {
		String text;
		if (lines.length <= 1) {
			return;
		}
		text = lines[0].trim();
		if (text.length() > 0) {
			gate.setName(text);
		}
		if (lines.length < 2) {
			return;
		}
		text = lines[1].trim();
		if (text.length() > 1) {
			Gate target = map.get(text);
			if (target == null) {
				log.info("no such gate: " + text);
				return;
			}
			gate.setTarget(target);
		}
	}

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

	void handleSign(SignChangeEvent e) {
		Block sign = e.getBlock();
		if (sign.getState() instanceof org.bukkit.block.Sign) {
			org.bukkit.block.Sign state = (org.bukkit.block.Sign)sign.getState();
			if (state.getData() instanceof org.bukkit.material.Sign) {
				org.bukkit.material.Sign data = (org.bukkit.material.Sign)state.getData();
				Block block = sign.getFace(data.getAttachedFace());
				Gate gate = get(block);
				if (gate != null && gate.ringContains(block)) {
					configure(gate, e.getLines());
				}
			}
		}
	}

	class Check implements Runnable {

		List<Gate> gates = new ArrayList<Gate>();
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
