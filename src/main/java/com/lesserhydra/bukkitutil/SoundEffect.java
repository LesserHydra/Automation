package com.lesserhydra.bukkitutil;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundEffect {
	
	private final Sound sound;
	private final float volume;
	private final float pitch;
	
	public SoundEffect(Sound sound, float volume, float pitch) {
		this.sound = sound;
		this.volume = volume;
		this.pitch = pitch;
	}
	
	public void play(Location location) {
		location.getWorld().playSound(location, sound, volume, pitch);
	}
	
	public void play(Location location, Player player) {
		player.playSound(location, sound, volume, pitch);
	}
	
	public Sound getSound() {
		return sound;
	}
	
	public float getVolume() {
		return volume;
	}
	
	public float getPitch() {
		return pitch;
	}
	
}
