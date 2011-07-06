package net.melvinesque.continuum;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

class GatePart {

	Material material;
	World world;
	int x, y, z;

	GatePart(Block block) {
		Location loc = block.getLocation();
		x = loc.getBlockX();
		y = loc.getBlockY();
		z = loc.getBlockZ();
		material = block.getType();
		world = block.getWorld();
	}

	boolean isIntact() {
		return world.getBlockAt(x, y, z).getType().equals(material);
	}

	Block getBlock() {
		return world.getBlockAt(x, y, z);
	}

}
