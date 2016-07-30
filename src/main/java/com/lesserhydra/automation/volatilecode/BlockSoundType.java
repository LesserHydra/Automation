package com.lesserhydra.automation.volatilecode;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_9_R2.util.CraftMagicNumbers;
import com.lesserhydra.bukkitutil.SoundEffect;
import com.lesserhydra.util.MapBuilder;
import net.minecraft.server.v1_9_R2.Block;
import net.minecraft.server.v1_9_R2.SoundEffectType;

public enum BlockSoundType {
	
	WOOD		(1.0F, 1.0F, Sound.BLOCK_WOOD_BREAK, Sound.BLOCK_WOOD_STEP, Sound.BLOCK_WOOD_PLACE, Sound.BLOCK_WOOD_HIT, Sound.BLOCK_WOOD_FALL),
	GRAVEL		(1.0F, 1.0F, Sound.BLOCK_GRAVEL_BREAK, Sound.BLOCK_GRAVEL_STEP, Sound.BLOCK_GRAVEL_PLACE, Sound.BLOCK_GRAVEL_HIT, Sound.BLOCK_GRAVEL_FALL),
	GRASS		(1.0F, 1.0F, Sound.BLOCK_GRASS_BREAK, Sound.BLOCK_GRASS_STEP, Sound.BLOCK_GRASS_PLACE, Sound.BLOCK_GRASS_HIT, Sound.BLOCK_GRASS_FALL),
	STONE		(1.0F, 1.0F, Sound.BLOCK_STONE_BREAK, Sound.BLOCK_STONE_STEP, Sound.BLOCK_STONE_PLACE, Sound.BLOCK_STONE_HIT, Sound.BLOCK_STONE_FALL),
	METAL		(1.0F, 1.5F, Sound.BLOCK_METAL_BREAK, Sound.BLOCK_METAL_STEP, Sound.BLOCK_METAL_PLACE, Sound.BLOCK_METAL_HIT, Sound.BLOCK_METAL_FALL),
	GLASS		(1.0F, 1.0F, Sound.BLOCK_GLASS_BREAK, Sound.BLOCK_GLASS_STEP, Sound.BLOCK_GLASS_PLACE, Sound.BLOCK_GLASS_HIT, Sound.BLOCK_GLASS_FALL),
	CLOTH		(1.0F, 1.0F, Sound.BLOCK_CLOTH_BREAK, Sound.BLOCK_CLOTH_STEP, Sound.BLOCK_CLOTH_PLACE, Sound.BLOCK_CLOTH_HIT, Sound.BLOCK_CLOTH_FALL),
	SAND		(1.0F, 1.0F, Sound.BLOCK_SAND_BREAK, Sound.BLOCK_SAND_STEP, Sound.BLOCK_SAND_PLACE, Sound.BLOCK_SAND_HIT, Sound.BLOCK_SAND_FALL),
	SNOW		(1.0F, 1.0F, Sound.BLOCK_SNOW_BREAK, Sound.BLOCK_SNOW_STEP, Sound.BLOCK_SNOW_PLACE, Sound.BLOCK_SNOW_HIT, Sound.BLOCK_SNOW_FALL),
	LADDER		(1.0F, 1.0F, Sound.BLOCK_LADDER_BREAK, Sound.BLOCK_LADDER_STEP, Sound.BLOCK_LADDER_PLACE, Sound.BLOCK_LADDER_HIT, Sound.BLOCK_LADDER_FALL),
	ANVIL		(0.3F, 1.0F, Sound.BLOCK_ANVIL_BREAK, Sound.BLOCK_ANVIL_STEP, Sound.BLOCK_ANVIL_PLACE, Sound.BLOCK_ANVIL_HIT, Sound.BLOCK_ANVIL_FALL),
	SLIME		(1.0F, 1.0F, Sound.BLOCK_SLIME_BREAK, Sound.BLOCK_SLIME_STEP, Sound.BLOCK_SLIME_PLACE, Sound.BLOCK_SLIME_HIT, Sound.BLOCK_SLIME_FALL);
	
	
	private static final Map<SoundEffectType, BlockSoundType> typeMap =
			MapBuilder.init(() -> new HashMap<SoundEffectType, BlockSoundType>())
			.put(SoundEffectType.a, WOOD)
			.put(SoundEffectType.b, GRAVEL)
			.put(SoundEffectType.c, GRASS)
			.put(SoundEffectType.d, STONE)
			.put(SoundEffectType.e, METAL)
			.put(SoundEffectType.f, GLASS)
			.put(SoundEffectType.g, CLOTH)
			.put(SoundEffectType.h, SAND)
			.put(SoundEffectType.i, SNOW)
			.put(SoundEffectType.j, LADDER)
			.put(SoundEffectType.k, ANVIL)
			.put(SoundEffectType.l, SLIME)
			.buildImmutable();
	
	private final float volume;
	private final float pitch;
	private final Sound breakSound;
	private final Sound stepSound;
	private final Sound placeSound;
	private final Sound hitSound;
	private final Sound fallSound;
	
	
	private BlockSoundType(float volume, float pitch, Sound breakSound, Sound stepSound, Sound placeSound, Sound hitSound, Sound fallSound) {
		this.volume = volume;
		this.pitch = pitch;
		this.breakSound = breakSound;
		this.stepSound = stepSound;
		this.placeSound = placeSound;
		this.hitSound = hitSound;
		this.fallSound = fallSound;
	}
	
	public float getVolume() {
		return volume;
	}
	
	public float getPitch() {
		return pitch;
	}
	
	public SoundEffect getBreakEffect() {
		return new SoundEffect(breakSound, volume, pitch); //TODO: ???
	}
	
	public SoundEffect getStepEffect() {
		return new SoundEffect(stepSound, volume * 0.15F, pitch);
	}
	
	public SoundEffect getPlaceEffect() {
		return new SoundEffect(placeSound, (volume + 1.0F) / 2.0F, pitch * 0.8F);
	}
	
	public SoundEffect getHitEffect() {
		return new SoundEffect(hitSound, volume, pitch); //TODO: ???
	}
	
	public SoundEffect getFallEffect() {
		return new SoundEffect(fallSound, volume * 0.5F, pitch * 0.75F);
	}
	
	public static BlockSoundType fromType(Material blockType) {
		Block block = CraftMagicNumbers.getBlock(blockType);
		return typeMap.get(block.w()); //OBS: Block line 581; step sound getter
	}
	
}
