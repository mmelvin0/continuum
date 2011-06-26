package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin {

	final double RADIUS = 10.0;
	final int DEPTH = 64;

	Logger log = Logger.getLogger("Minecraft");
	BukkitScheduler scheduler;
	PluginManager pm;
	Player player;
	Plugin self;
	Block fire;

	public void onEnable() {
		self = this;
		scheduler = getServer().getScheduler();
		pm = getServer().getPluginManager();
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
					scheduler.scheduleSyncDelayedTask(self, new Runnable() {
						public void run() {
							fire = null;
							player = null;
						}
					});
				}
			}
		}, Priority.Monitor, this);
		pm.registerEvent(Type.BLOCK_IGNITE, new BlockListener() {
			public void onBlockIgnite(BlockIgniteEvent event) {
				if (
					fire != null && player != null && !event.isCancelled() &&
					player.equals(event.getPlayer()) && 
					IgniteCause.FLINT_AND_STEEL.equals(event.getCause())
				) {
					traverse(fire, player);
				}
				fire = null;
				player = null;
			}
		}, Priority.Monitor, this);
		log.info(getDescription().getFullName() + " enabled");
	}

	public void onDisable() {
		log.info(getDescription().getFullName() + " disabled");
	}

	/**
	 * @todo Decide which direction to traverse first based on direction player is facing.
	 * @param fire Block set ablaze to begin traversal.
	 * @param player Player that started the fire.
	 */
	void traverse(Block fire, Player player) {
		player.sendMessage("yaw: " + player.getLocation().getYaw());
		List<Block> rim = new ArrayList<Block>();
		List<Block> air = new ArrayList<Block>();
		Block keystone = fire.getFace(BlockFace.DOWN);
		if (isKeystone(keystone) && isInside(fire)) {
			rim.add(keystone);
			air.add(fire);
			if (traverse(fire, fire, rim, air, 0, false)) {
				player.sendMessage("yup");
			} else {
				rim.clear();
				air.clear();
				rim.add(keystone);
				air.add(fire);
				if (traverse(fire, fire, rim, air, 0, true)) {
					player.sendMessage("yup");
				} else {
					player.sendMessage("nope");
				}
			}
		}
	}

	/**
	 * @todo Change swap parameter to a pair of vectors describing horizontal and vertical directions.
	 * @param fire Block from which the traversal began. isInside(first) must be true!
	 * @param block Block to traverse. isInside(current) must be true!
	 * @param rim Known border blocks.
	 * @param air Known inside blocks.
	 * @param depth Current recursion depth.
	 * @param swap Whether to swap x and z (change horizontal direction).
	 * @return Whether the shape is valid.
	 */
	boolean traverse(Block fire, Block block, List<Block> rim, List<Block> air, int depth, boolean swap) {
		if (depth > DEPTH) {
//			log.info("Max search depth (" + DEPTH + ") exceeded");
			return false;
		}
		if (block.getLocation().distance(fire.getLocation()) > RADIUS) {
//			log.info("Max search radius (" + RADIUS + ") exceeded");
			return false;
		}
		World world = block.getWorld();
		List<Block> interests = new ArrayList<Block>();
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				// don't traverse diagonally
				if (Math.abs(x) == Math.abs(y)) {
					continue;
				}
				Block next = world.getBlockAt(block.getLocation().toVector().add(new Vector(swap ? 0 : x, y, swap ? x : 0)).toLocation(world));
				if (air.contains(next) || rim.contains(next)) {
					// don't revisit blocks we've already seen
					continue;
				} else if (isBorder(next)) {
					rim.add(next);
				} else if (isInside(next)) {
					interests.add(next);
					air.add(next);
				} else {
					// not a border or inside block, so this invalidates the search
					return false;
				}
			}
		}
		for (Block interest : interests) {
			depth++;
			if (!traverse(fire, interest, rim, air, depth, swap)) {
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
		return block.getType().equals(Material.COBBLESTONE);
	}

}
