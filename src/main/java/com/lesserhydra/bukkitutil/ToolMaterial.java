package com.lesserhydra.bukkitutil;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

public enum ToolMaterial {
	
	NONE	(-1),
	WOOD	(0, Material.WOOD_SWORD, Material.WOOD_AXE, Material.WOOD_HOE, Material.WOOD_PICKAXE, Material.WOOD_SPADE),
	GOLD	(0, Material.GOLD_SWORD, Material.GOLD_AXE, Material.GOLD_HOE, Material.GOLD_PICKAXE, Material.GOLD_SPADE),
	STONE	(1, Material.STONE_SWORD, Material.STONE_AXE, Material.STONE_HOE, Material.STONE_PICKAXE, Material.STONE_SPADE),
	IRON	(2, Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_HOE, Material.IRON_PICKAXE, Material.IRON_SPADE),
	DIAMOND	(3, Material.DIAMOND_SWORD, Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SPADE);
	
	
	private final int level;
	private final Material[] toolTypes;
	private static Map<Material, ToolMaterial> typeMap = new HashMap<>();
	
	private ToolMaterial(int level, Material...toolTypes) {
		this.level = level;
		this.toolTypes = toolTypes;
	}
	
	static {
		for (ToolMaterial toolMaterial : values()) {
			for (Material type : toolMaterial.getToolTypes()) {
				typeMap.put(type, toolMaterial);
			}
		}
	}
	
	public static ToolMaterial fromType(Material toolType) {
		ToolMaterial result = typeMap.get(toolType);
		return (result == null ? NONE : result);
	}
	
	public int getMiningLevel() {
		return level;
	}
	
	public Material[] getToolTypes() {
		return toolTypes;
	}
	
	public int compareMaterial(ToolMaterial other) {
		return this.getMiningLevel() - other.getMiningLevel();
	}

}
