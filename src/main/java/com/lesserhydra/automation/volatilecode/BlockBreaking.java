package com.lesserhydra.automation.volatilecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.bukkit.block.Dispenser;
import org.bukkit.craftbukkit.v1_10_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EnderPearl;
import org.bukkit.material.Diode;
import com.lesserhydra.bukkitutil.RedstoneUtil;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockDispenser;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.EntityEnderPearl;
import net.minecraft.server.v1_10_R1.EnumDirection;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.IPosition;
import net.minecraft.server.v1_10_R1.ItemStack;
import net.minecraft.server.v1_10_R1.NBTTagCompound;
import net.minecraft.server.v1_10_R1.SourceBlock;
import net.minecraft.server.v1_10_R1.TileEntityDispenser;
import net.minecraft.server.v1_10_R1.World;

public class BlockBreaking {
	
	private static Method BLOCK_SILKTOUCHABLE;
	private static Method BLOCK_GET_SILKTOUCH;
	
	static {
		try {
			//OBF: isSilktouchable()
			BLOCK_SILKTOUCHABLE = Block.class.getDeclaredMethod("o");
			BLOCK_SILKTOUCHABLE.setAccessible(true);
			//OBF: getSilktouch(IBlockData)
			BLOCK_GET_SILKTOUCH = Block.class.getDeclaredMethod("u", IBlockData.class);
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
		IPosition iposition = BlockDispenser.a(isourceblock);
		EnumDirection enumdirection = BlockDispenser.e(isourceblock.f());
		
		pearl.setPosition(iposition.getX(), iposition.getY(), iposition.getZ());
		pearl.shoot((double) enumdirection.getAdjacentX(), (double) ((float) enumdirection.getAdjacentY() + 0.1F), (double) enumdirection.getAdjacentZ(), 1.1F, 6.0F);
		world.addEntity(pearl);
		
		return (EnderPearl) pearl.getBukkitEntity();
	}
	
	public static void activateRepeater(org.bukkit.block.Block diodeBlock) {
		if (RedstoneUtil.diodeIsLocked(diodeBlock)) return;
		
		org.bukkit.material.Diode diode = (Diode) diodeBlock.getState().getData();
		Block block = CraftMagicNumbers.getBlock(diodeBlock.getType());
		World world = ((CraftWorld)diodeBlock.getWorld()).getHandle();
		BlockPosition blockposition = new BlockPosition(diodeBlock.getX(), diodeBlock.getY(), diodeBlock.getZ());
		
		world.a(blockposition, block, diode.getDelay() * 2, -1); //OBF: Update diode state
	}
	
	@SuppressWarnings("deprecation")
	public static void redstoneUpdate(org.bukkit.block.Block diodeBlock) {
		Block block = CraftMagicNumbers.getBlock(diodeBlock.getType());
		World world = ((CraftWorld)diodeBlock.getWorld()).getHandle();
		BlockPosition blockposition = new BlockPosition(diodeBlock.getX(), diodeBlock.getY(), diodeBlock.getZ());
		IBlockData blockData = ((CraftChunk)diodeBlock.getChunk()).getHandle().getBlockData(blockposition);
		
		block.a(blockData, world, blockposition, block); //OBF: Redstone update
		//world.update(blockposition, block);
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isUnbreakable(org.bukkit.block.Block bukkitBlock) {
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		IBlockData blockData = ((CraftChunk)bukkitBlock.getChunk()).getHandle().getBlockData(new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));
		World world = ((CraftWorld)bukkitBlock.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
		
		return (block.b(blockData, world, blockPos) < 0); //OBF: Line 225, checks block strength (set to -1 if unbreakable)
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isInstantlyBreakable(org.bukkit.block.Block bukkitBlock) {
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		IBlockData blockData = ((CraftChunk)bukkitBlock.getChunk()).getHandle().getBlockData(new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));
		World world = ((CraftWorld)bukkitBlock.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
		
		return (block.b(blockData, world, blockPos) == 0); //OBF: Line 225, checks block strength
	}
	
	public static Collection<org.bukkit.inventory.ItemStack> getDrops(org.bukkit.block.Block bukkitBlock, org.bukkit.inventory.ItemStack tool) {
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		World world = ((CraftWorld)bukkitBlock.getWorld()).getHandle();
		IBlockData blockData = ((CraftChunk)bukkitBlock.getChunk()).getHandle().getBlockData(new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));//world.c();
		
		//Silk touch
		if (tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.SILK_TOUCH) > 0 && isSilkTouchable(block)) {
			return Arrays.asList(CraftItemStack.asBukkitCopy(silkTouch(block, blockData)));
		}
		
		int fortuneLevel = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOT_BONUS_BLOCKS);
		Collection<org.bukkit.entity.Entity> before = bukkitBlock.getWorld().getNearbyEntities(bukkitBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5);
		block.dropNaturally(world, new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()), blockData, 1.0F, fortuneLevel);
		Collection<org.bukkit.entity.Entity> after = bukkitBlock.getWorld().getNearbyEntities(bukkitBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5);
		after.removeAll(before);
		after.forEach(org.bukkit.entity.Entity::remove);
		return after.stream()
				.filter(entity -> entity instanceof org.bukkit.entity.Item)
				.map(itemEntity -> ((org.bukkit.entity.Item)itemEntity).getItemStack())
				.collect(Collectors.toList());
	}

	private static boolean isSilkTouchable(Block block) {
		try {
			return (boolean) BLOCK_SILKTOUCHABLE.invoke(block);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static ItemStack silkTouch(Block block, IBlockData data) {
		try {
			return (ItemStack) BLOCK_GET_SILKTOUCH.invoke(block, data);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
