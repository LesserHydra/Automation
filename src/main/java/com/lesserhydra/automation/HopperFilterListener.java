package com.lesserhydra.automation;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import com.lesserhydra.bukkitutil.BlockUtil;

class HopperFilterListener implements Listener {
	
	@EventHandler (priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onHopperMove(InventoryMoveItemEvent event) {
		if (!(event.getInitiator().getHolder() instanceof Hopper)) return;
		if (event.getInitiator() != event.getDestination()) return; //Only filter on pull
		Hopper hopper = (Hopper) event.getInitiator().getHolder();
		
		if(!hopperCanPullItem(hopper, event.getItem().getType())) event.setCancelled(true);
	}
	
	private boolean hopperCanPullItem(Hopper hopper, Material itemType) {
		List<ItemFrame> filters = BlockUtil.getItemFramesOnBlock(hopper.getBlock());
		if (filters.isEmpty()) return true;
		return filters.stream()
				.anyMatch(frame -> frame.getItem().getType() == itemType);
	}

}
