package net.melvinesque.continuum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.yaml.snakeyaml.Yaml;

public class GateManager {

	final String[] REQUIRED_PROPERTIES = {
		"x",
		"y",
		"z",
		"world",
		"face",
		"fill",
		"ring"
	};

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
		load();
	}

	void disable() {
		for (Gate gate : set) {
			gate.deactivate();
		}
		dump();
		set.clear();
		map.clear();
	}

	void load() {
		log.info("load");
		File file = new File(plugin.getDataFolder().getPath() + File.separator + "gates.yml");
		if (!file.exists()) {
			return;
		}
		log.info("exists");
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return;
			// that's fine, nothing to load...
		}
		log.info("opened");
		Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>)(new Yaml().load(stream));
		log.info("yaml");
		for (Map.Entry<String, Map<String, Object>> e : data.entrySet()) {
			String name = e.getKey();
			Map<String, Object> props = e.getValue();
			if (map.containsKey(name)) {
				log.info("gate already exists: " + name);
				continue;
			}
			boolean valid = true;
			for (String key : REQUIRED_PROPERTIES) {
				if (!props.containsKey(key) || props.get(key) == null) {
					valid = false;
				}
			}
			if (!valid) {
				// missing required properties
				log.info("gate missing required properties: " + name);
				continue;
			}
			String worldname = (String)props.get("world");
			World world = plugin.getServer().getWorld(worldname);
			if (world == null) {
				// no such world
				continue;
			}
			BlockFace face = BlockFace.valueOf(BlockFace.class, (String)props.get("face"));
			if (face == null) {
				// invalid face
				continue;
			}
			int minX = Integer.valueOf(String.valueOf(props.get("x")));
			int minY = Integer.valueOf(String.valueOf(props.get("y")));
			int minZ = Integer.valueOf(String.valueOf(props.get("z")));
			Set<GatePart> parts = new HashSet<GatePart>();
			List<List<List<String>>> listX;
			List<List<String>> listY;
			List<String> listZ;
			listX = (List<List<List<String>>>)props.get("fill");
			for (int x = 0; x < listX.size(); x++) {
				listY = listX.get(x);
				for (int y = 0; y < listY.size(); y++) {
					listZ = listY.get(y);
					for (int z = 0; z < listZ.size(); z++) {
						if ("AIR".equals(listZ.get(z))) {
							parts.add(new FillPart(world, minX + x, minY + y, minZ + z));
						}
					}
				}
			}
			listX = (List<List<List<String>>>)props.get("ring");
			Material material;
			for (int x = 0; x < listX.size(); x++) {
				listY = listX.get(x);
				for (int y = 0; y < listY.size(); y++) {
					listZ = listY.get(y);
					for (int z = 0; z < listZ.size(); z++) {
						material = Material.getMaterial(String.valueOf(listZ.get(z)));
						if (material != null) {
							parts.add(new RingPart(world, minX + x, minY + y, minZ + z, material));
						}
					}
				}
			}
			Location sign = null;
			if (props.get("sign") != null) {
				Map<String, Integer> signprops = (Map<String, Integer>)(props.get("sign"));
				sign = new Location(world, signprops.get("x"), signprops.get("y"), signprops.get("z"));
			}
			add(new Gate(world, name, face, parts, sign, plugin));
		}
	}

	void dump() {
		Map<String, Map<String, Object>> gates = new HashMap<String, Map<String, Object>>();
		Yaml yaml = new Yaml();
		for (Gate gate : set) {
			gates.put(gate.getName(), gate.getData());
		}
		File dir = plugin.getDataFolder();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(dir.getPath() + File.separator + "gates.yml");
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(file));
			yaml.dump(gates, writer);
		} catch (FileNotFoundException e) {
			log.warning("failed to save " + file.getPath() + " (file not found)");
		}
	}

	/* Accessors */

	void add(Gate gate) {
		map.put(gate.getName(), gate);
		if (!set.contains(gate)) {
			gate.dropSigns();
			set.add(gate);
			log.info(gate + " added");
		}
	}

	void create(World world, BlockFace face, Set<Block> fill, Set<Block> ring) {
		String name;
		int i = 0;
		do {
			name = "gate" + ++i;
		} while (has(name));
		add(new Gate(world, name, face, fill, ring, plugin));
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
//		log.info("sign change");
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
//			log.info("redstone");
			for (Gate gate : gates) {
				gate.checkPower();
			}
			super.run();
		}

	}

}
