package com.lesserhydra.automation;

import com.lesserhydra.bukkitutil.AdvancementUtil;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.util.SetBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.Hopper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class HopperFilterModule implements Module, Listener {
	
	private final Automation plugin;
	
	private static final Set<Material> durabilityItems = SetBuilder.init(HashSet<Material>::new)
			.add(Material.SHEARS)
			.add(Material.FISHING_ROD)
			.add(Material.CARROT_STICK)
			.add(Material.FLINT_AND_STEEL)
			.add(Material.BOW)
			.add(Material.ELYTRA)
			.add(Material.SHIELD)
			.add(Material.WOOD_SPADE)
			.add(Material.WOOD_PICKAXE)
			.add(Material.WOOD_HOE)
			.add(Material.WOOD_AXE)
			.add(Material.WOOD_SWORD)
			.add(Material.STONE_SPADE)
			.add(Material.STONE_PICKAXE)
			.add(Material.STONE_HOE)
			.add(Material.STONE_AXE)
			.add(Material.STONE_SWORD)
			.add(Material.IRON_SPADE)
			.add(Material.IRON_PICKAXE)
			.add(Material.IRON_HOE)
			.add(Material.IRON_AXE)
			.add(Material.IRON_SWORD)
			.add(Material.GOLD_SPADE)
			.add(Material.GOLD_PICKAXE)
			.add(Material.GOLD_HOE)
			.add(Material.GOLD_AXE)
			.add(Material.GOLD_SWORD)
			.add(Material.DIAMOND_SPADE)
			.add(Material.DIAMOND_PICKAXE)
			.add(Material.DIAMOND_HOE)
			.add(Material.DIAMOND_AXE)
			.add(Material.DIAMOND_SWORD)
			.add(Material.LEATHER_BOOTS)
			.add(Material.LEATHER_LEGGINGS)
			.add(Material.LEATHER_CHESTPLATE)
			.add(Material.LEATHER_HELMET)
			.add(Material.CHAINMAIL_BOOTS)
			.add(Material.CHAINMAIL_LEGGINGS)
			.add(Material.CHAINMAIL_CHESTPLATE)
			.add(Material.CHAINMAIL_HELMET)
			.add(Material.IRON_BOOTS)
			.add(Material.IRON_LEGGINGS)
			.add(Material.IRON_CHESTPLATE)
			.add(Material.IRON_HELMET)
			.add(Material.GOLD_BOOTS)
			.add(Material.GOLD_LEGGINGS)
			.add(Material.GOLD_CHESTPLATE)
			.add(Material.GOLD_HELMET)
			.add(Material.DIAMOND_BOOTS)
			.add(Material.DIAMOND_LEGGINGS)
			.add(Material.DIAMOND_CHESTPLATE)
			.add(Material.DIAMOND_HELMET)
			.buildImmutable();
	
	HopperFilterModule(Automation plugin) { this.plugin = plugin; }
	
	@Override
	public void init() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlaceFilter(HangingPlaceEvent event) {
		if (event.getEntity().getType() != EntityType.ITEM_FRAME) return;
		if (event.getBlock().getType() != Material.HOPPER) return;
		AdvancementUtil.giveAdvancement(event.getPlayer(), Bukkit.getAdvancement(new NamespacedKey(plugin, "placefilter")));
	}
	
	@EventHandler (priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onHopperMove(InventoryMoveItemEvent event) {
		if (!(event.getInitiator().getHolder() instanceof Hopper)) return;
		if (event.getInitiator() != event.getDestination()) return; //Only filter on pull
		Hopper hopper = (Hopper) event.getInitiator().getHolder();
		
		if(!hopperCanPullItem(hopper, event.getItem())) event.setCancelled(true);
	}
	
	private boolean hopperCanPullItem(Hopper hopper, ItemStack item) {
		List<ItemFrame> filters = BlockUtil.getItemFramesOnBlock(hopper.getBlock());
		if (filters.isEmpty()) return true;
		
		final boolean itemHasDurability = durabilityItems.contains(item.getType());
		
		boolean result = false;
		for (ItemFrame frame : filters) {
			boolean blacklist = isFrameUpsidedown(frame.getRotation());
			boolean filterMatch = itemsMatch(frame.getItem(), item, itemHasDurability);
			
			if (blacklist ^ filterMatch) result = true;
			if (blacklist && filterMatch) return false;
		}
		return result;
	}
	
	private static boolean itemsMatch(ItemStack filterItem, ItemStack item, boolean itemHasDurability) {
		return filterItem != null
				&& (item.getType() == filterItem.getType())
				&& (itemHasDurability || item.getDurability() == filterItem.getDurability())
				&& item.hasItemMeta() == filterItem.hasItemMeta()
				&& (!item.hasItemMeta() || Bukkit.getItemFactory().equals(item.getItemMeta(), filterItem.getItemMeta()));
	}
	
	/*private static boolean checkAdvancedFilterLine(String line, ItemStack item) {
		int index = line.indexOf(' ');
		if (index == -1) index = line.length();
		String first = line.substring(0, index);
		String rest = line.substring(index);
		if (first.equalsIgnoreCase("type")) {
			item.getType().toString();
		}
	}
	
	private static boolean checkAdvancedFilter(BookMeta filterMeta, ItemStack item) {
		return filterMeta.getPages().stream()
				.flatMap(page -> Arrays.stream(page.split("\n")))
				.allMatch(line -> checkAdvancedFilterLine(line, item));
	}*/
	
	private static boolean isFrameUpsidedown(Rotation rotation) {
		return rotation.ordinal() >= 3 && rotation.ordinal() <= 5;
	}
	
}
