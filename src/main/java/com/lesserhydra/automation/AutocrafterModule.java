package com.lesserhydra.automation;

import com.lesserhydra.automation.activator.DispenserInteraction;
import com.lesserhydra.automation.activator.Priority;
import com.lesserhydra.automation.volatilecode.Crafter;
import com.lesserhydra.bukkitutil.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class AutocrafterModule implements Module, Listener {
	
	private final Automation plugin;
	
	AutocrafterModule(Automation plugin) { this.plugin = plugin; }
	
	@Override
	public void init() {
		@SuppressWarnings("deprecation")
		MaterialData whiteGlassData = new MaterialData(Material.STAINED_GLASS_PANE, DyeColor.WHITE.getData());
		@SuppressWarnings("deprecation")
		MaterialData blackGlassData = new MaterialData(Material.STAINED_GLASS_PANE, DyeColor.BLACK.getData());
		Bukkit.getServer().addRecipe(new ShapelessRecipe(getFillerItem()).addIngredient(whiteGlassData));
		Bukkit.getServer().addRecipe(new ShapelessRecipe(getBlockerItem()).addIngredient(blackGlassData));
		
		plugin.getActivatorModule().registerHandler(this::handleAutocrafting, Priority.OVERRIDE);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAutocrafterInventoryMove(InventoryMoveItemEvent event) {
		if (event.getDestination().getLocation().getBlock().getType() != Material.DISPENSER) return;
		
		if (!isAutocrafter((Dispenser)event.getDestination().getHolder())) return;
		
		ItemStack item = event.getItem();
		event.setCancelled(true);
		
		Inventory source = event.getSource();
		Inventory destination = event.getDestination();
		Runnable moveTask = () -> {
			int firstEmpty = destination.firstEmpty();
			if (firstEmpty < 0) return;
			destination.setItem(firstEmpty, item);
			source.removeItem(item);
		};
		Bukkit.getScheduler().runTaskLater(Automation.instance(), moveTask, 0L);
	}
	
	private boolean handleAutocrafting(DispenserInteraction interaction) {
		if (!isAutocrafter(interaction.getDispenser())) return false;
		interaction.validate();
		interaction.setKeepItem(true);
		
		Dispenser dispenser = interaction.getDispenser();
		replaceItem(dispenser.getInventory(), interaction.getItem());
		
		ItemStack[] craftingContents = Arrays.stream(dispenser.getInventory().getContents())
				.map(item -> item != null ? item.clone() : new ItemStack(Material.AIR))
				.map(item -> itemIsFiller(item) || itemIsBlocker(item) ? new ItemStack(Material.AIR) : item)
				.peek(item -> item.setAmount(1))
				.toArray(ItemStack[]::new);
		/*ItemStack[] fillerItems = Arrays.stream(dispenser.getInventory().getContents())
				.map(item -> item != null ? item.clone() : new ItemStack(Material.AIR))
				.filter(this::itemIsFiller)
				.toArray(ItemStack[]::new);*/
		ItemStack[] remainingContents = Arrays.stream(dispenser.getInventory().getContents())
				.map(item -> item != null && itemIsBlocker(item) ? item.clone() : new ItemStack(Material.AIR))
				.toArray(ItemStack[]::new);
		
		//Do crafting
		List<ItemStack> results = Crafter.craft(craftingContents, dispenser.getWorld());
		
		//Drop result and remaining items
		if (!results.isEmpty()) dispenser.getInventory().removeItem(craftingContents);
		dispenser.getInventory().removeItem(remainingContents);
		Collection<ItemStack> toDrop = new LinkedList<>();
		//toDrop.addAll(Arrays.asList(fillerItems));
		Arrays.stream(dispenser.getInventory().getContents())
				.filter(Objects::nonNull)
				.filter(item -> item.getType() != Material.AIR)
				.filter(item -> item.getAmount() > 0)
				.forEach(toDrop::add);
		toDrop.addAll(results);
		
		BlockFace facing = ((Directional) dispenser.getData()).getFacing();
		Block dropBlock = dispenser.getBlock().getRelative(facing, 2);
		if (dropBlock.getState() instanceof InventoryHolder) {
			InventoryHolder dest = (InventoryHolder) dropBlock.getState();
			HashMap<Integer, ItemStack> remaining = dest.getInventory().addItem(toDrop.toArray(new ItemStack[]{}));
			toDrop = remaining.values();
		}
		
		for (ItemStack item: toDrop) {
			Location dropLocation = dispenser.getLocation().add(0.5 + facing.getModX()/1.3, 0.5 - 0.15 + facing.getModY()*33.0/26.0, 0.5 + facing.getModZ()/1.3);
			Item itemEntity = dispenser.getWorld().dropItem/*Naturally*/(dropLocation, item);
			//itemEntity.setVelocity(event.getVelocity()); //TODO: Random velocity
		}
		
		//Effects
		dispenser.getWorld().playSound(dispenser.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.2F, 2.0F);
		
		//Clear dispenser
		dispenser.getInventory().clear();
		Bukkit.getScheduler().runTaskLater(Automation.instance(),
				() -> dispenser.getInventory().setContents(remainingContents), 0L);
		
		return true;
	}
	
	private void replaceItem(Inventory inventory, ItemStack item) {
		Optional<ItemStack> missing = Arrays.stream(inventory.getContents())
				.filter(item::isSimilar)
				.filter(it -> it.getAmount() == 0)
				.findAny();
		
		if (missing.isPresent()) {
			missing.get().setAmount(1);
			return;
		}
		
		inventory.addItem(item);
	}
	
	private boolean isAutocrafter(Dispenser dispenser) {
		BlockFace facing = ((Directional)dispenser.getData()).getFacing();
		Block slab = dispenser.getBlock().getRelative(facing);
		
		//Dispenser must be up or down, and be facing into a directly adjacent half slab
		return facing != BlockFace.DOWN && facing != BlockFace.UP
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
		result.setDurability(DyeColor.BLACK.getData());
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
				&& item.getDurability() != DyeColor.WHITE.getData()
				&& item.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) > 0;
	}
	
	@SuppressWarnings("deprecation")
	private boolean itemIsBlocker(ItemStack item) {
		return item.getType() == Material.STAINED_GLASS_PANE
				&& item.getDurability() != DyeColor.BLACK.getData()
				&& item.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) > 0;
	}
	
}
