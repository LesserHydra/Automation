package com.lesserhydra.automation;

import org.bukkit.plugin.java.JavaPlugin;
import com.lesserhydra.automation.activator.ActivatorListener;

public class Automation extends JavaPlugin {
	
	private static Automation instance;
	
	private final AutocrafterListener autocrafterModule = new AutocrafterListener(this);
	private final ActivatorListener activatorListener = new ActivatorListener();
	private final BlockCartListener blockCartModule = new BlockCartListener();
	private final PearlModule pearlModule = new PearlModule(this);
	
	
	@Override
	public void onEnable() {
		instance = this;
		
		getServer().getPluginManager().registerEvents(new HopperFilterListener(), this);
		getServer().getPluginManager().registerEvents(new PulserListener(), this);
		getServer().getPluginManager().registerEvents(activatorListener, this);
		getServer().getPluginManager().registerEvents(blockCartModule, this);
		getServer().getPluginManager().registerEvents(autocrafterModule, this);
		
		//TODO: Abstract out into a module system
		activatorListener.init();
		blockCartModule.init();
		pearlModule.init();
		autocrafterModule.init();
		
		//TODO: Stop canceled hoppers from checking on next tick
		//TODO: Need some way to check if a cart is holding a block. Comparator/plate-track combo seems intuitive
		//TODO: Way for upwards item movement (droppers elevator and pulser may suffice)
	}
	
	@Override
	public void onDisable() {
		pearlModule.deinit();
		
		instance = null;
	}
	
	public ActivatorListener getActivatorModule() { return activatorListener; }
	
	public static Automation instance() { return instance; }
	
	public static void log(String msg) {
		instance.getLogger().info(msg);
	}
	
}
