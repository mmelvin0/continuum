package net.melvinesque.continuum;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

class RingPart extends GatePart {

	Material material;

	RingPart(Block block) {
		super(block);
		material = block.getType();
	}

	RingPart(World world, int x, int y, int z, Material material) {
		super(world, x, y, z);
		this.material = material;
	}

	Material getMaterial() {
		return material;
	}

}
