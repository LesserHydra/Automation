package com.lesserhydra.automation;

import com.lesserhydra.automation.activator.ActivatorModule;
import com.lesserhydra.hydracore.HydraCore;
import com.lesserhydra.util.Version;
import org.bukkit.plugin.java.JavaPlugin;


public class Automation extends JavaPlugin {
  
  private static final int CORE_MAJOR = 1;
  private static final int CORE_MINOR = 0;
  
  private static Automation instance;
  
  private final AutocrafterModule autocrafterModule = new AutocrafterModule(this);
  private final ActivatorModule activatorModule = new ActivatorModule(this);
  private final BlockCartModule blockCartModule = new BlockCartModule(this);
  private final PearlModule pearlModule = new PearlModule(this);
  private final HopperFilterModule hopperFilterModule = new HopperFilterModule(this);
  private final PulserModule pulserModule = new PulserModule(this);
  
  
  @Override
  public void onEnable() {
    assert HydraCore.isLoaded();
    Version.Compat coreCompat = HydraCore.expectVersion(CORE_MAJOR, CORE_MINOR);
    if (coreCompat != Version.Compat.MATCH) {
      if (coreCompat.isOutdated()) {
        getLogger().severe("The loaded version of HydraCore is outdated! Please update to "
            + CORE_MAJOR + "." + CORE_MINOR + "+.");
        //TODO: Link
      }
      else {
        getLogger().severe("The loaded version of HydraCore is incompatible with this " +
            "version of Automation. Please update Automation or downgrade HydraCore to "
            + CORE_MAJOR + "." + CORE_MINOR + "+.");
        //TODO: Links
      }
      
      getPluginLoader().disablePlugin(this);
      return;
    }
    
    instance = this;
    
    //TODO: Seperate modules into plugins
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
    //Skip if enabling failed
    if (instance == null) return;
    
    pulserModule.deinit();
    hopperFilterModule.deinit();
    autocrafterModule.deinit();
    pearlModule.deinit();
    blockCartModule.deinit();
    activatorModule.deinit();
    
    instance = null;
  }
  
  public ActivatorModule getActivatorModule() {
    return activatorModule;
  }
  
  public static Automation instance() {
    return instance;
  }
  
  public static void log(String msg) {
    instance.getLogger().info(msg);
  }
  
}
