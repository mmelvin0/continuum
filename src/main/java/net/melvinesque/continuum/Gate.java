package net.melvinesque.continuum;

import java.util.ArrayList;
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
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitScheduler;

public class Gate {

	Logger log = Logger.getLogger("Minecraft");

	BlockFace face;
	BukkitScheduler scheduler;
	ChargeMonitor monitor;
	Gate target;
	GateManager manager;
	GatePart parts[][][];
	Location sign;
	Main plugin;
	SignReader reader;
	SignUpdater updater;
	String name;
	Vortex vortex;
	World world;

	int minX, minY, minZ;
	int maxX, maxY, maxZ;
	int lenX, lenY, lenZ;
	boolean power = false;

	private Gate(World world, String name, BlockFace face, Main plugin) {
		this.world = world;
		this.name = name;
		this.face = face;
		this.plugin = plugin;
		this.manager = plugin.getGateManager();
		this.scheduler = plugin.getServer().getScheduler();
		this.monitor = new ChargeMonitor();
		this.reader = new SignReader();
		this.updater = new SignUpdater();
		this.vortex = new Vortex(this, plugin);
	}

	Gate(World world, String name, BlockFace face, Set<Block> fill, Set<Block> ring, Main plugin) {
		this(world, name, face, plugin);
		Set<Location> locations = new HashSet<Location>();
		locations.addAll(getBlockLocations(fill));
		locations.addAll(getBlockLocations(ring));
		calcSize(locations);
		this.parts = new GatePart[lenX][lenY][lenZ];
		for (Location l : locations) {
			Block b = world.getBlockAt(l);
			if (fill.contains(b)) {
				this.parts[l.getBlockX() - minX][l.getBlockY() - minY][l.getBlockZ() - minZ] = new FillPart(b);
			} else if (ring.contains(b)) {
				this.parts[l.getBlockX() - minX][l.getBlockY() - minY][l.getBlockZ() - minZ] = new RingPart(b);
			}
		}
		checkPower();
	}

	Gate(World world, String name, BlockFace face, Set<GatePart> parts, Location sign, Main plugin) {
		this(world, name, face, plugin);
		Map<Location, GatePart> map = getPartMap(parts);
		calcSize(map.keySet());
		this.parts = new GatePart[lenX][lenY][lenZ];
		for (Map.Entry<Location, GatePart> e : map.entrySet()) {
			Location l = e.getKey();
			this.parts[l.getBlockX() - minX][l.getBlockY() - minY][l.getBlockZ() - minZ] = e.getValue(); 
		}
		if (sign != null) {
			setSign(sign);
			updater.now();
		}
		checkPower();
	}

	void deactivate() {
		vortex.deactivate();
	}

	void destroy() {
		deactivate();
		removeSign();
	}

	public String toString() {
		return "Gate(" + name + ")";
	}

	Map<String, Object> getData() {
		// todo: clean this up
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Integer> pos;
		List<List<List<String>>> listX;
		List<List<String>> listY;
		List<String> listZ;
		GatePart part;
		result.put("x", minX);
		result.put("y", minY);
		result.put("z", minZ);
		if (sign == null) {
			result.put("sign", null);
		} else {
			pos = new HashMap<String, Integer>();
			pos.put("x", sign.getBlockX());
			pos.put("y", sign.getBlockY());
			pos.put("z", sign.getBlockZ());
			result.put("sign", pos);
		}
		result.put("face", face.name());
		result.put("target", target == null ? null : target.getName());
		result.put("world", world.getName());
		listX = new ArrayList<List<List<String>>>();
		for (int x = 0; x < lenX; x++) {
			listY = new ArrayList<List<String>>();
			listX.add(listY);
			for (int y = 0; y < lenY; y++) {
				listZ = new ArrayList<String>();
				listY.add(listZ);
				for (int z = 0; z < lenZ; z++) {
					part = parts[x][y][z];
					listZ.add(part instanceof FillPart ? part.getMaterial().name() : null);
				}
			}
		}
		result.put("fill", listX);
		listX = new ArrayList<List<List<String>>>();
		for (int x = 0; x < lenX; x++) {
			listY = new ArrayList<List<String>>();
			listX.add(listY);
			for (int y = 0; y < lenY; y++) {
				listZ = new ArrayList<String>();
				listY.add(listZ);
				for (int z = 0; z < lenZ; z++) {
					part = parts[x][y][z];
					listZ.add(part instanceof RingPart ? part.getMaterial().name() : null);
				}
			}
		}
		result.put("ring", listX);
		return result;
	}


	/* Geometry */

	void calcSize(Set<Location> locations) {
		int x, y, z;
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		minZ = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
		maxZ = Integer.MIN_VALUE;
		for (Location l : locations) {
			x = l.getBlockX();
			y = l.getBlockY();
			z = l.getBlockZ();
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}
		lenX = maxX - minX + 1;
		lenY = maxY - minY + 1;
		lenZ = maxZ - minZ + 1;
	}

	Set<Location> getBlockLocations(Set<Block> blocks) {
		Set<Location> locations = new HashSet<Location>();
		for (Block b : blocks) {
			locations.add(b.getLocation());
		}
		return locations;
	}

	Map<Location, GatePart> getPartMap(Set<GatePart> parts) {
		Map<Location, GatePart> map = new HashMap<Location, GatePart>();
		for (GatePart p : parts) {
			map.put(p.getLocation(), p);
		}
		return map;
	}

	boolean consistsOf(Location l) {
		if (!world.equals(l.getWorld())) {
			return false;
		}
		if (isSignIntact() && sign.equals(l)) {
			return true;
		}
		int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		return ringContains(l);
	}

	boolean isIntact() {
		GatePart part;
		// although sign is checked during integrity check
		// a broken sign doesn't mean a broken gate
		isSignIntact();
		for (int x = 0; x < lenX; x++) {
			for (int y = 0; y < lenY; y++) {
				for (int z = 0; z < lenZ; z++) {
					part = parts[x][y][z];
					if (part != null && !part.isIntact()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	Location getArrival(Gate from, Player player) {
		Location b = world.getBlockAt(getCenter()).getFace(face).getLocation();
		Location p = player.getLocation();
		double modX = lenX % 2 == 1 ? 0.5 : 0.0;
		double modY = lenY % 2 == 1 ? 0.5 : 0.0;
		double modZ = lenZ % 2 == 1 ? 0.5 : 0.0;
		float yaw = p.getYaw() - getYaw(from.face.getOppositeFace()) + getYaw();
		return new Location(world, b.getX() + modX, b.getY() + modY, b.getZ() + modZ, yaw, p.getPitch() + getPitch());  
	}

	Location getCenter() {
		return new Location(world, maxX - ((maxX - minX) / 2), maxY - ((maxY - minY) / 2), maxZ - ((maxZ - minZ) / 2));
	}

	Set<Block> getFill() {
		Set<Block> result = new HashSet<Block>();
		GatePart part;
		for (int x = 0; x < lenX; x++) {
			for (int y = 0; y < lenY; y++) {
				for (int z = 0; z < lenZ; z++) {
					part = parts[x][y][z];
					if (part instanceof FillPart) {
						result.add(part.getBlock());
					}
				}
			}
		}
		return result;
	}

	float getYaw() {
		return getYaw(face);
	}

	float getYaw(BlockFace face) {
		switch (face) {
			case WEST:
				return 0;
			case NORTH:
				return 90;
			case EAST:
				return 180;
			case SOUTH:
				return 270;
			default:
				return 0;
		}
	}

	float getPitch() {
		switch (face) {
			case UP:
				return -90;
			case DOWN:
				return 90;
			default:
				return 0;
		}
	}

	boolean fillContains(Location l) {
		int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		GatePart part = parts[x - minX][y - minY][z - minZ];
		return part instanceof FillPart;
	}

	boolean ringContains(Location l) {
		int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		GatePart part = parts[x - minX][y - minY][z - minZ];
		return part instanceof RingPart ? part.isIntact() : false;
	}


	/* Name & Target */

	String getName() {
		return name;
	}

	void setName(String name) {
		if (name == null || this.name.equals(name) || manager.has(name)) {
			return;
		}
		manager.map.remove(this.name);
		this.name = name;
		manager.map.put(this.name, this);
		if (target != null) {
			target.updater.later();
		}
	}

	Gate getTarget() {
		return target;
	}

	void setTarget(Gate gate) {
		if (this.equals(gate)) {
			// gate can't be its own target
			return;
		}
		target = gate;
		updater.later();
	}


	/* Power */

	void checkPower() {
		boolean p = isPowered();
		if (power != p) {
			power = p;
			monitor.schedule();
		}
	}

	boolean isPowered() {
		for (int x = 0; x < lenX; x++) {
			for (int y = 0; y < lenY; y++) {
				for (int z = 0; z < lenZ; z++) {
					GatePart part = parts[x][y][z];
					if (part instanceof RingPart) {
						Block block = part.getBlock();
						if (block.isBlockPowered()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	class ChargeMonitor implements Runnable {

		final int FIRST_LEVEL_TICK = 6;
		final int TICKS_PER_LEVEL = 2;
		final int MAX_LEVEL = 7;

		boolean scheduled = false;
		long start = 0;

		void schedule() {
			if (start <= 0)  {
				start = world.getFullTime();
			}
			if (!scheduled) {
				scheduler.scheduleSyncDelayedTask(plugin, this);
				scheduled = true;
			}
		}

		void stop() {
			start = 0;
			updater.now();
		}

		public void run() {
			scheduled = false;
			if (!isPowered()) {
				stop();
				return;
			}
			long level = getLevel();
			if (level >= MAX_LEVEL) {
				if (target != null && !vortex.active) {
					vortex.activate();
					stop();
				}
			} else {
				schedule();
			}
			if (level != updater.getLevel()) {
				updater.now();
			}
		}

		long getCharge() {
			return start <= 0 ? 0 : world.getFullTime() - start;
		}

		long getLevel() {
			return Math.max(0, (getCharge() - FIRST_LEVEL_TICK + TICKS_PER_LEVEL) / TICKS_PER_LEVEL);
		}
	}

	/* Signs */

	void attachSign(Location l) {
		if (isIntact() && !isSignIntact()) {
			setSign(l);
			reader.later();
		}
	}

	void dropSign(Location l) {
		if (l == null) {
			return;
		}
		if (ringContains(l)) {
			// just in case someone tries to make a gate out of signs
			return;
		}
		Block b = world.getBlockAt(l);
		MaterialData md = b.getState().getData();
		if (!(md instanceof org.bukkit.material.Sign)) {
			// don't do anything if it isn't a sign
			return;
		}
		if (!ringContains(b.getFace(((org.bukkit.material.Sign)md).getAttachedFace()).getLocation())) {
			// only pay attention to signs attached to the gate's ring
			return;
		}
		// the sign must be "dropped" just like this
		// any other way causes horrendous bugs (infinite sign stacks on the ground, worse)
		b.setType(Material.AIR);
		world.dropItemNaturally(b.getLocation(), new ItemStack(Material.SIGN, 1));
	}

	void dropSigns() {
		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					dropSign(world.getBlockAt(x, y, z).getLocation());
				}
			}
		}
	}

	void removeSign() {
		if (sign == null) {
			return;
		}
		dropSign(sign);
		sign = null;
	}

	boolean isSignIntact() {
		if (sign == null) {
			return false;
		}
		if (isSignIntact(sign)) {
			return true;
		}
		removeSign();
		return false;
	}

	boolean isSignIntact(Location l) {
		if (l == null) {
			return false;
		}
		Block block = world.getBlockAt(l);
		BlockState state = block.getState();
		MaterialData data = state.getData();
		if (
			!(state instanceof org.bukkit.block.Sign) ||
			!(data instanceof org.bukkit.material.Sign)
		) {
			// it's not a sign
			return false;
		}
		org.bukkit.material.Sign material = (org.bukkit.material.Sign)data;
		if (!ringContains(block.getFace(material.getAttachedFace()).getLocation())) {
			// it's not attached to the gate's ring
			return false;
		}
		return true;
	}

	void setSign(Location l) {
		if (l == null) {
			removeSign();
		} else if (isSignIntact(l)) {
			if (sign != null) {
				removeSign();
			}
			sign = l;
		}
	}

	abstract class SignSynchronizer implements Runnable {

		boolean scheduled = false;

		void now() {
			boolean was = scheduled;
			run();
			if (was) {
				later();
			}
		}

		void later() {
			if (!scheduled) {
				scheduler.scheduleSyncDelayedTask(plugin, this);
				scheduled = true;
			}
		}

		public void run() {
			scheduled = false;
		}

	}

	class SignReader extends SignSynchronizer {

		public void run() {
			super.run();
			if (!isSignIntact()) {
				return;
			}
			BlockState bs = world.getBlockAt(sign).getState();
			if (!(bs instanceof org.bukkit.block.Sign)) {
				return;
			}
			org.bukkit.block.Sign sign = (org.bukkit.block.Sign)bs;
			String lines[] = sign.getLines();
			for (int i = 0; i < Math.min(2, lines.length); i++) {
				String line = lines[i].trim();
				if (line.length() == 0) {
					continue;
				}
				if (i == 0) {
					setName(line);
				} else if (i == 1) {
					setTarget(manager.get(line));
				}
			}
			updater.now();
		}

	}

	class SignUpdater extends SignSynchronizer {

		long level = 0;

		public void run() {
			super.run();
			if (!isSignIntact()) {
				return;
			}
			BlockState bs = world.getBlockAt(sign).getState();
			if (!(bs instanceof org.bukkit.block.Sign)) {
				return;
			}
			org.bukkit.block.Sign sign = (org.bukkit.block.Sign)bs;
			level = monitor.getLevel();
			String chevrons = "";
			for (int i = 0; i < level; i++) {
				chevrons += "*";
			}
			setLine(sign, 0, name);
			setLine(sign, 1, chevrons);
			setLine(sign, 2, chevrons);
			setLine(sign, 3, target == null ? "" : target.getName());
			sign.update();
		}

		long getLevel() {
			return level;
		}

		void setLine(org.bukkit.block.Sign sign, int line, String text) {
			if (line < 0 || line > 3) {
				return;
			}
			if (text == null) {
				text = "";
			}
			if (text.length() > 15) {
				text = text.substring(0, 15);
			}
			sign.setLine(line, text);
		}

	}

}
