package com.lesserhydra.automation;

import org.bukkit.plugin.java.JavaPlugin;
import com.lesserhydra.automation.activator.ActivatorModule;

public class Automation extends JavaPlugin {
	
	private static Automation instance;
	
	private final AutocrafterModule autocrafterModule = new AutocrafterModule(this);
	private final ActivatorModule activatorModule = new ActivatorModule(this);
	private final BlockCartModule blockCartModule = new BlockCartModule(this);
	private final PearlModule pearlModule = new PearlModule(this);
	private final HopperFilterModule hopperFilterModule = new HopperFilterModule(this);
	private final PulserModule pulserModule = new PulserModule(this);
	
	
	@Override
	public void onEnable() {
		instance = this;
		
		//TODO: Seperate modules into plugins with common utility library?
		activatorModule.init();
		blockCartModule.init();
		pearlModule.init();
		autocrafterModule.init();
		hopperFilterModule.init();
		pulserModule.init();
		
		//TODO: Stop canceled hoppers from checking on next tick
		//TODO: Need some way to check if a cart is holding a block. Comparator/plate-track combo seems intuitive
		//TODO: Way for upwards item movement (droppers elevator and pulser may suffice)
	}
	
	@Override
	public void onDisable() {
		pulserModule.deinit();
		hopperFilterModule.deinit();
		autocrafterModule.deinit();
		pearlModule.deinit();
		blockCartModule.deinit();
		activatorModule.deinit();
		
		instance = null;
	}
	
	public ActivatorModule getActivatorModule() { return activatorModule; }
	
	public static Automation instance() { return instance; }
	
	public static void log(String msg) {
		instance.getLogger().info(msg);
	}
	
}
