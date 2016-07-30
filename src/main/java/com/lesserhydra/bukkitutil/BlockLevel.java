package com.lesserhydra.bukkitutil;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

public enum BlockLevel {
	
	ANY		(ToolMaterial.NONE),
	WOOD	(ToolMaterial.WOOD),
	STONE	(ToolMaterial.STONE, Material.IRON_ORE, Material.IRON_BLOCK, Material.LAPIS_ORE, Material.LAPIS_BLOCK),
	IRON	(ToolMaterial.IRON, Material.GOLD_ORE, Material.GOLD_BLOCK, Material.EMERALD_ORE, Material.EMERALD_BLOCK, Material.DIAMOND_ORE,
			Material.DIAMOND_BLOCK, Material.REDSTONE_ORE, Material.GLOWING_REDSTONE_ORE, Material.REDSTONE_BLOCK),
	DIAMOND	(ToolMaterial.DIAMOND, Material.OBSIDIAN);
	
	
	private final ToolMaterial required;
	private final Material[] blockTypes;
	private static Map<Material, BlockLevel> typeMap = new HashMap<>();
	
	private BlockLevel(ToolMaterial required, Material...blockTypes) {
		this.required = required;
		this.blockTypes = blockTypes;
	}
	
	static {
		for (BlockLevel blockLevel : values()) {
			for (Material type : blockLevel.getBlockTypes()) {
				typeMap.put(type, blockLevel);
			}
		}
	}
	
	public static BlockLevel fromBlockType(Material blockType) {
		BlockLevel result = typeMap.get(blockType);
		return (result == null ? ANY : result);
	}
	
	public ToolMaterial getRequiredToolMaterial() {
		return required;
	}
	
	public Material[] getBlockTypes() {
		return blockTypes;
	}
	
	public boolean toolCanMine(ToolMaterial toolMaterial) {
		return (toolMaterial.compareMaterial(required) >= 0);
	}
	
}
