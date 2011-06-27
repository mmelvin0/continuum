package net.melvinesque.continuum;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class Gate {

	List<Block> inside;
	List<Block> outside;
	BlockFace face;

	Gate(List<Block> inside, List<Block> outside, BlockFace face) {
		this.inside = inside;
		this.outside = outside;
		this.face = face;
	}

}
