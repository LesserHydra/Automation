package com.lesserhydra.automation;

import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Diode;
import com.lesserhydra.automation.volatilecode.BlockBreaking;
import com.lesserhydra.bukkitutil.InventoryUtil;

public class PulserListener implements Listener {
	
	//private final Map<Location, BukkitTask> resetTimers = new HashMap<>();
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onHopperMove(InventoryMoveItemEvent event) {
		if (!(event.getInitiator().getHolder() instanceof Hopper)) return;
		if (event.getInitiator() != event.getSource()) return; //Only pulse on push
		if (!InventoryUtil.inventoryHasRoom(event.getDestination(), event.getItem())) return; //Event still runs if no room in dest
		Hopper hopper = (Hopper) event.getInitiator().getHolder();
		
		pulse(hopper.getBlock());
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBrewingFinish(BrewEvent event) {
		pulse(event.getBlock());
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFurnaceSmelt(FurnaceSmeltEvent event) {
		pulse(event.getBlock());
	}
	
	/*@EventHandler (priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void cancelPrematurePowerLoss(BlockRedstoneEvent event) {
		if (event.getBlock().getType() != Material.DIODE_BLOCK_ON) return;
		if (event.getOldCurrent() != 15) return;
		
		if (resetTimers.containsKey(event.getBlock().getLocation())) event.setNewCurrent(15);
	}*/
	
	private void pulse(Block pulserBlock) {
		Stream.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)
				.map(pulserBlock::getRelative)
				.filter(sideBlock -> sideBlock.getType() == Material.DIODE_BLOCK_OFF || sideBlock.getType() == Material.DIODE_BLOCK_ON)
				.filter(sideBlock -> pulserBlock.getRelative(((Diode) sideBlock.getState().getData()).getFacing()).equals(sideBlock))
				.forEach(BlockBreaking::activateRepeater);
	}
	
	/*private void activateDiode(Block block) {
		BukkitTask resetTimer = resetTimers.get(block.getLocation());
		if (resetTimer != null) resetTimer.cancel();
		else BlockBreaking.activateRepeater(block);
		
		resetTimer = Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> {
				resetTimers.remove(block.getLocation());
				//BlockBreaking.deactivateRepeater(block); //TODO: See if redstone update can be sent instead
				BlockBreaking.redstoneUpdate(block);
			}, ((Diode) block.getState().getData()).getDelay() * 2 + 1);
		resetTimers.put(block.getLocation(), resetTimer);
	}*/
	
}
