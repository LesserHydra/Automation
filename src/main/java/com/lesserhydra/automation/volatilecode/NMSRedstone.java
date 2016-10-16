package com.lesserhydra.automation.volatilecode;

import com.lesserhydra.bukkitutil.RedstoneUtil;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.World;
import org.bukkit.craftbukkit.v1_10_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.material.Diode;

public class NMSRedstone {
	
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
	
}
