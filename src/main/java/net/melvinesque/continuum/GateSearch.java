package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	PluginManager pm;
	Main plugin;
	GateManager manager;
	Block fire;
	Player player;

	GateSearch(Main main) {
		plugin = main;
	}

	void enable() {
		manager = plugin.getGateManager();
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
					if (manager.get(block) == null) {
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
					Set<Block> fill = new HashSet<Block>();
					Set<Block> ring = new HashSet<Block>();
					if (search(fire, fill, ring, player)) {
						BlockFace face = facing(ring, player);
						log.info("facing: " + face.name());
						if (
							face.equals(BlockFace.NORTH) ||
							face.equals(BlockFace.SOUTH) ||
							face.equals(BlockFace.EAST) ||
							face.equals(BlockFace.WEST)
						) {
							manager.create(face, fill, ring);
							event.setCancelled(true);
						}
					}
				}
				fire = null;
				player = null;
			}
		}, Priority.Highest, plugin);
	}
	
	void disable() {}
	
	BlockFace facing(Set<Block> ring, Player player) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		double x, y, z;
		Location loc;
		for (Block block : ring) {
			loc = block.getLocation();
			x = loc.getX();
			y = loc.getY();
			z = loc.getZ();
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}
		log.info("min/max x: " + minX + ".." + maxX + " y: " + minY + ".." + maxY + " z: " + minZ + ".." + maxZ);
		loc = new Location(player.getWorld(), maxX - ((maxX - minX) / 2), maxY - ((maxY - minY) / 2), maxZ - ((maxZ - minZ) / 2));
		log.info("center: " + loc.getX() + " " + loc.getY() + " " + loc.getZ());
		loc = player.getLocation();
		if (minX == maxX) {
			return loc.getX() - .5 < minX ? BlockFace.NORTH : BlockFace.SOUTH;
		} else if (minY == maxY) {
			return loc.getY() - .5 < minY ? BlockFace.DOWN : BlockFace.UP;
		} else if (minZ == maxZ) {
			return loc.getZ() - .5 < minZ ? BlockFace.EAST : BlockFace.WEST;
		} else {
			return BlockFace.SELF;
		}
	}

	boolean search(Block fire, Set<Block> fill, Set<Block> ring, Player player) {
		Block keystone = fire.getFace(BlockFace.DOWN);
		Location loc = fire.getLocation();
		List<Vector> horizontals = new ArrayList<Vector>();
		Vector vertical = new Vector(0, 1, 0);
		float yaw = player.getLocation().getYaw();
		if (yaw < 0) {
			yaw += 360;
		}
		// 0 = W, 90 = N, 180 = E, 270 = S
		// determine most likely orientation based on player facing
		log.info("yaw: " + yaw);
		if ((yaw >= 45 && yaw <= 135) || (yaw >= 225 && yaw <= 315)) {
			horizontals.add(new Vector(0, 0, 1));
			horizontals.add(new Vector(1, 0, 0));
		} else {
			horizontals.add(new Vector(1, 0, 0));
			horizontals.add(new Vector(0, 0, 1));
		}
		if (isKeystone(keystone) && isFill(fire)) {
			for (Vector horizontal : horizontals) {
				fill.clear();
				ring.clear();
				fill.add(fire);
				ring.add(keystone);
				if (search(loc, loc, fill, ring, horizontal, vertical, 0)) {
					return true;
				}
			}
		}
		return false;
	}

	boolean search(Location fire, Location current, Set<Block> fill, Set<Block> ring, Vector horizontal, Vector vertical, int depth) {
		if (depth > DEPTH) {
			log.info("Max search depth (" + DEPTH + ") exceeded");
			return false;
		}
		if (current.distance(fire) > RADIUS) {
			log.info("Max search radius (" + RADIUS + ") exceeded");
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
				if (fill.contains(block) || ring.contains(block)) {
					// don't revisit blocks we've already seen
					continue;
				} else if (isFill(block)) {
					interests.add(block.getLocation());
					fill.add(block);
				} else if (isRing(block)) {
					ring.add(block);
				} else {
					// not a fill or ring block, invalidate search
					return false;
				}
			}
		}
		for (Location next : interests) {
			depth++;
			if (!search(fire, next, fill, ring, horizontal, vertical, depth)) {
				return false;
			}
			depth--;
		}
		return true;
	}

	boolean isKeystone(Block block) {
		Material matieral = block.getType();
		return (
			matieral.equals(Material.COBBLESTONE) ||
			matieral.equals(Material.DIAMOND_BLOCK) ||
			matieral.equals(Material.IRON_BLOCK) ||
			matieral.equals(Material.GOLD_BLOCK) ||
			matieral.equals(Material.LOG) ||
			matieral.equals(Material.NETHERRACK)
		);
	}

	boolean isFill(Block block) {
		Material material = block.getType();
		return (
			material.equals(Material.AIR) ||
			material.equals(Material.SIGN_POST) ||
			material.equals(Material.WALL_SIGN)
		);
	}

	boolean isRing(Block block) {
		return !isFill(block);
	}

}
