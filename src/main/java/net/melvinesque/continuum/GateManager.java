package net.melvinesque.continuum;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

public class GateManager {

	List<Gate> gates = new ArrayList<Gate>();
	Main plugin;

	/**
	 * @todo Watch for block destruction and ensure gates are intact
	 * @todo Watch for signs attached to gates
	 */
	GateManager(Main main) {
		plugin = main;
	}

	void enable() {}

	void disable() {}

	void add(Gate gate, Player player) {
		gates.add(gate);
	}

}
