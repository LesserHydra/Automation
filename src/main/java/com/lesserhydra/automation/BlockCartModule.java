package com.lesserhydra.automation;

import com.lesserhydra.automation.BlockCartInteraction.Action;
import com.lesserhydra.automation.activator.DispenserInteraction;
import com.lesserhydra.bukkitutil.AdvancementUtil;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.bukkitutil.InventoryUtil;
import com.lesserhydra.util.MapBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
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
import org.bukkit.material.Gate;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.RedstoneWire;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.material.Tripwire;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class BlockCartModule implements Module, Listener {
	
	//TODO: TrainCarts compatability
	
	//TODO: LOTS of cleanup, including:
	//Combine maps, use single class that controls how different materials are handled
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
					.put(Material.REDSTONE, item -> new RedstoneWire())
					.put(Material.STONE_BUTTON, BlockCartModule::makeAttachedBottom)
					.put(Material.WOOD_BUTTON, BlockCartModule::makeAttachedBottom)
					.put(Material.LEVER, BlockCartModule::makeAttachedBottom)
					.put(Material.DIODE, item -> new Diode(BlockFace.NORTH))
					.put(Material.REDSTONE_COMPARATOR, item -> new Comparator())
					.put(Material.PISTON_BASE, BlockCartModule::makeFacingForward)
					.put(Material.PISTON_STICKY_BASE, BlockCartModule::makeFacingForward)
					.put(Material.DISPENSER, BlockCartModule::makeFacingForward)
					.put(Material.DROPPER, BlockCartModule::makeFacingForward)
					.put(Material.OBSERVER, BlockCartModule::makeFacingForward)
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
					.put(Material.REDSTONE_WIRE, cart -> new ItemStack(Material.REDSTONE, 1))
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
	
	private final Map<Material, Consumer<BlockCartInteraction>> clickFunctionality =
			MapBuilder.init(HashMap<Material, Consumer<BlockCartInteraction>>::new)
					.put(Material.CAKE_BLOCK, BlockCartModule::interactWithCake)
					.put(Material.ENDER_CHEST, BlockCartModule::interactWithEnderchest)
					.put(Material.WORKBENCH, (interaction) -> {
						if (!interaction.hasPlayer()) return;
						interaction.setSuccess(true);
						Player player = interaction.getPlayer();
						Minecart cart = interaction.getMinecart();
						player.openWorkbench(cart.getLocation(), true);
					})
					.put(Material.FENCE_GATE, BlockCartModule::interactWithGate)
					.put(Material.ACACIA_FENCE_GATE, BlockCartModule::interactWithGate)
					.put(Material.BIRCH_FENCE_GATE, BlockCartModule::interactWithGate)
					.put(Material.DARK_OAK_FENCE_GATE, BlockCartModule::interactWithGate)
					.put(Material.JUNGLE_FENCE_GATE, BlockCartModule::interactWithGate)
					.put(Material.SPRUCE_FENCE_GATE, BlockCartModule::interactWithGate)
					.put(Material.TRAP_DOOR, BlockCartModule::interactWithTrapdoor)
					.put(Material.IRON_TRAPDOOR, BlockCartModule::interactWithIronTrapdoor)
					.put(Material.LEVER, BlockCartModule::interactWithLever)
					.put(Material.DIODE_BLOCK_OFF, BlockCartModule::interactWithDiode)
					.put(Material.REDSTONE_COMPARATOR_OFF, BlockCartModule::interactWithComparator)
					.put(Material.DAYLIGHT_DETECTOR, (interaction) -> {
						interaction.setSuccess(true);
						interaction.getMinecart().setDisplayBlock(new MaterialData(Material.DAYLIGHT_DETECTOR_INVERTED));
					})
					.put(Material.DAYLIGHT_DETECTOR_INVERTED, (interaction) -> {
						interaction.setSuccess(true);
						interaction.getMinecart().setDisplayBlock(new MaterialData(Material.DAYLIGHT_DETECTOR));
					})
					.put(Material.CAULDRON, BlockCartModule::interactWithCauldron)
					.buildImmutable();
	
	private static MaterialData makeFacingUp(ItemStack item) {
		MaterialData data = item.getData();
		((Directional)data).setFacingDirection(BlockFace.UP);
		return data;
	}
	
	private static MaterialData makeFacingForward(ItemStack item) {
		MaterialData data = item.getData();
		((Directional)data).setFacingDirection(BlockFace.NORTH);
		return data;
	}
	
	private static MaterialData makeAttachedBottom(ItemStack item) {
		SimpleAttachableMaterialData data = (SimpleAttachableMaterialData) item.getData();
		data.setFacingDirection(BlockFace.UP);
		return data;
	}
	
	@SuppressWarnings("deprecation")
	private static void interactWithCauldron(BlockCartInteraction interaction) {
		ItemStack item = interaction.getItem();
		Minecart minecart = interaction.getMinecart();
		Cauldron cauldron = (Cauldron) interaction.getMinecart().getDisplayBlock();
		
		if (item.getType() == Material.WATER_BUCKET) {
			if (cauldron.isFull()) return;
			cauldron.setData((byte)3);
			minecart.setDisplayBlock(cauldron);
			minecart.getWorld().playSound(minecart.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0F, 1.0F);
			interaction.setSuccess(true);
			interaction.setItemUsed(true);
			interaction.setResult(new ItemStack(Material.BUCKET, 1));
		}
		else if (item.getType() == Material.BUCKET) {
			if (!cauldron.isFull()) return;
			cauldron.setData((byte)0);
			minecart.setDisplayBlock(cauldron);
			minecart.getWorld().playSound(minecart.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0F, 1.0F);
			interaction.setSuccess(true);
			interaction.setItemUsed(true);
			interaction.setResult(new ItemStack(Material.WATER_BUCKET, 1));
		}
		else if (item.getType() == Material.POTION) {
			if (cauldron.isFull()) return;
			cauldron.setData((byte) (cauldron.getData() + 1));
			minecart.setDisplayBlock(cauldron);
			minecart.getWorld().playSound(minecart.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1.0F, 1.0F);
			interaction.setSuccess(true);
			interaction.setItemUsed(true);
			interaction.setResult(new ItemStack(Material.GLASS_BOTTLE, 1));
		}
		else if (item.getType() == Material.GLASS_BOTTLE) {
			if (cauldron.isEmpty()) return;
			cauldron.setData((byte) (cauldron.getData() - 1));
			minecart.setDisplayBlock(cauldron);
			minecart.getWorld().playSound(minecart.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0F, 1.0F);
			interaction.setSuccess(true);
			interaction.setItemUsed(true);
			interaction.setResult(new ItemStack(Material.POTION, 1));
		}
	}
	
	private static void interactWithCake(BlockCartInteraction interaction) {
		if (!interaction.hasPlayer()) return;
		interaction.setSuccess(true);
		
		Player player = interaction.getPlayer();
		Minecart cart = interaction.getMinecart();
		Cake cakeData = (Cake) cart.getDisplayBlock();
		if (player.getFoodLevel() >= 20) return;
		
		cakeData.setSlicesRemaining(cakeData.getSlicesRemaining() - 1);
		cart.setDisplayBlock(cakeData);
		player.setFoodLevel(player.getFoodLevel() + 2);
		player.setSaturation(player.getSaturation() + 0.4F);
		
		if (cakeData.getSlicesRemaining() == 0) cart.setDisplayBlock(new MaterialData(Material.AIR));
	}
	
	private static void interactWithEnderchest(BlockCartInteraction interaction) {
		if (!interaction.hasPlayer()) return;
		interaction.setSuccess(true);
		
		Player player = interaction.getPlayer();
		Minecart cart = interaction.getMinecart();
		cart.getWorld().playSound(cart.getLocation(), Sound.BLOCK_ENDERCHEST_OPEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
		player.openInventory(player.getEnderChest());
		//TODO: Close sound?
	}
	
	private static void interactWithLever(BlockCartInteraction interaction) {
		interaction.setSuccess(true);
		Minecart minecart = interaction.getMinecart();
		Lever lever = (Lever) minecart.getDisplayBlock();
		lever.setPowered(!lever.isPowered());
		minecart.setDisplayBlock(lever);
		minecart.getWorld().playSound(minecart.getLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}
	
	private static void interactWithDiode(BlockCartInteraction interaction) {
		interaction.setSuccess(true);
		Minecart minecart = interaction.getMinecart();
		Diode diode = (Diode) minecart.getDisplayBlock();
		diode.setDelay(diode.getDelay()%4 + 1);
		minecart.setDisplayBlock(diode);
	}
	
	private static void interactWithComparator(BlockCartInteraction interaction) {
		interaction.setSuccess(true);
		Minecart minecart = interaction.getMinecart();
		Comparator comparator = (Comparator) minecart.getDisplayBlock();
		comparator.setSubtractionMode(!comparator.isSubtractionMode());
		minecart.setDisplayBlock(comparator);
	}
	
	private static void interactWithTrapdoor(BlockCartInteraction interaction) {
		interaction.setSuccess(true);
		Minecart minecart = interaction.getMinecart();
		TrapDoor display = (TrapDoor) minecart.getDisplayBlock();
		display.setOpen(!display.isOpen());
		minecart.setDisplayBlock(display);
		
		Sound sound = display.isOpen() ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
		minecart.getWorld().playSound(minecart.getLocation(), sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}
	
	private static void interactWithIronTrapdoor(BlockCartInteraction interaction) {
		interaction.setSuccess(true);
		Minecart minecart = interaction.getMinecart();
		TrapDoor display = (TrapDoor) minecart.getDisplayBlock();
		display.setOpen(!display.isOpen());
		minecart.setDisplayBlock(display);
		
		Sound sound = display.isOpen() ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
		minecart.getWorld().playSound(minecart.getLocation(), sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}
	
	private static void interactWithGate(BlockCartInteraction interaction) {
		interaction.setSuccess(true);
		Minecart minecart = interaction.getMinecart();
		Gate display = (Gate) minecart.getDisplayBlock();
		display.setOpen(!display.isOpen());
		minecart.setDisplayBlock(display);
		
		Sound sound = display.isOpen() ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
		minecart.getWorld().playSound(minecart.getLocation(), sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
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
		
		Automation.instance().getActivatorModule().registerHandler(this::handleDispenser);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMinecartCollide(VehicleEntityCollisionEvent event) {
		if (!(event.getVehicle() instanceof Minecart)) return;
		Minecart minecart = (Minecart) event.getVehicle();
		
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getEntity();
		
		if (minecart.getDisplayBlock().getItemType() != Material.CACTUS) return;
		
		entity.damage(1, minecart);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMinecartClicked(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Minecart)) return;
		Minecart minecart = (Minecart) event.getRightClicked();
		
		Player player = event.getPlayer();
		ItemStack handItem = player.getInventory().getItemInMainHand();
		
		boolean cartWasEmpty = !doesMinecartHaveItem(minecart);
		
		//Handle interaction
		Action action = (player.isSneaking() ? Action.TAKE : Action.USE);
		BlockCartInteraction interaction = new BlockCartInteraction(player, minecart, handItem, action);
		handleInteraction(interaction);
		
		minecart = interaction.getMinecart();
		
		//Handle advancements
		if (interaction.isSuccess()) {
			if (action == Action.TAKE) {
				AdvancementUtil.giveAdvancement(event.getPlayer(), Bukkit.getAdvancement(new NamespacedKey(plugin, "takefromcart")));
			} else {
				if (cartWasEmpty) AdvancementUtil.giveAdvancement(event.getPlayer(), Bukkit.getAdvancement(new NamespacedKey(plugin, "addtocart")));
				else AdvancementUtil.giveAdvancement(event.getPlayer(), Bukkit.getAdvancement(new NamespacedKey(plugin, "useoncart")));
			}
		}
		
		//Cancel event if interaction succeeded, or minecart has block
		event.setCancelled(interaction.isSuccess()
				|| (minecart.getType() == EntityType.MINECART && doesMinecartHaveItem(minecart)));
		
		//Return if nothing was done
		if (!interaction.isSuccess()) return;
		
		//Remove used item from hand
		if (interaction.isItemUsed()) player.getInventory().setItemInMainHand(InventoryUtil.subtractItem(handItem));
		
		//Add returned item to hand/inventory/ground
		if (interaction.hasResult()) InventoryUtil.givePlayerItem(player, interaction.getResult());
	}
	
	private BlockCartInteraction attemptInteract(Minecart minecart, ItemStack item, Action action) {
		BlockCartInteraction interaction = new BlockCartInteraction(minecart, item, action);
		handleInteraction(interaction);
		return interaction;
	}
	
	private boolean handleDispenser(DispenserInteraction dispInteraction) {
		if (dispInteraction.getItem().getType() == Material.SHEARS) dispInteraction.validate();
		Action action = dispInteraction.getItem().getType() == Material.SHEARS ? Action.TAKE : Action.USE;
		ItemStack item = action == Action.USE ? dispInteraction.getItem() : new ItemStack(Material.AIR);
		
		//Find facing minecarts
		Collection<Minecart> minecarts = BlockUtil.getEntitiesInBlock(dispInteraction.getFacingBlock()).stream()
				.filter(entity -> entity instanceof Minecart)
				.map(entity -> (Minecart) entity)
				.collect(Collectors.toList());
		//None found, stop
		if (minecarts.isEmpty()) return false;
		dispInteraction.validate();
		
		//Iterate over minecarts, stopping on success
		BlockCartInteraction interaction = minecarts.stream()
				.map(cart -> attemptInteract(cart, item, action))
				.filter(BlockCartInteraction::isSuccess)
				.findAny().orElse(null);
		if (interaction == null) return false;
		
		//Success
		dispInteraction.setKeepItem(!interaction.isItemUsed());
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
		
		BlockCartInteraction interaction = new BlockCartInteraction(minecart, new ItemStack(Material.AIR), Action.TAKE);
		removeFromCart(interaction);
		ItemStack item = interaction.getResult();
		interaction.getMinecart().getWorld().dropItemNaturally(interaction.getMinecart().getLocation().add(0, 0.5, 0), item);
		
		if (interaction.wasMinecartChanged()) {
			//Get new minecart
			minecart = interaction.getMinecart();
			assert(minecart.getType() == EntityType.MINECART);
			
			//Re-call event
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
		if (interaction.getAction() == Action.TAKE) removeFromCart(interaction);
		else if (doesMinecartHaveItem(interaction.getMinecart())) useOnCart(interaction);
		else addToCart(interaction);
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
		minecart.getWorld().playSound(minecart.getLocation(), Sound.ENTITY_ITEMFRAME_ADD_ITEM, 0.5F, 0.75F);
		
		//Finalize stuff
		finalWork.getOrDefault(usedType, (i, m) -> {}).accept(interaction.getItem(), minecart);
		
		//Success
		interaction.setItemUsed(true);
		interaction.setSuccess(true);
	}
	
	//private void removeFromCart(Minecart minecart) {
	//	removeFromCart(new BlockCartInteraction(minecart, new ItemStack(Material.AIR), Action.REMOVE));
	//}
	private void removeFromCart(BlockCartInteraction interaction) {
		Minecart minecart = interaction.getMinecart();
		MaterialData blockData = minecart.getDisplayBlock();
		
		//Ensure minecart is not empty
		if (blockData.getItemType() == Material.AIR) return;
		
		//Remove block
		ItemStack item = takeItemFromCart(minecart);
		interaction.setResult(item);
		interaction.setSuccess(true);
		if (minecart.getType() != EntityType.MINECART) minecart = changeMinecart(interaction, RideableMinecart.class);
		minecart.setDisplayBlock(new MaterialData(Material.AIR));
		minecart.getWorld().playSound(minecart.getLocation(), Sound.ENTITY_ITEMFRAME_REMOVE_ITEM, 0.5F, 0.75F);
	}
	
	private void useOnCart(BlockCartInteraction interaction) {
		Minecart minecart = interaction.getMinecart();
		MaterialData blockData = minecart.getDisplayBlock();
		
		//Ensure minecart is not empty
		if (blockData.getItemType() == Material.AIR) return;
		
		//Run through handler
		clickFunctionality.getOrDefault(blockData.getItemType(), (i) -> {}).accept(interaction);
	}
	
	private ItemStack takeItemFromCart(Minecart minecart) {
		return itemReturnOverride.getOrDefault(minecart.getDisplayBlock().getItemType(), cart -> cart.getDisplayBlock().toItemStack(1))
				.apply(minecart);
	}
	
	private static boolean doesMinecartHaveItem(Minecart minecart) {
		return minecart.getDisplayBlock().getItemType() != Material.AIR;
	}
	
	private static <T extends Minecart> T changeMinecart(BlockCartInteraction interaction, Class<T> clazz) {
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
