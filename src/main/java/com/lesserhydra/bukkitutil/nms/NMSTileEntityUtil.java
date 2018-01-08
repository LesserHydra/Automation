package com.lesserhydra.bukkitutil.nms;

import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.EntityMinecartMobSpawner;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.MobSpawnerAbstract;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.TileEntity;
import net.minecraft.server.v1_12_R1.TileEntityMobSpawner;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftMinecart;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaBlockState;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


@SuppressWarnings("WeakerAccess")
public class NMSTileEntityUtil {
  
  private static Field EntityMinecraftMobSpawner_spawner;
  private static Field CraftMetaBlockState_blockEntityTag;
  private static Method Block_getTileEntity;
  
  static {
    try {
      EntityMinecraftMobSpawner_spawner = EntityMinecartMobSpawner.class.getDeclaredField("a"); //OBF: Line 11, MobSpawnerAbstract
      EntityMinecraftMobSpawner_spawner.setAccessible(true);
      CraftMetaBlockState_blockEntityTag = CraftMetaBlockState.class.getDeclaredField("blockEntityTag");
      CraftMetaBlockState_blockEntityTag.setAccessible(true);
      Block_getTileEntity = CraftBlockEntityState.class.getDeclaredMethod("getTileEntity");
      Block_getTileEntity.setAccessible(true);
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      e.printStackTrace();
    }
  }
  
  /*---------------------------------------------------------------------------------------------*/
  
  @Nullable
  public static CraftTileEntityWrapper getTileEntityFrom(org.bukkit.block.BlockState state) {
    if (!(state instanceof CraftBlockEntityState)) return null;
    return new CraftTileEntityWrapper(getTileEntity((CraftBlockEntityState<? extends TileEntity>)state));
  }
  
  @Nullable
  public static CraftTileEntityWrapper getTileEntityFrom(BlockStateMeta meta) {
    NBTTagCompound blockEntityTag;
    try {
      blockEntityTag = (NBTTagCompound) CraftMetaBlockState_blockEntityTag.get(meta);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    TileEntity tile = TileEntity.create(null, blockEntityTag);
    return tile == null ? null : new CraftTileEntityWrapper(tile);
  }
  
  @NotNull
  public static CraftTileEntityWrapper getTileEntityFrom(SpawnerMinecart cart) {
    EntityMinecartMobSpawner entityMinecartMobSpawner = (EntityMinecartMobSpawner) ((CraftMinecart)cart).getHandle();
    MobSpawnerAbstract spawner;
    try {
      spawner = (MobSpawnerAbstract) EntityMinecraftMobSpawner_spawner.get(entityMinecartMobSpawner);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  
    NBTTagCompound spawnerNBT = new NBTTagCompound();
    MinecraftKey mobSpawnerKey = TileEntity.a(TileEntityMobSpawner.class); //OBF: Line 62, lookupKey()
    assert mobSpawnerKey != null;
    spawnerNBT.setString("id", mobSpawnerKey.toString());
    spawnerNBT = spawner.b(spawnerNBT); //OBF: Line 182, saveNBT()
    TileEntity tile = TileEntity.create(null, spawnerNBT);
    assert tile != null;
    return new CraftTileEntityWrapper(tile);
  }
  
  @NotNull
  public static CraftTileEntityWrapper getTileEntityAt(org.bukkit.World world, int x, int y, int z) {
    return new CraftTileEntityWrapper(((CraftWorld) world).getTileEntityAt(x, y, z));
  }
  
  /*---------------------------------------------------------------------------------------------*/
  
  @SuppressWarnings("unchecked")
  @NotNull
  public static <T extends TileEntity> T getTileEntity(CraftBlockEntityState<T> blockEntityState) {
    try {
      return (T) Block_getTileEntity.invoke(blockEntityState);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void setTileEntity(EntityMinecartMobSpawner cart, TileEntity tile) {
    MobSpawnerAbstract spawner;
    try {
      spawner = (MobSpawnerAbstract) EntityMinecraftMobSpawner_spawner.get(cart);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    NBTTagCompound compound = tile.save(new NBTTagCompound());
    spawner.a(tile.save(compound)); //OBF: Line 144, loadNBT()
  }
  
  
  public static void setTileEntity(World nmsWorld, BlockPosition position, TileEntity tile) {
    NBTTagCompound compound = tile.save(new NBTTagCompound());
    compound.setInt("x", position.getX());
    compound.setInt("y", position.getY());
    compound.setInt("z", position.getZ());
    TileEntity destTile = nmsWorld.getTileEntity(position);
    assert destTile != null;
    destTile.load(compound);
    destTile.update();
  }
  
  public static void setTileEntity(CraftMetaBlockState blockMeta, TileEntity tile) {
    NBTTagCompound blockEntityTag = tile.save(new NBTTagCompound());
    blockEntityTag.remove("x");
    blockEntityTag.remove("y");
    blockEntityTag.remove("z");
    try {
      CraftMetaBlockState_blockEntityTag.set(blockMeta, blockEntityTag);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
  
}
