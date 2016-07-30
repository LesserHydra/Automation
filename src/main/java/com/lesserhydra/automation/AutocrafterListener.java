package com.lesserhydra.automation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dropper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import com.lesserhydra.automation.volatilecode.Crafter;

public class AutocrafterListener implements Listener {
	
	//TODO: Attempt to use armorstands as markers and display of items; this would solve most issues and look really cool
	//TODO: Need filter item for current strategy (Stained glass panes!)
	//TODO: Permenant filter items in addition to the temp ones?
	//TODO: Block discuise is not complete
	//TODO: Confuses client when breaking
	//TODO: Inventory syncing (multiple players, hopper input/output, ect)
	//TODO: Hopper below would be nicer - but possibly laggy, since blocked hoppers run every tick!
	//TODO: Attempt to support FastCraft
	
	private static final String autocrafterLocationMeta = "automation.autocrafterlocation";
	
	private final Set<Location> autocrafterLocations = new HashSet<>();
	
	
	public void init() {
		@SuppressWarnings("deprecation")
		MaterialData glassData = new MaterialData(Material.STAINED_GLASS_PANE, DyeColor.WHITE.getData());
		Bukkit.getServer().addRecipe(new ShapelessRecipe(getFillerItem()).addIngredient(glassData));
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onAutocrafterPlace(BlockPlaceEvent event) {
		if (event.getBlock().getType() != Material.WORKBENCH) return;
		
		BlockState blockState = event.getBlock().getState();
		autocrafterLocations.add(blockState.getLocation());
		blockState.setType(Material.DROPPER);
		MaterialData dropperData = blockState.getData();
		((Directional) dropperData).setFacingDirection(BlockFace.DOWN);
		blockState.setData(dropperData);
		blockState.update(true);
		
		Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> blockState.getWorld().getPlayers().stream()
				.forEach(player -> player.sendBlockChange(blockState.getLocation(), Material.WORKBENCH, (byte) 0))
				, 1L);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onAutocrafterBreak(BlockBreakEvent event) {
		if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
		if (event.getBlock().getType() != Material.DROPPER) return;
		if (!autocrafterLocations.contains(event.getBlock().getLocation())) return;
		
		Block block = event.getBlock();
		event.setCancelled(true);
		block.setType(Material.AIR);
		block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.WORKBENCH));
		autocrafterLocations.remove(block.getLocation());
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onAutocrafterInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock().getType() != Material.DROPPER) return;
		if (!autocrafterLocations.contains(event.getClickedBlock().getLocation())) return;

		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		
		Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> player.sendBlockChange(block.getLocation(), Material.WORKBENCH, (byte) 0), 1L);
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getPlayer().isSneaking()) return;
		
		Dropper dropper = (Dropper) block.getState();
		InventoryView crafterView = player.openWorkbench(block.getLocation(), true);
		
		player.setMetadata(autocrafterLocationMeta, new FixedMetadataValue(Automation.instance(), block.getLocation()));
		
		CraftingInventory inv = (CraftingInventory) crafterView.getTopInventory();
		inv.setMatrix(dropper.getInventory().getContents());

		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onAutocrafterClose(InventoryCloseEvent event) {
		if (event.getInventory().getType() != InventoryType.WORKBENCH) return;
		
		CraftingInventory inv = (CraftingInventory) event.getInventory();	
		
		HumanEntity player = event.getPlayer();
		Optional<MetadataValue> foundMetadata = player.getMetadata(autocrafterLocationMeta).stream()
				.filter(meta -> meta.getOwningPlugin() == Automation.instance())
				.findAny();
		if (!foundMetadata.isPresent()) return;
		
		Block block = ((Location) foundMetadata.get().value()).getBlock();
		if (block.getType() != Material.DROPPER) return;
		if (!autocrafterLocations.contains(block.getLocation())) return;
		
		Dropper dropper = (Dropper) block.getState();
		dropper.getInventory().setContents(inv.getMatrix());
		inv.clear();
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onAutocrafterInventoryMove(InventoryMoveItemEvent event) {
		if (event.getDestination().getLocation().getBlock().getType() != Material.DROPPER) return;
		
		Location location = event.getDestination().getLocation();
		if (!autocrafterLocations.contains(location)) return;
		
		ItemStack item = event.getItem();
		event.setCancelled(true);
		
		Runnable moveTask = () -> {
			int firstEmpty = event.getDestination().firstEmpty();
			if (firstEmpty < 0) return;
			event.getDestination().setItem(firstEmpty, item);
			event.getSource().removeItem(item);
		};
		Bukkit.getScheduler().runTaskLater(Automation.instance(), moveTask, 0L);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onAutocrafterFire(BlockDispenseEvent event) {
		if (event.getBlock().getType() != Material.DROPPER) return;
		if (!autocrafterLocations.contains(event.getBlock().getLocation())) return;
		
		Dropper dropper = (Dropper) event.getBlock().getState();
		
		ItemStack[] craftingContents = Arrays.stream(dropper.getInventory().getContents())
				.map(item -> item != null ? item.clone() : new ItemStack(Material.AIR))
				.map(item -> itemIsFiller(item) ? new ItemStack(Material.AIR) : item)
				.peek(item -> item.setAmount(1))
				.toArray(ItemStack[]::new);
		ItemStack[] fillerItems = Arrays.stream(dropper.getInventory().getContents())
				.map(item -> item != null ? item.clone() : new ItemStack(Material.AIR))
				.filter(this::itemIsFiller)
				.toArray(ItemStack[]::new);
		
		//Do crafting
		List<ItemStack> results = Crafter.craft(craftingContents, dropper.getWorld());
		if (results.isEmpty()) {
			event.setCancelled(true);
			return;
		}
		
		//Drop result and remaining items
		dropper.getInventory().addItem(event.getItem());
		event.setItem(results.remove(0));
		dropper.getInventory().removeItem(craftingContents);
		dropper.getInventory().removeItem(fillerItems);
		List<ItemStack> toDrop = new LinkedList<>();
		toDrop.addAll(Arrays.asList(fillerItems));
		Arrays.stream(dropper.getInventory().getContents())
				.filter(Objects::nonNull)
				.filter(item -> item.getType() != Material.AIR)
				.filter(item -> item.getAmount() > 0)
				.forEach(toDrop::add);
		toDrop.addAll(results);
		
		BlockFace facing = ((Directional) event.getBlock().getState().getData()).getFacing();
		for (ItemStack item: toDrop) {
			Location dropLocation = dropper.getLocation().add(0.5 + facing.getModX()/1.3, 0.5 + facing.getModY()/1.3, 0.5 + facing.getModZ()/1.3);
			Item itemEntity = dropper.getWorld().dropItemNaturally(dropLocation, item);
			itemEntity.setVelocity(event.getVelocity());
		}
		
		//Clear dispenser
		dropper.getInventory().clear();
		Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> dropper.getInventory().clear(), 0L);
		
	}
	
	private ItemStack getFillerItem() {
		ItemStack result = new ItemStack(Material.STAINED_GLASS_PANE);
		ItemMeta meta = result.getItemMeta();
		meta.setDisplayName(ChatColor.WHITE.toString() + ChatColor.BOLD.toString() + "Filler");
		meta.setLore(Arrays.asList("", ChatColor.GRAY + "Fills a spot in Crafting Crates"));
		meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		result.setItemMeta(meta);
		
		return result;
	}
	
	private boolean itemIsFiller(ItemStack item) {
		if (item.getType() != Material.STAINED_GLASS_PANE) return false;
		return (item.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) > 0);
	}
	
}
