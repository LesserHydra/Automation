package com.lesserhydra.bukkitutil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
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
	
	/**
	 * Returns a collection of entities intersecting with the given block.
	 * @param block Block to check for intersection with entities
	 * @return Collection of resulting entities
	 */
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
	
	/**
	 * Checks if a given block can be mined by a given tool's material.
	 * Note that this function does not check the actual tool type (shovel vs pick),
	 * but instead looks at the tool "material" (wood vs stone).
	 * @param block Block
	 * @param tool Tool
	 * @return True is tool material can break block material
	 */
	public static boolean blockCanBeBrokenByTool(Block block, ItemStack tool) {
		ToolMaterial toolMaterial = ToolMaterial.fromType(tool.getType());
		BlockLevel blockLevel = BlockLevel.fromBlockType(block.getType());
		return blockLevel.toolCanMine(toolMaterial);
	}
	
	/**
	 * Checks whether a given block is a half slab of any material.
	 * @param block Block to check
	 * @return True if block is a half slab
	 */
	public static boolean isHalfSlab(Block block) {
		Material blockMat = block.getType();
		return (blockMat == Material.STEP || blockMat == Material.WOOD_STEP
				|| blockMat == Material.STONE_SLAB2 || blockMat == Material.PURPUR_SLAB);
	}
	
	/**
	 * Checks what direction a half slab is "facing".
	 * @param slab Half slab block to check
	 * @return Down for bottom slabs, up for top slabs
	 */
	@SuppressWarnings("deprecation")
	public static BlockFace getHalfSlabFace(Block slab) {
		return (slab.getData() < 8 ? BlockFace.DOWN : BlockFace.UP);
	}
	
}
