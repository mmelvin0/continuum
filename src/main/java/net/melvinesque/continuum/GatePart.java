package net.melvinesque.continuum;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

abstract class GatePart {

	World world;
	int x, y, z;

	GatePart(Block block) {
		this(block.getWorld(), block.getLocation());
	}

	GatePart(World world, Location l) {
		this(world, l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}

	GatePart(World world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	boolean isIntact() {
		return world.getBlockAt(x, y, z).getType().equals(getMaterial());
	}

	Block getBlock() {
		return world.getBlockAt(x, y, z);
	}

	Location getLocation() {
		return new Location(world, x, y, z);
	}

	abstract Material getMaterial();

}
