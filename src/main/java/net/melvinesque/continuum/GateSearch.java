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
	PluginManager pm;
	Main plugin;
	Player player;
	Block fire;
	
	GateSearch(Main main) {
		plugin = main;
		pm = plugin.getServer().getPluginManager();
		scheduler = plugin.getServer().getScheduler();
	}
	
	void enable() {
		pm.registerEvent(Type.PLAYER_INTERACT, new PlayerListener() {
			public void onPlayerInteract(PlayerInteractEvent event) {
				ItemStack item = event.getItem();
				if (
					fire == null && player == null && item != null &&
					!event.isCancelled() && event.hasBlock() &&
					Material.FLINT_AND_STEEL.equals(item.getType()) &&
					Action.RIGHT_CLICK_BLOCK.equals(event.getAction())
				) {
					// interested in fire started by player until next server tick
					fire = event.getClickedBlock().getFace(BlockFace.UP);
					player = event.getPlayer();
					scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
						public void run() {
							fire = null;
							player = null;
						}
					});
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
					if (search(fire, player)) {
						event.setCancelled(true);
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
		pm = null;
		scheduler = null;
		plugin = null;
	}
	
	/**
	 * @todo determine facing of the gate based on player facing
	 * @param burning Block set ablaze to begin traversal.
	 * @param player Player that started the fire.
	 */
	boolean search(Block burning, Player player) {
		Block keystone = burning.getFace(BlockFace.DOWN);
		Location fire = burning.getLocation();
		List<Block> border = new ArrayList<Block>();
		List<Block> inside = new ArrayList<Block>();
		List<Vector> horizontals = new ArrayList<Vector>();
		Vector vertical = new Vector(0, 1, 0);
		float yaw = player.getLocation().getYaw();
		if (yaw < 0) {
			yaw += 360;
		}
		// 0 = W, 90 = N, 180 = E, 270 = S
		// determine most likely orientation based on player facing
		if ((yaw >= 45 && yaw <= 135) || (yaw >= 225 && yaw <= 315)) {
			horizontals.add(new Vector(0, 0, 1));
			horizontals.add(new Vector(1, 0, 0));
		} else {
			horizontals.add(new Vector(1, 0, 0));
			horizontals.add(new Vector(0, 0, 1));
		}
		if (isKeystone(keystone) && isInside(burning)) {
			for (Vector horizontal : horizontals) {
				border.clear();
				inside.clear();
				border.add(keystone);
				inside.add(burning);
				if (search(fire, fire, border, inside, 0, horizontal, vertical)) {
					player.sendMessage("thars a gate!");
					return true;
				}
			}
		}
		return false;
	}

	boolean search(Location fire, Location current, List<Block> border, List<Block> inside, int depth, Vector horizontal, Vector vertical) {
		if (depth > DEPTH) {
			log.info("Max search depth (" + DEPTH + ") exceeded");
			return false;
		}
		if (current.distance(fire) > RADIUS) {
			log.info("Max search radius (" + RADIUS + ") exceeded");
			return false;
		}
		List<Location> search = new ArrayList<Location>();
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
				if (border.contains(block) || inside.contains(block)) {
					// don't revisit blocks we've already seen
					continue;
				} else if (isBorder(block)) {
					border.add(block);
				} else if (isInside(block)) {
					search.add(block.getLocation());
					inside.add(block);
				} else {
					// not a border or filing block, so this invalidates the search
					return false;
				}
			}
		}
		for (Location next : search) {
			depth++;
			if (!search(fire, next, border, inside, depth, horizontal, vertical)) {
				return false;
			}
			depth--;
		}
		return true;
	}

	boolean isBorder(Block block) {
		return !block.getType().equals(Material.AIR);
	}

	boolean isInside(Block block) {
		return block.getType().equals(Material.AIR);
	}

	boolean isKeystone(Block block) {
		return block.getType().equals(Material.COBBLESTONE) || block.getType().equals(Material.DIAMOND_BLOCK) || block.getType().equals(Material.GOLD_BLOCK);
	}
	
}
