package net.melvinesque.continuum;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitScheduler;

public class Gate {

	Logger log = Logger.getLogger("Minecraft");

	BlockFace face;
	BukkitScheduler scheduler;
	Gate target;
	GateManager manager;
	GatePart parts[][][];
	Location sign1, sign2;
	Main plugin;
	String name;
	World world;

	int minX, minY, minZ;
	int maxX, maxY, maxZ;
	int lenX, lenY, lenZ;
	boolean power;

	Gate(String name, BlockFace face, List<Block> fill, List<Block> ring, Main plugin) {
		Location location;
		int x, y, z;
		this.name = name;
		this.face = face;
		this.plugin = plugin;
		manager = plugin.getGateManager();
		scheduler = plugin.getServer().getScheduler();
		power = false;
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		minZ = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
		maxZ = Integer.MIN_VALUE;
		for (Block block : ring) {
			location = block.getLocation();
			x = location.getBlockX();
			y = location.getBlockY();
			z = location.getBlockZ();
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
		parts = new GatePart[lenX][lenY][lenZ];
		for (Block block : ring) {
			if (world == null) {
				world = block.getWorld();
			}
			location = block.getLocation();
			parts[location.getBlockX() - minX][location.getBlockY() - minY][location.getBlockZ() - minZ] = new RingPart(block);
		}
		for (Block block : fill) {
			if (world == null) {
				world = block.getWorld();
			}
			location = block.getLocation();
			parts[location.getBlockX() - minX][location.getBlockY() - minY][location.getBlockZ() - minZ] = new FillPart(block);
		}
		checkPower(true);
	}

	public String toString() {
		return "Gate<" + name + ">";
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

	boolean consistsOf(Location location) {
		if (!world.equals(location.getWorld())) {
			return false;
		}
		if (sign1 != null && sign1.equals(location)) {
			return true;
		}
		if (sign2 != null && sign2.equals(location)) {
			return true;
		}
		int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		return ringContains(world.getBlockAt(location));
	}

	boolean ringContains(Block block) {
		Location location = block.getLocation();
		int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		// TODO: ensure materials match
		return parts[x - minX][y - minY][z - minZ] instanceof RingPart;
	}

	String getName() {
		return name;
	}

	void setName(String name) {
		if (name == null || this.name.equals(name)) {
			return;
		}
		if (manager.has(name)) {
			log.info("gate already exists: " + name);
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

	void checkPower() {
		checkPower(false);
	}

	void checkPower(boolean initial) {
		boolean power = isPowered();
		if (power != this.power) {
			this.power = power;
			log.info(this + " power = " + power);
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
		scheduler.scheduleSyncDelayedTask(plugin, new SignWriter());
	}

	boolean isSignIntact(Location location) {
		Block block = world.getBlockAt(location);
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

	class SignWriter implements Runnable {

		public void run() {
			if (sign1 != null) {
				if (isSignIntact(sign1)) {
					Block block = world.getBlockAt(sign1);
					BlockState state = block.getState();
					if (state instanceof org.bukkit.block.Sign) {
						org.bukkit.block.Sign sign = (org.bukkit.block.Sign)state;
						sign.setLine(0, name);
						sign.setLine(1, "");
						sign.setLine(2, "");
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
