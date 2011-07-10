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

	final int FIRST_LEVEL_TICK = 14;
	final int TICKS_PER_LEVEL = 4;
	final int MAX_LEVEL = 7;
	final int MAX_CHARGE = (FIRST_LEVEL_TICK + MAX_LEVEL * TICKS_PER_LEVEL) - 1;

	Logger log = Logger.getLogger("Minecraft");

	Activator activator;
	BlockFace face;
	BukkitScheduler scheduler;
	Gate target;
	GateManager manager;
	GatePart parts[][][];
	Location sign1, sign2;
	Main plugin;
	String name;
	Vortex vortex;
	World world;

	int minX, minY, minZ;
	int maxX, maxY, maxZ;
	int lenX, lenY, lenZ;
	int charge = 0;
	boolean power = false;

	Gate(World world, String name, BlockFace face, Main plugin) {
		this.world = world;
		this.name = name;
		this.face = face;
		this.plugin = plugin;
		this.manager = plugin.getGateManager();
		this.scheduler = plugin.getServer().getScheduler();
		this.activator = new Activator();
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
		checkPower(true);
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
		checkPower(true);
	}

	void destroy() {
		vortex.deactivate();
		// should all signs be dropped? or just primary/secondary signs?
		dropSigns();
	}

	public String toString() {
		return "Gate(" + name + ")";
	}

	Map<String, Object> getData() {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Integer> pos;
		List<List<List<String>>> listX;
		List<List<String>> listY;
		List<String> listZ;
		GatePart part;
		result.put("x", minX);
		result.put("y", minY);
		result.put("z", minZ);
		if (sign1 == null) {
			result.put("sign", null);
		} else {
			pos = new HashMap<String, Integer>();
			pos.put("x", sign1.getBlockX());
			pos.put("y", sign1.getBlockY());
			pos.put("z", sign1.getBlockZ());
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

	void calcSizeParts(Set<GatePart> parts) {
		Set<Location> locs = new HashSet<Location>();
		for (GatePart p : parts) {
			locs.add(p.getLocation());
		}
		calcSize(locs);
	}

	boolean consistsOf(Location loc) {
		if (!world.equals(loc.getWorld())) {
			return false;
		}
		if (sign1 != null && sign1.equals(loc)) {
			return true;
		}
		if (sign2 != null && sign2.equals(loc)) {
			return true;
		}
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		return ringContains(world.getBlockAt(loc));
	}

	boolean isIntact() {
		GatePart part;
		if (sign1 != null && !isSignIntact(sign1)){
			setPrimarySign(null);
		}
		if (sign2 != null && !isSignIntact(sign2)) {
			setSecondarySign(null);
		}
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

	Location getArrivalLocation(Player player) {
		// this is all wrong
		float x = (float)maxX - (((float)maxX - (float)minX) / 2) + face.getModX();
		float y = (float)maxY - (((float)maxY - (float)minY) / 2) + face.getModY();
		float z = (float)maxZ - (((float)maxZ- (float)minZ) / 2) + face.getModZ();
		Location p = player.getLocation();
		return new Location(world, x, y, z, p.getYaw(), p.getPitch());
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

	boolean fillContains(Location loc) {
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		GatePart part = parts[x - minX][y - minY][z - minZ];
		return part instanceof FillPart;
	}

	boolean ringContains(Block block) {
		Location loc = block.getLocation();
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
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
		log.info(this + " name = " + name);
		this.name = name;
		manager.map.put(this.name, this);
		if (target != null) {
			target.updateSigns();
		}
	}

	Gate getTarget() {
		return target;
	}

	void setTarget(Gate gate) {
		if (this.equals(gate)) {
			log.info(gate + " cannot be its own target");
			return;
		}
		target = gate;
		log.info(this + " target = " + target);
		updateSigns();
	}


	/* Power */

	void checkPower() {
		checkPower(false);
	}

	void checkPower(boolean initial) {
		boolean power = isPowered();
		if (power != this.power) {
			this.power = power;
			log.info(this + " power = " + power);
			if (power) {
				activator.schedule();
			} else {
				activator.schedule();
			}
		}
	}

	void deactivate() {
		vortex.deactivate();
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

	int getPowerLevel() {
		return Math.max(0, (charge - FIRST_LEVEL_TICK + TICKS_PER_LEVEL) / TICKS_PER_LEVEL);
	}

	class Activator implements Runnable {

		boolean scheduled = false;

		public void run() {
			scheduled = false;
			power = isPowered();
			if (power) {
				if (getPowerLevel() < MAX_LEVEL) {
					charge++;
					updateSigns();
				}
				if (getPowerLevel() == MAX_LEVEL) {
					if (target != null && !vortex.active) {
						vortex.activate();
					}
				} else {
					schedule();
				}
			} else if (charge > 0) {
				charge = 0;
				updateSigns();
			}
			log.info("charge = " + charge + " level = " + getPowerLevel());
		}

		public void schedule() {
			if (!scheduled) {
				scheduler.scheduleSyncDelayedTask(plugin, this);
				scheduled = true;
			}
		}

	}

	/* Signs */
	
	void attachSign(Block sign) {
		isIntact();
		if (sign1 == null) {
			setPrimarySign(sign);
		} else if (sign2 == null) {
			setSecondarySign(sign);
		}
	}

	void dropSigns() {
		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (ringContains(block)) {
						// just in case someone tries to make a gate out of signs
						continue;
					}
					MaterialData data = block.getState().getData();
					if (!(data instanceof org.bukkit.material.Sign)) {
						continue;
					}
					if (!ringContains(block.getFace(((org.bukkit.material.Sign)data).getAttachedFace()))) {
						// only pay attention to signs attached to the gate's ring
						continue;
					}
					// the sign must be "dropped" just like this
					// any other way causes horrendous bugs (infinite sign stacks on the ground, worse)
					block.setType(Material.AIR);
					world.dropItemNaturally(block.getLocation(), new ItemStack(Material.SIGN, 1));
				}
			}
		}
	}

	void updateSigns() {
		scheduler.scheduleSyncDelayedTask(plugin, new SignUpdater());
	}

	boolean isSignIntact(Location loc) {
		Block block = world.getBlockAt(loc);
		BlockState state = block.getState();
		MaterialData data = state.getData();
		if (
			!(state instanceof org.bukkit.block.Sign) ||
			!(data instanceof org.bukkit.material.Sign)
		) {
			// it's not a sign anymore
			return false;
		}
		org.bukkit.material.Sign material = (org.bukkit.material.Sign)data;
		if (!ringContains(block.getFace(material.getAttachedFace()))) {
			// it's not attached to the gate's ring anymore
			return false;
		}
		return true;
	}

	void setPrimarySign(Block sign) {
		if (sign == null) {
			sign1 = null;
		} else {
			sign1 = sign.getLocation();
			scheduler.scheduleSyncDelayedTask(plugin, new SignReader());
		}
	}

	void setSecondarySign(Block sign) {
		if (sign == null) {
			sign2 = null;
		} else {
			sign2 = sign.getLocation();
			updateSigns();
		}
	}

	class SignReader implements Runnable {

		public void run() {
			if (sign1 != null) {
				if (isSignIntact(sign1)) {
					Block block = world.getBlockAt(sign1);
					BlockState state = block.getState();
					if (state instanceof org.bukkit.block.Sign) {
						org.bukkit.block.Sign sign = (org.bukkit.block.Sign)state;
						String lines[] = sign.getLines();
						for (int i = 0; i < lines.length; i++) {
							String line = lines[i].trim();
							if (line.length() == 0) {
								continue;
							}
							switch (i) {
								case 0:
									setName(line);
									break;
								case 1:
									setTarget(manager.get(line));
									break;
							}
						}
					}
				} else {
					setPrimarySign(null);
				}
			}
			updateSigns();
		}

	}

	class SignUpdater implements Runnable {

		public void run() {
			if (sign1 != null) {
				if (isSignIntact(sign1)) {
					log.info("updating sign");
					Block block = world.getBlockAt(sign1);
					BlockState state = block.getState();
					if (state instanceof org.bukkit.block.Sign) {
						org.bukkit.block.Sign sign = (org.bukkit.block.Sign)state;
						sign.setLine(0, name);
						StringBuilder bar = new StringBuilder();
						for (int i = 0; i < getPowerLevel(); i++) {
							bar.append("⌂");
						}
						sign.setLine(1, bar.toString());
						sign.setLine(2, bar.toString());
						sign.setLine(3, target == null ? "" : target.getName());
						sign.update();
					}
				} else {
					setPrimarySign(null);
				}
			}
		}

	}

}
