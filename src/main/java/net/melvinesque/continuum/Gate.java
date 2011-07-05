package net.melvinesque.continuum;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;

public class Gate {

	Logger log = Logger.getLogger("Minecraft");

	BlockFace face;
	Gate target;
	GateManager manager;
	GatePart parts[][][];
	String name;
	World world;

	int minX, minY, minZ;
	int maxX, maxY, maxZ;
	int lenX, lenY, lenZ;
	boolean power;

	Gate(String name, BlockFace face, List<Block> fill, List<Block> ring, GateManager manager) {
		Location location;
		int x, y, z;
		this.name = name;
		this.face = face;
		this.manager = manager;
		this.power = false;
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

	boolean rectangleContains(Location location) {
		if (!world.equals(location.getWorld())) {
			return false;
		}
		int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		return true;
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
	
	void dropSigns() {
		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (ringContains(block)) {
						continue;
					}
					MaterialData data = block.getState().getData();
					if (!(data instanceof Sign)) {
						continue;
					}
					if (!ringContains(block.getFace(((Sign)data).getAttachedFace()))) {
						continue;
					}
					block.setType(Material.AIR);
					world.dropItemNaturally(block.getLocation(), new ItemStack(Material.SIGN, 1));
				}
			}
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
	}

}
