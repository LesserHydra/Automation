package com.lesserhydra.bukkitutil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

/**
 * A collection of misc utilities dealing with blocks.
 * @author Justin Lawen
 */
public class BlockUtil {
	
	public static Collection<Entity> getEntitiesInBlock(Block block) {
		return block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5);
	}
	
	/**
	 * Returns the horizontal facings to the right and left of the given forward facing direction.
	 * @param facing The forward facing direction
	 * @return The right and left facing directions
	 */
	public static BlockFace[] getHorizontalSides(BlockFace facing) {
		if (facing == BlockFace.EAST || facing == BlockFace.WEST) return new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH};
		else return new BlockFace[]{BlockFace.EAST, BlockFace.WEST};
	}
	
	/**
	 * Finds all item frames attached to the given block.
	 * @param block The block to check
	 * @return All attached item frames
	 */
	public static List<ItemFrame> getItemFramesOnBlock(Block block) {
		return block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 0.6, 0.6, 0.6).stream()
				.filter(entity -> entity instanceof ItemFrame)
				.map(entity -> (ItemFrame) entity)
				.collect(Collectors.toList());
	}
	
	public static boolean blockCanBeBrokenByTool(Block block, ItemStack tool) {
		ToolMaterial toolMaterial = ToolMaterial.fromType(tool.getType());
		BlockLevel blockLevel = BlockLevel.fromBlockType(block.getType());
		return blockLevel.toolCanMine(toolMaterial);
	}
	
	public static ItemStack silktouch(Block block) {
		return block.getState().getData().toItemStack();
	}
	
}
