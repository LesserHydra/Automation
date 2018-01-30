package com.lesserhydra.automation;

import com.google.common.collect.Streams;
import com.lesserhydra.automation.activator.DispenserInteraction;
import com.lesserhydra.automation.activator.Priority;
import com.lesserhydra.bukkitutil.AdvancementUtil;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.bukkitutil.CraftingGrid;
import com.lesserhydra.bukkitutil.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

class AutocrafterModule implements Module, Listener {
	
	private final Automation plugin;
	
	AutocrafterModule(Automation plugin) { this.plugin = plugin; }
	
	@Override
	public void init() {
		@SuppressWarnings("deprecation")
		MaterialData whiteGlassData = new MaterialData(Material.STAINED_GLASS_PANE, DyeColor.WHITE.getWoolData());
		@SuppressWarnings("deprecation")
		MaterialData blackGlassData = new MaterialData(Material.STAINED_GLASS_PANE, DyeColor.BLACK.getWoolData());
		Bukkit.getServer().addRecipe(new ShapelessRecipe(new NamespacedKey(plugin, "filter"), getFillerItem()).addIngredient(whiteGlassData));
		Bukkit.getServer().addRecipe(new ShapelessRecipe(new NamespacedKey(plugin, "blocker"), getBlockerItem()).addIngredient(blackGlassData));
		
		plugin.getActivatorModule().registerHandler(this::handleAutocrafting, Priority.OVERRIDE);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMakeAutocrafter(BlockPlaceEvent event) {
		if (!didMakeAutocrafter(event.getBlockPlaced())) return;
		AdvancementUtil.giveAdvancement(event.getPlayer(), Bukkit.getAdvancement(new NamespacedKey(plugin, "makecrafter")));
	}
	
	private boolean didMakeAutocrafter(Block placed) {
		if (BlockUtil.isHalfSlab(placed)) {
			BlockFace slabFace = BlockUtil.getHalfSlabFace(placed);
			Block other = placed.getRelative(slabFace);
			
			if (other.getType() != Material.DISPENSER) return false;
			Dispenser dispenser = (Dispenser) other.getState();
			BlockFace dispFace = ((Directional)dispenser.getData()).getFacing();
			return dispFace == slabFace.getOppositeFace();
		}
		else if (placed.getType() == Material.DISPENSER) {
			Dispenser dispenser = (Dispenser) placed.getState();
			BlockFace dispFace = ((Directional)dispenser.getData()).getFacing();
			Block other = placed.getRelative(dispFace);
			
			if (!BlockUtil.isHalfSlab(other)) return false;
			BlockFace slabFace = BlockUtil.getHalfSlabFace(other);
			return dispFace == slabFace.getOppositeFace();
		}
		else return false;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAutocrafterInventoryMove(InventoryMoveItemEvent event) {
		if (event instanceof InventoryMoveItemAutocrafterEvent) return;
		if (event.getDestination().getLocation().getBlock().getType() != Material.DISPENSER) return;
		
		if (!isAutocrafter((Dispenser)event.getDestination().getHolder())) return;
		
		ItemStack item = event.getItem();
		Inventory source = event.getSource();
		Inventory destination = event.getDestination();
		
		//Cancel behavior, rerun event
		event.setCancelled(true);
		
		//Move item
		Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> {
			int firstEmpty = destination.firstEmpty();
			if (firstEmpty < 0) return;
			
			InventoryMoveItemAutocrafterEvent newEvent = new InventoryMoveItemAutocrafterEvent(source, item, destination);
			Bukkit.getPluginManager().callEvent(newEvent);
			if (newEvent.isCancelled()) return;
			
			destination.setItem(firstEmpty, item);
			source.removeItem(item);
		}, 0L);
	}
	
	private boolean handleAutocrafting(DispenserInteraction interaction) {
		if (!isAutocrafter(interaction.getDispenser())) return false;
		interaction.validate();
		interaction.setKeepItem(true);
		
		Dispenser dispenser = interaction.getDispenser();
		replaceItem(dispenser, interaction.getItem());
		
		ItemStack[] fillerContents = new ItemStack[9];
		ItemStack[] remainingContents = new ItemStack[9];
		ItemStack[] craftingContents = new ItemStack[9];
		
		ItemStack[] originalDispenserContents = dispenser.getInventory().getContents();
		for (int i = 0; i < 9; ++i) {
			fillerContents[i] = new ItemStack(Material.AIR);
			remainingContents[i] = new ItemStack(Material.AIR);
			craftingContents[i] = new ItemStack(Material.AIR);
			
			ItemStack item = originalDispenserContents[i];
			if (item == null) continue;
			if (itemIsBlocker(item)) remainingContents[i] = item;
			else if (itemIsFiller(item)) fillerContents[i] = item;
			else craftingContents[i] = item;
		}
		
		//Do crafting
		CraftingGrid result = InventoryUtil.craft(new CraftingGrid(craftingContents), dispenser.getWorld());
		
		Collection<ItemStack> toDrop = new ArrayList<>(10);
		
		//Handle success
		if (result.isSuccess()) {
			toDrop.add(result.getResult());
			dispenser.getWorld().playSound(dispenser.getLocation(), Sound.BLOCK_PISTON_EXTEND, 0.8F, 2F);
			
			Location partLoc = interaction.getFacingBlock().getLocation().add(0.5, 0.5, 0.5);
			dispenser.getWorld().spawnParticle(Particle.CLOUD, partLoc, 20, 0.2, 0.1, 0.2, 0.05);
		} else {
			dispenser.getWorld().playSound(dispenser.getLocation(), Sound.BLOCK_WOOD_BUTTON_CLICK_OFF, 0.8F, 0.7F);
		}
		
		//Drop result and remaining items
		Streams.concat(Arrays.stream(result.getContents()), Arrays.stream(fillerContents))
				.filter(item -> item.getType() != Material.AIR)
				.forEach(toDrop::add);
		
		BlockFace facing = ((Directional) dispenser.getData()).getFacing();
		Block dropBlock = dispenser.getBlock().getRelative(facing, 2);
		if (dropBlock.getState() instanceof InventoryHolder) {
			InventoryHolder dest = (InventoryHolder) dropBlock.getState();
			HashMap<Integer, ItemStack> remaining = dest.getInventory().addItem(toDrop.toArray(new ItemStack[0]));
			toDrop = remaining.values();
		}
		
		for (ItemStack item: toDrop) {
			Location dropLocation = dispenser.getLocation().add(0.5 + facing.getModX()/1.3, 0.5 - 0.15 + facing.getModY()*33.0/26.0, 0.5 + facing.getModZ()/1.3);
			Item itemEntity = dispenser.getWorld().dropItem(dropLocation, item);
			//itemEntity.setVelocity(event.getVelocity()); //TODO: Random velocity
		}
		
		//Clear dispenser
		dispenser.getInventory().clear();
		Bukkit.getScheduler().runTaskLater(Automation.instance(),
				() -> dispenser.getInventory().setContents(remainingContents), 0L);
		
		return true;
	}
	
	private void replaceItem(Dispenser dispenser, ItemStack item) {
		int missing = InventoryUtil.getMissingStack(dispenser.getInventory(), item);
		if (missing == -1) dispenser.getInventory().addItem(item);
		else dispenser.getInventory().setItem(missing, item);
	}
	
	private boolean isAutocrafter(Block block) {
		return block instanceof Dispenser && isAutocrafter((Dispenser)block);
	}
	
	private boolean isAutocrafter(Dispenser dispenser) {
		BlockFace facing = ((Directional)dispenser.getData()).getFacing();
		Block slab = dispenser.getBlock().getRelative(facing);
		
		//Dispenser must be up or down, and be facing into a directly adjacent half slab
		return (facing == BlockFace.DOWN || facing == BlockFace.UP)
				&& BlockUtil.isHalfSlab(slab)
				&& BlockUtil.getHalfSlabFace(slab) == facing.getOppositeFace();
	}
	
	private ItemStack getFillerItem() {
		ItemStack result = new ItemStack(Material.STAINED_GLASS_PANE);
		ItemMeta meta = result.getItemMeta();
		meta.setDisplayName(ChatColor.WHITE.toString() + ChatColor.BOLD.toString() + "Filler");
		meta.setLore(Arrays.asList("", ChatColor.GRAY + "Fills a spot in Autocrafters"));
		meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		result.setItemMeta(meta);
		
		return result;
	}
	
	@SuppressWarnings("deprecation")
	private ItemStack getBlockerItem() {
		ItemStack result = new ItemStack(Material.STAINED_GLASS_PANE);
		result.setDurability(DyeColor.BLACK.getWoolData());
		ItemMeta meta = result.getItemMeta();
		meta.setDisplayName(ChatColor.WHITE.toString() + ChatColor.BOLD.toString() + "Blocker");
		meta.setLore(Arrays.asList("", ChatColor.GRAY + "Permanently fills a spot in Autocrafters"));
		meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		result.setItemMeta(meta);
		
		return result;
	}
	
	@SuppressWarnings("deprecation")
	private boolean itemIsFiller(ItemStack item) {
		return item.getType() == Material.STAINED_GLASS_PANE
				&& item.getDurability() == DyeColor.WHITE.getWoolData()
				&& item.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) > 0;
	}
	
	@SuppressWarnings("deprecation")
	private boolean itemIsBlocker(ItemStack item) {
		return item.getType() == Material.STAINED_GLASS_PANE
				&& item.getDurability() == DyeColor.BLACK.getWoolData()
				&& item.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) > 0;
	}
	
}
