package com.lesserhydra.automation.volatilecode;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import com.lesserhydra.util.MapBuilder;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.EnumPistonReaction;
import net.minecraft.server.v1_10_R1.Material;

public enum NMSMaterial {
	
	AIR			(Material.AIR),
	GRASS		(Material.GRASS),
	EARTH		(Material.EARTH),
	WOOD		(Material.WOOD),
	STONE		(Material.STONE),
	ORE			(Material.ORE),
	HEAVY		(Material.HEAVY),
	WATER		(Material.WATER),
	LAVA		(Material.LAVA),
	LEAVES		(Material.LEAVES),
	PLANT		(Material.PLANT),
	REPLACEABLE_PLANT (Material.REPLACEABLE_PLANT),
	SPONGE		(Material.SPONGE),
	CLOTH		(Material.CLOTH),
	FIRE		(Material.FIRE),
	SAND		(Material.SAND),
	ORIENTABLE	(Material.ORIENTABLE),
	WOOL		(Material.WOOL),
	SHATTERABLE	(Material.SHATTERABLE),
	BUILDABLE_GLASS	(Material.BUILDABLE_GLASS),
	TNT			(Material.TNT),
	CORAL		(Material.CORAL),
	ICE			(Material.ICE),
	SNOW_LAYER	(Material.SNOW_LAYER),
	PACKED_ICE	(Material.PACKED_ICE),
	SNOW_BLOCK	(Material.SNOW_BLOCK),
	CACTUS		(Material.CACTUS),
	CLAY		(Material.CLAY),
	PUMPKIN		(Material.PUMPKIN),
	DRAGON_EGG	(Material.DRAGON_EGG),
	PORTAL		(Material.PORTAL),
	CAKE		(Material.CAKE),
	WEB			(Material.WEB),
	PISTON		(Material.PISTON),
	BANNER		(Material.BANNER);
	
	private static final Map<EnumPistonReaction, PistonMoveReaction> moveReactionMap =
			MapBuilder.init(() -> new EnumMap<EnumPistonReaction, PistonMoveReaction>(EnumPistonReaction.class))
			.put(EnumPistonReaction.NORMAL, PistonMoveReaction.MOVE)
			.put(EnumPistonReaction.BLOCK, PistonMoveReaction.BLOCK)
			.put(EnumPistonReaction.DESTROY, PistonMoveReaction.BREAK)
			.buildImmutable();
	
	private static final Map<Material, NMSMaterial> typeMap = new HashMap<>();
	static {
		for (NMSMaterial type : values()) {
			typeMap.put(type.material, type);
		}
	}
	
	private final Material material;
	
	
	private NMSMaterial(Material material) {
		this.material = material;
	}
	
	public boolean isLiquid() {
		return material.isLiquid();
	}
	
	public boolean isBuildable() {
		return material.isBuildable();
	}
	
	public boolean blocksLight() {
		return material.blocksLight();
	}
	
	public boolean isSolid() {
		return material.isSolid();
	}
	
	public boolean isBurnable() {
		return material.isBurnable();
	}
	
	public boolean isReplaceable() {
		return material.isReplaceable();
	}
	
	public boolean isAlwaysDestroyable() {
		return material.isAlwaysDestroyable();
	}
	
	/*public boolean k() {
		return material.k();
	}*/
	
	public PistonMoveReaction getPushReaction() {
		return moveReactionMap.get(material.getPushReaction());
	}
	
	@SuppressWarnings("deprecation")
	public static NMSMaterial fromType(org.bukkit.Material blockType) {
		Block block = CraftMagicNumbers.getBlock(blockType);
		return typeMap.get(block.q(block.getBlockData())); //OBF: Get block material
	}
	
}
