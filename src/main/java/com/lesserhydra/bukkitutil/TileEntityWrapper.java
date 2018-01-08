package com.lesserhydra.bukkitutil;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.inventory.meta.BlockStateMeta;


public interface TileEntityWrapper {
  
  void copyTo(BlockStateMeta block);
  
  //void copyTo(Banner block);
  //void copyTo(Beacon block);
  //void copyTo(BrewingStand block);
  //void copyTo(Chest block);
  //void copyTo(CommandBlock block);
  void copyTo(CreatureSpawner block);
  //void copyTo(Dispenser block);
  //void copyTo(Dropper block);
  //void copyTo(Furnace block);
  //void copyTo(ShulkerBox block);
  
  void copyTo(SpawnerMinecart block);
  
}
