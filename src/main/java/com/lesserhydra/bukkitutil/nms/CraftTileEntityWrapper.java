package com.lesserhydra.bukkitutil.nms;

import com.lesserhydra.bukkitutil.TileEntityWrapper;
import net.minecraft.server.v1_12_R1.EntityMinecartMobSpawner;
import net.minecraft.server.v1_12_R1.TileEntity;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftMinecart;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaBlockState;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;


@SuppressWarnings("WeakerAccess")
public class CraftTileEntityWrapper implements TileEntityWrapper {
  
  @NotNull
  private final TileEntity tile;
  
  public CraftTileEntityWrapper(@NotNull TileEntity tile) {
    this.tile = tile;
  }
  
  @NotNull
  public TileEntity getTile() {
    return tile;
  }
  
  @Override
  public void copyTo(BlockStateMeta blockMeta) {
    NMSTileEntityUtil.setTileEntity((CraftMetaBlockState) blockMeta, tile);
  }
  
  @Override
  public void copyTo(CreatureSpawner spawner) {
    NMSTileEntityUtil.setTileEntity((EntityMinecartMobSpawner) spawner, tile);
  }
  
  @Override
  public void copyTo(SpawnerMinecart cart) {
    NMSTileEntityUtil.setTileEntity((EntityMinecartMobSpawner) ((CraftMinecart)cart).getHandle(), tile);
  }
  
}
