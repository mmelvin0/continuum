package net.melvinesque.continuum;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

class FillPart extends GatePart {

	FillPart(Block block) {
		super(block);
	}

	FillPart(World world, int x, int y, int z) {
		super(world, x, y, z);
	}

	boolean isIntact() {
		return true;
	}

	Material getMaterial() {
		return Material.AIR;
	}

}
