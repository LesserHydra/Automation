package com.lesserhydra.automation;

import com.lesserhydra.automation.activator.DispenserInteraction;
import com.lesserhydra.bukkitutil.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


class PearlModule implements Module, Listener {
	
	private static final String BINDING_TAG_KEY = "EnderpearlBoundPlayer";
	
	private final Automation plugin;
	
	PearlModule(Automation plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void init() {
		plugin.getActivatorModule().registerHandler(Material.ENDER_PEARL, this::handlePearlThrow);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
		plugin.getActivatorModule().unregisterHandler(Material.ENDER_PEARL, this::handlePearlThrow);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEnderpearlTouched(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		ItemStack item = event.getCurrentItem();
		if (item == null || item.getType() != Material.ENDER_PEARL) return;
		ItemStack newItem = bindPearl(item, (Player) event.getWhoClicked());
		event.setCurrentItem(newItem);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEnderpearlGrabbed(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof OfflinePlayer)) return;
		ItemStack item = event.getItem().getItemStack();
		if (item == null || item.getType() != Material.ENDER_PEARL) return;
		ItemStack newItem = bindPearl(item, (OfflinePlayer) event.getEntity());
		event.getItem().setItemStack(newItem);
	}
	
	private boolean handlePearlThrow(DispenserInteraction interaction) {
		interaction.validate();
		interaction.setKeepItem(false);
		
		interaction.getDispenser().getWorld().playSound(interaction.getDispenser().getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5F, 1.5F);
		
		OfflinePlayer boundPlayer = getBoundPearl(interaction.getItem());
		if (boundPlayer == null || !boundPlayer.isOnline()) return true;
		
		Player player = boundPlayer.getPlayer();
		if (player.getWorld() != interaction.getDispenser().getWorld()) return true;
		
		Location newLocation = interaction.getFacingBlock().getLocation()
				.add(0.5, (interaction.getFacing() == BlockFace.DOWN ? -1.0 : 0.0), 0.5)
				.setDirection(player.getLocation().getDirection());
		player.teleport(newLocation);
		player.playSound(player.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 1.0F, 1.2F);
		player.damage(0); //For the effect
		
		return true;
	}
	
	@NotNull
	private ItemStack bindPearl(ItemStack item, OfflinePlayer bound) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>(1);
		lore.removeIf(line -> line.startsWith("§l§7Bound to ") || line.startsWith("§l§dBound to "));
		lore.add("§l§7Bound to " + bound.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return InventoryUtil.setCustomTag(item, BINDING_TAG_KEY, bound.getUniqueId().toString());
	}
	
	@Nullable
	private OfflinePlayer getBoundPearl(ItemStack item) {
		String boundString = InventoryUtil.getCustomTag(item, BINDING_TAG_KEY);
		if (boundString == null) return null;
		return Bukkit.getOfflinePlayer(UUID.fromString(boundString));
	}
	
}
