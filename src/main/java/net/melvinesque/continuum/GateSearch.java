package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

class GateSearch {
	
	final double RADIUS = 10.0;
	final int DEPTH = 64;
	
	Logger log = Logger.getLogger("Minecraft");
	BukkitScheduler scheduler;
	GateManager gates;
	PluginManager pm;
	Main plugin;
	Player player;
	Block fire;
	
	GateSearch(Main main) {
		plugin = main;
	}
	
	void enable() {
		gates = plugin.getGateManager();
		pm = plugin.getServer().getPluginManager();
		scheduler = plugin.getServer().getScheduler();
		pm.registerEvent(Type.PLAYER_INTERACT, new PlayerListener() {
			public void onPlayerInteract(PlayerInteractEvent event) {
				ItemStack item = event.getItem();
				if (
					fire == null && player == null && item != null &&
					!event.isCancelled() && event.hasBlock() &&
					Material.FLINT_AND_STEEL.equals(item.getType()) &&
					Action.RIGHT_CLICK_BLOCK.equals(event.getAction())
				) {
					Block block = event.getClickedBlock();
					if (gates.getGateAt(block.getLocation()) == null) {
						// interested in fire started by player until next server tick
						fire = block.getFace(BlockFace.UP);
						player = event.getPlayer();
						scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
							public void run() {
								fire = null;
								player = null;
							}
						});
					}
				}
			}
		}, Priority.Monitor, plugin);
		pm.registerEvent(Type.BLOCK_IGNITE, new BlockListener() {
			public void onBlockIgnite(BlockIgniteEvent event) {
				if (
					fire != null && player != null && !event.isCancelled() &&
					fire.equals(event.getBlock()) &&
					player.equals(event.getPlayer()) &&
					IgniteCause.FLINT_AND_STEEL.equals(event.getCause())
				) {
					List<Block> inside = new ArrayList<Block>();
					List<Block> outside = new ArrayList<Block>();
					if (search(fire, player, inside, outside)) {
						BlockFace face = facing(outside, player);
						player.sendMessage("facing: " + face.name());
						if (
							face.equals(BlockFace.NORTH) ||
							face.equals(BlockFace.SOUTH) ||
							face.equals(BlockFace.EAST) ||
							face.equals(BlockFace.WEST)
						) {
							// TODO: signs should pop off
							gates.add(new Gate(fire.getWorld(), inside, outside, face), player);
							event.setCancelled(true);
						}
					}
				}
				fire = null;
				player = null;
			}
		}, Priority.Highest, plugin);
	}
	
	void disable() {
		fire = null;
		player = null;
		gates = null;
		pm = null;
		scheduler = null;
		plugin = null;
	}
	
	BlockFace facing(List<Block> outside, Player player) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		double x, y, z;
		Location l;
		for (Block b : outside) {
			l = b.getLocation();
			x = l.getX();
			y = l.getY();
			z = l.getZ();
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}
		player.sendMessage("min/max x: " + minX + ".." + maxX + " y: " + minY + ".." + maxY + " z: " + minZ + ".." + maxZ);
		l = new Location(player.getWorld(), maxX - ((maxX - minX) / 2), maxY - ((maxY - minY) / 2), maxZ - ((maxZ - minZ) / 2));
		player.sendMessage("center: " + l.getX() + " " + l.getY() + " " + l.getZ());
		l = player.getLocation();
		if (minX == maxX) {
			return l.getX() - .5 < minX ? BlockFace.NORTH : BlockFace.SOUTH;
		} else if (minY == maxY) {
			return l.getY() - .5 < minY ? BlockFace.DOWN : BlockFace.UP;
		} else if (minZ == maxZ) {
			return l.getZ() - .5 < minZ ? BlockFace.EAST : BlockFace.WEST;
		} else {
			return BlockFace.SELF;
		}
	}

	/**
	 * @param burning Block set ablaze to begin traversal.
	 * @param player Player that started the fire.
	 */
	boolean search(Block burning, Player player, List<Block> inside, List<Block> outside) {
		Block keystone = burning.getFace(BlockFace.DOWN);
		Location fire = burning.getLocation();
		List<Vector> horizontals = new ArrayList<Vector>();
		Vector vertical = new Vector(0, 1, 0);
		float yaw = player.getLocation().getYaw();
		if (yaw < 0) {
			yaw += 360;
		}
		// 0 = W, 90 = N, 180 = E, 270 = S
		// determine most likely orientation based on player facing
		player.sendMessage("yaw: " + yaw);
		if ((yaw >= 45 && yaw <= 135) || (yaw >= 225 && yaw <= 315)) {
			horizontals.add(new Vector(0, 0, 1));
			horizontals.add(new Vector(1, 0, 0));
		} else {
			horizontals.add(new Vector(1, 0, 0));
			horizontals.add(new Vector(0, 0, 1));
		}
		if (isKeystone(keystone) && isInside(burning)) {
			for (Vector horizontal : horizontals) {
				inside.clear();
				outside.clear();
				inside.add(burning);
				outside.add(keystone);
				if (search(fire, fire, inside, outside, horizontal, vertical, 0)) {
					return true;
				}
			}
		}
		return false;
	}

	boolean search(Location fire, Location current, List<Block> inside, List<Block> outside, Vector horizontal, Vector vertical, int depth) {
		if (depth > DEPTH) {
			player.sendMessage("Max search depth (" + DEPTH + ") exceeded");
			return false;
		}
		if (current.distance(fire) > RADIUS) {
			player.sendMessage("Max search radius (" + RADIUS + ") exceeded");
			return false;
		}
		List<Location> interests = new ArrayList<Location>();
		World world = current.getWorld();
		Block block;
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				// don't traverse diagonally
				if (Math.abs(x) == Math.abs(y)) {
					continue;
				}
				block = world.getBlockAt(current.toVector()
				                                .add(new Vector(x, x, x).multiply(horizontal))
				                                .add(new Vector(y, y, y).multiply(vertical))
				                                .toLocation(world));
				if (inside.contains(block) || outside.contains(block)) {
					// don't revisit blocks we've already seen
					continue;
				} else if (isInside(block)) {
					interests.add(block.getLocation());
					inside.add(block);
				} else if (isOutside(block)) {
					outside.add(block);
				} else {
					// not an outside or inside block, invalidate search
					return false;
				}
			}
		}
		for (Location next : interests) {
			depth++;
			if (!search(fire, next, inside, outside, horizontal, vertical, depth)) {
				return false;
			}
			depth--;
		}
		return true;
	}

	boolean isInside(Block block) {
		return block.getType().equals(Material.AIR);
	}

	boolean isOutside(Block block) {
		return !block.getType().equals(Material.AIR);
	}

	boolean isKeystone(Block block) {
		Material m = block.getType();
		return (
			m.equals(Material.COBBLESTONE) ||
			m.equals(Material.DIAMOND_BLOCK) ||
			m.equals(Material.GOLD_BLOCK) ||
			m.equals(Material.LOG) ||
			m.equals(Material.NETHERRACK)
		);
	}

}
