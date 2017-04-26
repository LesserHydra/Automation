package com.lesserhydra.automation.volatilecode;

import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockDirectional;
import net.minecraft.server.v1_11_R1.BlockDispenser;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.EntityEnderPearl;
import net.minecraft.server.v1_11_R1.EnumDirection;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.IPosition;
import net.minecraft.server.v1_11_R1.Item;
import net.minecraft.server.v1_11_R1.ItemStack;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.SourceBlock;
import net.minecraft.server.v1_11_R1.TileEntityDispenser;
import net.minecraft.server.v1_11_R1.World;
import org.bukkit.block.Dispenser;
import org.bukkit.craftbukkit.v1_11_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_11_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EnderPearl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class BlockBreaking {
	
	private static Method BLOCK_GET_SILKTOUCH;
	
	static {
		try {
			//OBF: getSilktouch(IBlockData) Line 692, gets block as silktouched item
			BLOCK_GET_SILKTOUCH = Block.class.getDeclaredMethod("w", IBlockData.class);
			BLOCK_GET_SILKTOUCH.setAccessible(true);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	public static String getCustomTag(org.bukkit.inventory.ItemStack item, String key) {
		ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound compound = nmsItem.getTag();
		if (compound == null) return "";
		return compound.getString(key);
	}
	
	public static org.bukkit.inventory.ItemStack setCustomTag(org.bukkit.inventory.ItemStack item, String key, String value) {
		ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound compound = nmsItem.getTag();
		if (compound == null) compound = new NBTTagCompound();
		compound.setString(key, value);
		nmsItem.setTag(compound);
		
		return CraftItemStack.asBukkitCopy(nmsItem);
	}
	
	public static org.bukkit.inventory.ItemStack removeCustomTag(org.bukkit.inventory.ItemStack item, String key) {
		ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound compound = nmsItem.getTag();
		if (compound == null) return item;
		compound.remove(key);
		if (compound.isEmpty()) nmsItem.setTag(null);
		
		return CraftItemStack.asBukkitCopy(nmsItem);
	}
	
	public static EnderPearl launchPearl(Dispenser source) {
		TileEntityDispenser dispenserBlock = ((CraftDispenser)source).getTileEntity();
		World world = dispenserBlock.getWorld();
		EntityEnderPearl pearl = new EntityEnderPearl(world);
		SourceBlock isourceblock = new SourceBlock(dispenserBlock.getWorld(), dispenserBlock.getPosition());
		IPosition iposition = BlockDispenser.a(isourceblock); //OBF: Line 186, gets position in front of dispenser
		EnumDirection enumdirection = isourceblock.e().get(BlockDirectional.FACING);
		
		pearl.setPosition(iposition.getX(), iposition.getY(), iposition.getZ());
		pearl.shoot((double) enumdirection.getAdjacentX(), (double) ((float) enumdirection.getAdjacentY() + 0.1F), (double) enumdirection.getAdjacentZ(), 1.1F, 6.0F);
		world.addEntity(pearl);
		
		return (EnderPearl) pearl.getBukkitEntity();
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isUnbreakable(org.bukkit.block.Block bukkitBlock) {
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		IBlockData blockData = ((CraftChunk)bukkitBlock.getChunk()).getHandle().getBlockData(new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));
		World world = ((CraftWorld)bukkitBlock.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
		
		return (block.a(blockData, world, blockPos) < 0); //OBF: Line 455, checks block strength (set to -1 if unbreakable)
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isInstantlyBreakable(org.bukkit.block.Block bukkitBlock) {
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		IBlockData blockData = ((CraftChunk)bukkitBlock.getChunk()).getHandle().getBlockData(new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));
		World world = ((CraftWorld)bukkitBlock.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
		
		return (block.a(blockData, world, blockPos) == 0); //OBF: Line 455, checks block strength
	}
	
	public static Collection<org.bukkit.inventory.ItemStack> getDrops(org.bukkit.block.Block bukkitBlock, org.bukkit.inventory.ItemStack tool) {
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		World world = ((CraftWorld)bukkitBlock.getWorld()).getHandle();
		IBlockData blockData = ((CraftChunk)bukkitBlock.getChunk()).getHandle().getBlockData(new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));//world.c();
		
		//Silk touch
		if (tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.SILK_TOUCH) > 0 && isSilkTouchable(block)) {
			return Collections.singletonList(CraftItemStack.asBukkitCopy(silkTouch(block, blockData)));
		}
		
		int fortuneLevel = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOT_BONUS_BLOCKS);
		Collection<org.bukkit.entity.Entity> before = bukkitBlock.getWorld().getNearbyEntities(bukkitBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5);
		block.dropNaturally(world, new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()), blockData, 1.0F, fortuneLevel);
		Collection<org.bukkit.entity.Entity> after = bukkitBlock.getWorld().getNearbyEntities(bukkitBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5);
		after.removeAll(before);
		
		//Get itemstacks
		Collection<org.bukkit.inventory.ItemStack> results = after.stream()
				.filter(entity -> entity instanceof org.bukkit.entity.Item)
				.map(itemEntity -> ((org.bukkit.entity.Item)itemEntity).getItemStack())
				.collect(Collectors.toList());
		
		//Remove items
		//DEBUG
		after.stream()
				.filter(entity -> entity instanceof org.bukkit.entity.Item)
				.forEach(org.bukkit.entity.Entity::remove);
		
		return results;
	}

	private static boolean isSilkTouchable(Block block) {
		//OBF: isSilktouchable() Line 732, checks if block can be silktouched (false for spawners and the like)
		return block.o();
	}

	private static ItemStack silkTouch(Block block, IBlockData data) {
		try {
			return (ItemStack) BLOCK_GET_SILKTOUCH.invoke(block, data);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return new ItemStack((Item)null);
		}
	}
	
}
