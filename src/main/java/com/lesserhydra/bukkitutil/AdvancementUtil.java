package com.lesserhydra.bukkitutil;

import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public class AdvancementUtil {
	
	public static void giveAdvancement(Player player, Advancement advancement) {
		AdvancementProgress progress = player.getAdvancementProgress(advancement);
		progress.getRemainingCriteria().forEach(progress::awardCriteria);
	}
	
}
