package com.lesserhydra.automation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import com.lesserhydra.automation.BlockCartInteraction.Action;
import com.lesserhydra.automation.activator.DispenserInteraction;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.bukkitutil.InventoryUtil;
import com.lesserhydra.util.MapBuilder;

public class BlockCartListener implements Listener {
	
	private final Map<Material, Class<? extends Minecart>> typeMap =
			MapBuilder.init(HashMap<Material, Class<? extends Minecart>>::new)
			.put(Material.TNT, ExplosiveMinecart.class)
			.put(Material.CHEST, StorageMinecart.class)
			.put(Material.TRAPPED_CHEST, StorageMinecart.class)
			.put(Material.HOPPER, HopperMinecart.class)
			.put(Material.FURNACE, PoweredMinecart.class)
			//.put(Material.MOB_SPAWNER, SpawnerMinecart.class)
			.put(Material.COMMAND, CommandMinecart.class)
			.buildImmutable();
	
	
	public void init() {
		Automation.instance().getActivatorModule().registerHandler(this::handleBlockAdd);
		Automation.instance().getActivatorModule().registerHandler(Material.SHEARS, this::handleBlockRemove);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMinecartClicked(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Minecart)) return;
		Minecart minecart = (Minecart) event.getRightClicked();
		
		Player player = event.getPlayer();
		ItemStack handItem = player.getInventory().getItemInMainHand();
		
		//Cancel interaction with block cart
		if (minecart.getType() == EntityType.MINECART && doesMinecartHaveItem(minecart)) event.setCancelled(true);
		
		//Handle interaction
		BlockCartInteraction interaction = new BlockCartInteraction(handItem, (player.isSneaking() ? Action.REMOVE : Action.ADD));
		handleInteraction(minecart, interaction);
		
		//Cancel if nothing was done
		if (!interaction.isSuccess()) return;
		event.setCancelled(true);
		
		//Remove used item from hand
		if (interaction.isItemUsed()) player.getInventory().setItemInMainHand(InventoryUtil.subtractItem(handItem));
		
		//Add returned item to hand/inventory/ground
		if (interaction.hasResult()) InventoryUtil.givePlayerItem(player, interaction.getResult());
	}
	
	//TODO: Move to a single function for interacting with minecarts?
	public boolean handleBlockAdd(DispenserInteraction dispInteraction) {
		//Find facing minecart
		Minecart minecart = BlockUtil.getEntitiesInBlock(dispInteraction.getFacingBlock()).stream()
				.filter(entity -> entity instanceof Minecart)
				.map(entity -> (Minecart) entity)
				.filter(cart -> !doesMinecartHaveItem(cart))
				.findAny().orElse(null);
		if (minecart == null) return false;
		dispInteraction.validate();
		
		//Handle interaction
		BlockCartInteraction interaction = new BlockCartInteraction(dispInteraction.getItem(), Action.ADD);
		handleInteraction(minecart, interaction);
		
		//Fail if nothing was done
		if (!interaction.isSuccess()) return false;
		
		//Success
		dispInteraction.validate();
		dispInteraction.setKeepItem(interaction.isItemUsed());
		if (interaction.hasResult()) dispInteraction.setResults(new ItemStack[]{interaction.getResult()});
		return true;
	}
	
	public boolean handleBlockRemove(DispenserInteraction dispInteraction) {
		dispInteraction.validate();
		
		//Find facing minecart
		Minecart minecart = BlockUtil.getEntitiesInBlock(dispInteraction.getFacingBlock()).stream()
				.filter(entity -> entity instanceof Minecart)
				.map(entity -> (Minecart) entity)
				.filter(cart -> doesMinecartHaveItem(cart))
				.findAny().orElse(null);
		if (minecart == null) return false;
		
		//Handle interaction
		BlockCartInteraction interaction = new BlockCartInteraction(new ItemStack(Material.AIR), Action.REMOVE);
		handleInteraction(minecart, interaction);
		
		//Fail if nothing was done
		if (!interaction.isSuccess()) return false;
		
		//Success
		dispInteraction.setDamageItem(true);
		if (interaction.hasResult()) dispInteraction.setResults(new ItemStack[]{interaction.getResult()});
		return true;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityEnterMinecart(VehicleEnterEvent event) {
		if (!(event.getVehicle() instanceof Minecart)) return;
		if (doesMinecartHaveItem((Minecart) event.getVehicle())) event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockMinecartBreak(VehicleDestroyEvent event) {
		if (event.getVehicle().getType() != EntityType.MINECART) return;
		Minecart minecart = (Minecart) event.getVehicle();
		MaterialData currentBlock = minecart.getDisplayBlock();
		if (currentBlock.getItemType() == Material.AIR) return;
		
		ItemStack item = currentBlock.toItemStack();
		item.setAmount(1);
		minecart.getWorld().dropItemNaturally(minecart.getLocation().add(0, 0.5, 0), item);
	}
	
	private void handleInteraction(Minecart minecart, BlockCartInteraction interaction) {
		if (interaction.getAction() == Action.ADD) addToCart(minecart, interaction);
		else if (interaction.getAction() == Action.REMOVE) removeFromCart(minecart, interaction);
	}
	
	private void addToCart(Minecart minecart, BlockCartInteraction interaction) {
		MaterialData blockData = minecart.getDisplayBlock();
		Material usedType = interaction.getItem().getType();
		
		//Ensure minecart is empty
		if (blockData.getItemType() != Material.AIR) return;
		
		//Must have item to add
		if (usedType == Material.AIR) return;
		
		//FIXME: Liquids don't show up. Better solution?
		/*//Do liquids
		if (InventoryUtil.isLiquidBucket(usedType)) {
			interaction.setResult(new ItemStack(Material.BUCKET));
			interaction.setItemUsed(true);
			interaction.setSuccess(true);
			minecart.setDisplayBlock(new MaterialData(InventoryUtil.getLiquid(usedType)));
			return;
		}*/
		
		//The following is only for block materials
		if (!usedType.isBlock()) return;
		
		//Add block
		minecart.eject();
		
		//Change type if relevant
		Class<? extends Minecart> clazz = typeMap.get(usedType);
		if (clazz != null) minecart = changeMinecart(minecart, clazz);
		
		//Set display
		minecart.setDisplayBlock(interaction.getItem().getData());
		
		//Success
		interaction.setItemUsed(true);
		interaction.setSuccess(true);
	}
	
	private void removeFromCart(Minecart minecart, BlockCartInteraction interaction) {
		MaterialData blockData = minecart.getDisplayBlock();
		
		//Ensure minecart is not empty
		if (blockData.getItemType() == Material.AIR) return;
		
		//FIXME: Liquids don't show up. Better solution?
		/*//Do liquids
		if (InventoryUtil.isLiquid(blockData.getItemType())) {
			if (interaction.getItem().getType() != Material.BUCKET) return;
			interaction.setResult(new ItemStack(InventoryUtil.getBucket(blockData.getItemType())));
			interaction.setItemUsed(true);
			interaction.setSuccess(true);
			minecart.setDisplayBlock(new MaterialData(Material.AIR));
			return;
		}*/
		
		//Remove block
		ItemStack item = blockData.toItemStack();
		item.setAmount(1);
		interaction.setResult(item);
		interaction.setSuccess(true);
		minecart.setDisplayBlock(new MaterialData(Material.AIR));
		if (minecart.getType() != EntityType.MINECART) changeMinecart(minecart, RideableMinecart.class);
	}
	
	public static boolean doesMinecartHaveItem(Minecart minecart) {
		if (minecart.getType() != EntityType.MINECART) return true;
		return (minecart.getDisplayBlock().getItemType() != Material.AIR);
	}
	
	public static <T extends Minecart> T changeMinecart(Minecart minecart, Class<T> clazz) {
		if (clazz.isInstance(minecart)) return clazz.cast(minecart);
		
		if (minecart instanceof InventoryHolder) {
			Inventory inventory = ((InventoryHolder) minecart).getInventory();
			Arrays.stream(inventory.getContents())
					.filter(item -> item != null && item.getType() != Material.AIR)
					.forEach(item -> minecart.getWorld().dropItemNaturally(minecart.getLocation().add(0, 0.5, 0), item));
			inventory.clear();
		}
		
		minecart.remove();
		T result = minecart.getWorld().spawn(minecart.getLocation(), clazz);
		result.teleport(minecart.getLocation());
		result.setVelocity(minecart.getVelocity());
		result.setDamage(minecart.getDamage());
		result.setDerailedVelocityMod(minecart.getDerailedVelocityMod());
		result.setFlyingVelocityMod(minecart.getFlyingVelocityMod());
		result.setMaxSpeed(minecart.getMaxSpeed());
		result.setSlowWhenEmpty(minecart.isSlowWhenEmpty());
		return result;
	}
	
}
