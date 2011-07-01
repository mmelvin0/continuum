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
		Location location = block.getLocation();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
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
