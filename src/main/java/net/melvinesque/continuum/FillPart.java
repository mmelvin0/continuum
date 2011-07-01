package net.melvinesque.continuum;

import org.bukkit.block.Block;

class FillPart extends GatePart {

	FillPart(Block block) {
		super(block);
	}

	boolean isIntact() {
		return true;
	}

}
