package net.melvinesque.continuum;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	Logger log = Logger.getLogger("Minecraft");
	GateManager manager;
	GateSearch search;

	public void onEnable() {
		manager = new GateManager(this);
		manager.enable();
		search = new GateSearch(this);
		search.enable();
		log.info("[" + getDescription().getFullName() + "] enabled");
	}

	public void onDisable() {
		manager.disable();
		manager = null;
		search.disable();
		search = null;
		log.info(getDescription().getFullName() + " disabled");
	}
	
	GateManager getGateManager() {
		return manager;
	}

}
