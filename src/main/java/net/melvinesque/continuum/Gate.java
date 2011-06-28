package net.melvinesque.continuum;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class Gate {

	GateBlock blocks[][][];
	BlockFace face;
	String name = "untitled";
	String destination;
	World world;
	boolean power = false;
	int minX;
	int minY;
	int minZ;
	int maxX;
	int maxY;
	int maxZ;
	int lenX;
	int lenY;
	int lenZ;

	Gate(World world, List<Block> inside, List<Block> outside, BlockFace face) {
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		minZ = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
		maxZ = Integer.MIN_VALUE;
		int x, y, z;
		Location l;
		for (Block b : outside) {
			l = b.getLocation();
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
		blocks = new GateBlock[lenX][lenY][lenZ];
		for (Block b : outside) {
			l = b.getLocation();
			blocks[l.getBlockX() - minX][l.getBlockY() - minY][l.getBlockZ() - minZ] = new OutsideBlock(b);
		}
		for (Block b : inside) {
			l = b.getLocation();
			blocks[l.getBlockX() - minX][l.getBlockY() - minY][l.getBlockZ() - minZ] = new InsideBlock(b);
		}
		this.face = face;
		this.world = world;
	}

	boolean contains(Location l) {
		if (!world.equals(l.getWorld())) {
			return false;
		}
		int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		return true;
	}
	
	boolean intact() {
		for (int x = 0; x < lenX; x++) {
			for (int y = 0; y < lenY; y++) {
				for (int z = 0; z < lenZ; z++) {
					GateBlock b = blocks[x][y][z];
					if (b instanceof OutsideBlock && !b.intact(world)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	boolean isOutside(Block block) {
		Location l = block.getLocation();
		int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return false;
		}
		return blocks[x - minX][y - minY][z - minZ] instanceof OutsideBlock;
	}

	String getName() {
		return name == null ? "" : name;
	}

	void setName(String name) {
		this.name = name;
	}

	String getDestination() {
		return destination;
	}

	void setDestination(String destination) {
		this.destination = destination;
	}

	boolean wasPowered() {
		return power;
	}

	boolean isPowered() {
		for (int x = 0; x < lenX; x++) {
			for (int y = 0; y < lenY; y++) {
				for (int z = 0; z < lenZ; z++) {
					GateBlock g = blocks[x][y][z];
					if (g instanceof OutsideBlock) {
						Block b = g.getBlock(world);
						if (b.isBlockPowered() || b.isBlockIndirectlyPowered()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	void setPower(boolean power) {
		this.power = power;
		Logger.getLogger("Minecraft").info(name + " power " + Boolean.toString(this.power));
	}

}
