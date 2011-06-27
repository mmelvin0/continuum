package net.melvinesque.continuum;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	Logger log = Logger.getLogger("Minecraft");
	GateSearch search;

	public void onEnable() {
		search = new GateSearch(this);
		search.enable();
		log.info(getDescription().getFullName() + " enabled");
	}

	public void onDisable() {
		search.disable();
		search = null;
		log.info(getDescription().getFullName() + " disabled");
	}

}
