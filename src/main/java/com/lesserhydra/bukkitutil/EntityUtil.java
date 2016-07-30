package com.lesserhydra.bukkitutil;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Cow;
import org.bukkit.entity.MushroomCow;
import org.bukkit.inventory.ItemStack;

public class EntityUtil {
	
	public static Cow shearMushroomCow(MushroomCow mushCow) {
		//Despawn mooshroom
		mushCow.remove();
		
		//Explosions to cover our tracks!
		mushCow.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, mushCow.getLocation().add(0, 0.45, 0), 1);
		
		//Spawn normal cow
		Cow cow = mushCow.getWorld().spawn(mushCow.getLocation(), Cow.class);
		
		//Totally the same cow!
		cow.setHealth(mushCow.getHealth());
		cow.setCustomName(mushCow.getCustomName());
		
		//Sound
		cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_MOOSHROOM_SHEAR, 1.0F, 1.0F);
		
		//Drop 5 mushrooms
		for (int i = 0; i < 5; i++) {
			cow.getWorld().dropItemNaturally(cow.getLocation().add(0, 0.9, 0), new ItemStack(Material.RED_MUSHROOM));
		}
		
		//Return result
		return cow;
	}
	
}
