package net.melvinesque.continuum;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

class GateBlock {

	int x;
	int y;
	int z;
	int m;

	GateBlock(Block b) {
		Location l = b.getLocation();
		x = l.getBlockX();
		y = l.getBlockY();
		z = l.getBlockZ();
		m = b.getTypeId();
	}
	
	boolean intact(World world) {
		return world.getBlockAt(x, y, z).getTypeId() == m;
	}
	
}
