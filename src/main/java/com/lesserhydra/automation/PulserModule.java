package com.lesserhydra.automation;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.lesserhydra.bukkitutil.AdvancementUtil;
import com.lesserhydra.bukkitutil.RedstoneUtil;
import com.lesserhydra.util.SetBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Diode;
import com.lesserhydra.bukkitutil.InventoryUtil;

class PulserModule implements Module, Listener {
	
	private static final Set<Material> pulsingMaterials = SetBuilder.init(HashSet<Material>::new)
			.add(Material.HOPPER)
			.add(Material.BREWING_STAND)
			.add(Material.FURNACE)
			.add(Material.BURNING_FURNACE)
			.buildImmutable();
	
	private final Automation plugin;
	
	PulserModule(Automation plugin) { this.plugin = plugin; }
	
	@Override
	public void init() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPulserPlace(BlockPlaceEvent event) {
		if (event.getItemInHand().getType() != Material.DIODE) return;
		BlockState state = event.getBlockPlaced().getState();
		if (!(state.getData() instanceof Diode)) return;
		BlockFace facing = ((Diode) state.getData()).getFacing();
		Block pulsingBlock = event.getBlockPlaced().getRelative(facing.getOppositeFace());
		if (!pulsingMaterials.contains(pulsingBlock.getType())) return;
		
		AdvancementUtil.giveAdvancement(event.getPlayer(), Bukkit.getAdvancement(new NamespacedKey(plugin, "placepulser")));
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onHopperMove(InventoryMoveItemEvent event) {
		if (!(event.getInitiator().getHolder() instanceof Hopper)) return;
		if (event.getInitiator() != event.getSource()) return; //Only pulse on push
		if (!InventoryUtil.inventoryHasRoom(event.getDestination(), event.getItem())) return; //Event still runs if no room in dest
		Hopper hopper = (Hopper) event.getInitiator().getHolder();
		
		pulse(hopper.getBlock());
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBrewingFinish(BrewEvent event) { pulse(event.getBlock()); }
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFurnaceSmelt(FurnaceSmeltEvent event) { pulse(event.getBlock()); }
	
	private void pulse(Block pulserBlock) {
		Stream.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)
				.map(pulserBlock::getRelative)
				.filter(sideBlock -> sideBlock.getType() == Material.DIODE_BLOCK_OFF || sideBlock.getType() == Material.DIODE_BLOCK_ON)
				.filter(sideBlock -> pulserBlock.getRelative(((Diode) sideBlock.getState().getData()).getFacing()).equals(sideBlock))
				.forEach(RedstoneUtil::activateRepeater);
	}
	
}
