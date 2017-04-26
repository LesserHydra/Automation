package com.lesserhydra.automation;

import com.lesserhydra.automation.BlockCartInteraction.Action;
import com.lesserhydra.automation.activator.DispenserInteraction;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.bukkitutil.InventoryUtil;
import com.lesserhydra.util.MapBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Cake;
import org.bukkit.material.Cauldron;
import org.bukkit.material.Comparator;
import org.bukkit.material.Diode;
import org.bukkit.material.Directional;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Openable;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.Tripwire;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

class BlockCartModule implements Module, Listener {
	
	//TODO: TrainCarts compatability
	
	//TODO: LOTS of cleanup, including:
	//Combine maps, use single class that controls how different materials are handled
	//Properly handle interact action
	//Sort functions nicely
	//Other...
	
	private final Map<Material, Class<? extends Minecart>> typeMap =
			MapBuilder.init(HashMap<Material, Class<? extends Minecart>>::new)
					.put(Material.TNT, ExplosiveMinecart.class)
					.put(Material.CHEST, StorageMinecart.class)
					.put(Material.TRAPPED_CHEST, StorageMinecart.class)
					.put(Material.BLACK_SHULKER_BOX, StorageMinecart.class)
					.put(Material.BLUE_SHULKER_BOX, StorageMinecart.class)
					.put(Material.BROWN_SHULKER_BOX, StorageMinecart.class)
					.put(Material.CYAN_SHULKER_BOX, StorageMinecart.class)
					.put(Material.GRAY_SHULKER_BOX, StorageMinecart.class)
					.put(Material.GREEN_SHULKER_BOX, StorageMinecart.class)
					.put(Material.LIGHT_BLUE_SHULKER_BOX, StorageMinecart.class)
					.put(Material.LIME_SHULKER_BOX, StorageMinecart.class)
					.put(Material.MAGENTA_SHULKER_BOX, StorageMinecart.class)
					.put(Material.ORANGE_SHULKER_BOX, StorageMinecart.class)
					.put(Material.PINK_SHULKER_BOX, StorageMinecart.class)
					.put(Material.PURPLE_SHULKER_BOX, StorageMinecart.class)
					.put(Material.RED_SHULKER_BOX, StorageMinecart.class)
					.put(Material.SILVER_SHULKER_BOX, StorageMinecart.class)
					.put(Material.WHITE_SHULKER_BOX, StorageMinecart.class)
					.put(Material.YELLOW_SHULKER_BOX, StorageMinecart.class)
					.put(Material.HOPPER, HopperMinecart.class)
					.put(Material.FURNACE, PoweredMinecart.class)
					.put(Material.MOB_SPAWNER, SpawnerMinecart.class)
					.put(Material.COMMAND, CommandMinecart.class)
					.buildImmutable();
	
	private final Map<Material, Function<ItemStack, MaterialData>> displayOverride =
			MapBuilder.init(HashMap<Material, Function<ItemStack, MaterialData>>::new)
					.put(Material.CAKE, item -> new Cake())
					.put(Material.CAULDRON_ITEM, item -> new Cauldron())
					.put(Material.BREWING_STAND_ITEM, item -> new MaterialData(Material.BREWING_STAND))
					.put(Material.STRING, item -> new Tripwire())
					.put(Material.STONE_BUTTON, BlockCartModule::makeAttachedBottom)
					.put(Material.WOOD_BUTTON, BlockCartModule::makeAttachedBottom)
					.put(Material.LEVER, BlockCartModule::makeAttachedBottom)
					.put(Material.DIODE, item -> new Diode(BlockFace.NORTH))
					.put(Material.REDSTONE_COMPARATOR, item -> new Comparator())
					.put(Material.PISTON_BASE, BlockCartModule::makeFacingUp)
					.put(Material.PISTON_STICKY_BASE, BlockCartModule::makeFacingUp)
					.put(Material.DISPENSER, BlockCartModule::makeFacingUp)
					.put(Material.DROPPER, BlockCartModule::makeFacingUp)
					.buildImmutable();
	
	private final Map<Material, BiConsumer<ItemStack, Minecart>> finalWork =
			MapBuilder.init(HashMap<Material, BiConsumer<ItemStack, Minecart>>::new)
					.put(Material.BLACK_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.BLUE_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.BROWN_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.CYAN_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.GRAY_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.GREEN_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.LIGHT_BLUE_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.LIME_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.MAGENTA_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.ORANGE_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.PINK_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.PURPLE_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.RED_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.SILVER_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.WHITE_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					.put(Material.YELLOW_SHULKER_BOX, BlockCartModule::finilizeShulkerCart)
					//.put(Material.MOB_SPAWNER, BlockCartModule::finalizeSpawner)
					.buildImmutable();

	private final Map<Material, Function<Minecart, ItemStack>> itemReturnOverride =
			MapBuilder.init(HashMap<Material, Function<Minecart, ItemStack>>::new)
					.put(Material.CAKE_BLOCK, cart -> {
						Cake cakeData = (Cake)cart.getDisplayBlock();
						if (cakeData.getSlicesEaten() > 0) return new ItemStack(Material.AIR);
						return new ItemStack(Material.CAKE, 1);
					})
					.put(Material.CAULDRON, cart -> new ItemStack(Material.CAULDRON_ITEM, 1))
					.put(Material.BREWING_STAND, cart -> new ItemStack(Material.BREWING_STAND_ITEM, 1))
					.put(Material.TRIPWIRE, cart -> new ItemStack(Material.STRING, 1))
					.put(Material.BLACK_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.BLUE_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.BROWN_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.CYAN_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.GRAY_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.GREEN_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.LIGHT_BLUE_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.LIME_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.MAGENTA_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.ORANGE_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.PINK_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.PURPLE_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.RED_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.SILVER_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.WHITE_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					.put(Material.YELLOW_SHULKER_BOX, BlockCartModule::shulkerboxReturn)
					//.put(Material.MOB_SPAWNER, BlockCartModule::spawnerReturn)
					.put(Material.DIODE_BLOCK_OFF, item -> new ItemStack(Material.DIODE, 1))
					.put(Material.REDSTONE_COMPARATOR_OFF, item -> new ItemStack(Material.REDSTONE_COMPARATOR, 1))
					.put(Material.DAYLIGHT_DETECTOR_INVERTED, item -> new ItemStack(Material.DAYLIGHT_DETECTOR, 1))
					.buildImmutable();
	
	private final Map<Material, BiConsumer<Player, Minecart>> clickFunctionality =
			MapBuilder.init(HashMap<Material, BiConsumer<Player, Minecart>>::new)
					.put(Material.CAKE_BLOCK, (player, cart) -> {
						Cake cakeData = (Cake) cart.getDisplayBlock();
						if (player.getFoodLevel() >= 20) return;
						cakeData.setSlicesRemaining(cakeData.getSlicesRemaining() - 1);
						cart.setDisplayBlock(cakeData);
						player.setFoodLevel(player.getFoodLevel() + 2);
						player.setSaturation(player.getSaturation() + 0.4F);
						if (cakeData.getSlicesRemaining() == 0) removeFromCart(cart);
					})
					.put(Material.ENDER_CHEST, (player, cart) -> {
						//TODO: player.playSound(cart.getLocation(), Sound.BLOCK_ENDERCHEST_OPEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
						player.openInventory(player.getEnderChest());
					})
					.put(Material.WORKBENCH, (player, cart) -> player.openWorkbench(cart.getLocation(), true))
					.put(Material.FENCE_GATE, (p, cart) -> interactWithOpenable(cart))
					.put(Material.ACACIA_FENCE_GATE, (p, cart) -> interactWithOpenable(cart))
					.put(Material.BIRCH_FENCE_GATE, (p, cart) -> interactWithOpenable(cart))
					.put(Material.DARK_OAK_FENCE_GATE, (p, cart) -> interactWithOpenable(cart))
					.put(Material.JUNGLE_FENCE_GATE, (p, cart) -> interactWithOpenable(cart))
					.put(Material.SPRUCE_FENCE_GATE, (p, cart) -> interactWithOpenable(cart))
					.put(Material.TRAP_DOOR, (p, cart) -> interactWithOpenable(cart))
					.put(Material.IRON_TRAPDOOR, (p, cart) -> interactWithOpenable(cart))
					.put(Material.LEVER, (p, cart) -> interactWithLever(cart))
					.put(Material.DIODE_BLOCK_OFF, (p, cart) -> interactWithDiode(cart))
					.put(Material.REDSTONE_COMPARATOR_OFF, (p, cart) -> interactWithComparator(cart))
					.put(Material.DAYLIGHT_DETECTOR, (p, cart) -> cart.setDisplayBlock(new MaterialData(Material.DAYLIGHT_DETECTOR_INVERTED)))
					.put(Material.DAYLIGHT_DETECTOR_INVERTED, (p, cart) -> cart.setDisplayBlock(new MaterialData(Material.DAYLIGHT_DETECTOR)))
					.buildImmutable();
	
	private static MaterialData makeFacingUp(ItemStack item) {
		MaterialData data = item.getData();
		((Directional)data).setFacingDirection(BlockFace.UP);
		return data;
	}
	
	private static MaterialData makeAttachedBottom(ItemStack item) {
		SimpleAttachableMaterialData data = (SimpleAttachableMaterialData) item.getData();
		data.setFacingDirection(BlockFace.UP);
		return data;
	}
	
	private static void interactWithLever(Minecart minecart) {
		Lever lever = (Lever) minecart.getDisplayBlock();
		lever.setPowered(!lever.isPowered());
		minecart.setDisplayBlock(lever);
	}
	
	private static void interactWithDiode(Minecart minecart) {
		Diode diode = (Diode) minecart.getDisplayBlock();
		diode.setDelay(diode.getDelay()%4 + 1);
		minecart.setDisplayBlock(diode);
	}
	
	private static void interactWithComparator(Minecart minecart) {
		Comparator comparator = (Comparator) minecart.getDisplayBlock();
		comparator.setSubtractionMode(!comparator.isSubtractionMode());
		minecart.setDisplayBlock(comparator);
	}
	
	private static void interactWithOpenable(Minecart minecart) {
		MaterialData display = minecart.getDisplayBlock();
		//TODO: Sound
		((Openable)display).setOpen(!((Openable)display).isOpen());
		minecart.setDisplayBlock(display);
	}
	
	//TODO: Currently requires nms
	/*private static void finalizeSpawner(ItemStack item, Minecart cart) {
		SpawnerMinecart spawnerCart = (SpawnerMinecart) cart;
		BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
		if (!meta.hasBlockState()) return;
		
		CreatureSpawner spawnerState = (CreatureSpawner) meta.getBlockState();
		spawnerCart.set
	}
	
	private static ItemStack spawnerReturn(Minecart minecart) {
	}*/
	
	private static void finilizeShulkerCart(ItemStack item, Minecart cart) {
		StorageMinecart storage = (StorageMinecart) cart;
		BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
		if (!meta.hasBlockState()) return;
		
		ShulkerBox boxState = (ShulkerBox) meta.getBlockState();
		Inventory boxInventory = boxState.getInventory();
		storage.getInventory().setContents(boxInventory.getContents());
		storage.setCustomName(meta.getDisplayName());
	}
	
	private static ItemStack shulkerboxReturn(Minecart minecart) {
		ItemStack item = minecart.getDisplayBlock().toItemStack(1);
		if (!(minecart instanceof StorageMinecart)) return item;
		
		StorageMinecart storage = (StorageMinecart) minecart;
		BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
		ShulkerBox boxState = (ShulkerBox) meta.getBlockState();
		boxState.getInventory().setContents(Arrays.copyOf(storage.getInventory().getContents(), 27));
		
		meta.setBlockState(boxState);
		meta.setDisplayName(storage.getCustomName());
		item.setItemMeta(meta);
		storage.getInventory().clear();
		storage.setCustomName(null);
		return item;
	}
	
	private final Automation plugin;
	
	BlockCartModule(Automation plugin) { this.plugin = plugin; }
	
	public void init() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		Automation.instance().getActivatorModule().registerHandler(this::handleBlockAdd);
		Automation.instance().getActivatorModule().registerHandler(Material.SHEARS, this::handleBlockRemove);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMinecartClicked(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Minecart)) return;
		Minecart minecart = (Minecart) event.getRightClicked();
		
		Player player = event.getPlayer();
		ItemStack handItem = player.getInventory().getItemInMainHand();
		
		//Cancel interaction with block cart
		if (!player.isSneaking() && minecart.getType() == EntityType.MINECART && doesMinecartHaveItem(minecart)) {
			event.setCancelled(true);
			//TODO: Use interaction?
			clickFunctionality.getOrDefault(minecart.getDisplayBlock().getItemType(), (p, m) -> {}).accept(player, minecart);
		}
		
		//Handle interaction
		Action action = (player.isSneaking() ? Action.REMOVE : Action.ADD);
		BlockCartInteraction interaction = new BlockCartInteraction(minecart, handItem, action);
		handleInteraction(interaction);
		
		//Cancel if nothing was done
		if (!interaction.isSuccess()) return;
		event.setCancelled(true);
		
		//Remove used item from hand
		if (interaction.isItemUsed()) player.getInventory().setItemInMainHand(InventoryUtil.subtractItem(handItem));
		
		//Add returned item to hand/inventory/ground
		if (interaction.hasResult()) InventoryUtil.givePlayerItem(player, interaction.getResult());
	}
	
	//TODO: Move to a single function for interacting with minecarts?
	private boolean handleBlockAdd(DispenserInteraction dispInteraction) {
		//Find facing minecart
		Minecart minecart = BlockUtil.getEntitiesInBlock(dispInteraction.getFacingBlock()).stream()
				.filter(entity -> entity instanceof Minecart)
				.map(entity -> (Minecart) entity)
				.filter(cart -> !doesMinecartHaveItem(cart))
				.findAny().orElse(null);
		if (minecart == null) return false;
		dispInteraction.validate();
		
		//Handle interaction
		BlockCartInteraction interaction = new BlockCartInteraction(minecart, dispInteraction.getItem(), Action.ADD);
		handleInteraction(interaction);
		
		//Fail if nothing was done
		if (!interaction.isSuccess()) return false;
		
		//Success
		dispInteraction.validate();
		dispInteraction.setKeepItem(!interaction.isItemUsed());
		if (interaction.hasResult()) dispInteraction.setResults(interaction.getResult());
		return true;
	}
	
	private boolean handleBlockRemove(DispenserInteraction dispInteraction) {
		dispInteraction.validate();
		
		//Find facing minecart
		Minecart minecart = BlockUtil.getEntitiesInBlock(dispInteraction.getFacingBlock()).stream()
				.filter(entity -> entity instanceof Minecart)
				.map(entity -> (Minecart) entity)
				.filter(BlockCartModule::doesMinecartHaveItem)
				.findAny().orElse(null);
		if (minecart == null) return false;
		
		//Handle interaction
		BlockCartInteraction interaction = new BlockCartInteraction(minecart, new ItemStack(Material.AIR), Action.REMOVE);
		handleInteraction(interaction);
		
		//Fail if nothing was done
		if (!interaction.isSuccess()) return false;
		
		//Success
		if (interaction.hasResult()) dispInteraction.setResults(interaction.getResult());
		return true;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityEnterMinecart(VehicleEnterEvent event) {
		if (!(event.getVehicle() instanceof Minecart)) return;
		if (doesMinecartHaveItem((Minecart) event.getVehicle())) event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockMinecartBreak(VehicleDestroyEvent event) {
		if (!(event.getVehicle() instanceof Minecart)) return;
		
		Minecart minecart = (Minecart) event.getVehicle();
		if (minecart.getDisplayBlock().getItemType() == Material.AIR) return;
		
		BlockCartInteraction interaction = new BlockCartInteraction(minecart, new ItemStack(Material.AIR), Action.REMOVE);
		removeFromCart(interaction);
		ItemStack item = interaction.getResult();
		interaction.getMinecart().getWorld().dropItemNaturally(interaction.getMinecart().getLocation().add(0, 0.5, 0), item);
		
		if (interaction.wasMinecartChanged()) {
			//Get new minecart
			minecart = interaction.getMinecart();
			assert(minecart.getType() == EntityType.MINECART);
			
			//Recall event
			event.setCancelled(true);
			VehicleDestroyEvent newEvent = new VehicleDestroyEvent(minecart, event.getAttacker());
			Bukkit.getServer().getPluginManager().callEvent(newEvent);
			
			//Destroy minecart
			if (newEvent.isCancelled()) return;
			ItemStack cartItem = new ItemStack(Material.MINECART, 1);
			ItemMeta cartItemMeta = cartItem.getItemMeta();
			cartItemMeta.setDisplayName(minecart.getCustomName());
			cartItem.setItemMeta(cartItemMeta);
			
			minecart.getWorld().dropItemNaturally(minecart.getLocation().add(0, 0.5, 0), cartItem);
			minecart.remove();
		}
	}
	
	private void handleInteraction(BlockCartInteraction interaction) {
		if (interaction.getAction() == Action.ADD) addToCart(interaction);
		else if (interaction.getAction() == Action.REMOVE) removeFromCart(interaction);
	}
	
	private void addToCart(BlockCartInteraction interaction) {
		Minecart minecart = interaction.getMinecart();
		MaterialData blockData = minecart.getDisplayBlock();
		Material usedType = interaction.getItem().getType();
		
		//Ensure minecart is empty
		if (blockData.getItemType() != Material.AIR) return;
		
		//Must have item to add
		if (usedType == Material.AIR) return;
		
		//Skip for non-block materials, if there is no override registered
		if (!usedType.isBlock()
				&& !displayOverride.containsKey(usedType)) return;
		
		//Skip for items with non-empty block state meta, if there is no finalizer registered
		if (interaction.getItem().getItemMeta() instanceof BlockStateMeta
				&& ((BlockStateMeta)interaction.getItem().getItemMeta()).hasBlockState()
				&& !finalWork.containsKey(usedType)) return;
		
		//Add block
		minecart.eject();
		
		//Change type if relevant
		Class<? extends Minecart> clazz = typeMap.get(usedType);
		if (clazz != null) minecart = changeMinecart(interaction, clazz);
		
		//Set display
		minecart.setDisplayBlock(displayOverride.getOrDefault(usedType, ItemStack::getData).apply(interaction.getItem()));
		
		//Finalize stuff
		finalWork.getOrDefault(usedType, (i, m) -> {}).accept(interaction.getItem(), minecart);
		
		//Success
		interaction.setItemUsed(true);
		interaction.setSuccess(true);
	}
	
	private void removeFromCart(Minecart minecart) {
		removeFromCart(new BlockCartInteraction(minecart, new ItemStack(Material.AIR), Action.REMOVE));
	}
	private void removeFromCart(BlockCartInteraction interaction) {
		Minecart minecart = interaction.getMinecart();
		MaterialData blockData = minecart.getDisplayBlock();
		
		//Ensure minecart is not empty
		if (blockData.getItemType() == Material.AIR) return;
		
		//Remove block
		ItemStack item = takeItemFromCart(minecart);
		interaction.setResult(item);
		interaction.setSuccess(true);
		minecart.setDisplayBlock(new MaterialData(Material.AIR));
		if (minecart.getType() != EntityType.MINECART) minecart = changeMinecart(interaction, RideableMinecart.class);
	}
	
	private ItemStack takeItemFromCart(Minecart minecart) {
		return itemReturnOverride.getOrDefault(minecart.getDisplayBlock().getItemType(), cart -> cart.getDisplayBlock().toItemStack(1))
				.apply(minecart);
	}
	
	public static boolean doesMinecartHaveItem(Minecart minecart) {
		return minecart.getType() != EntityType.MINECART
				|| minecart.getDisplayBlock().getItemType() != Material.AIR;
	}
	
	public static <T extends Minecart> T changeMinecart(BlockCartInteraction interaction, Class<T> clazz) {
		Minecart minecart = interaction.getMinecart();
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
		result.setCustomName(minecart.getCustomName());
		
		interaction.setMinecart(result);
		return result;
	}
	
}
