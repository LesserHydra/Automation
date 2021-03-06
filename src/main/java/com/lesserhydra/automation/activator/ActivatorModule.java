package com.lesserhydra.automation.activator;

import com.comphenix.protocol.ProtocolLibrary;
import com.lesserhydra.automation.Automation;
import com.lesserhydra.automation.Module;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.bukkitutil.InventoryUtil;
import com.lesserhydra.util.EnumMapPriorityView;
import com.lesserhydra.util.MapBuilder;
import com.lesserhydra.util.PriorityView;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.Cauldron;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Directional;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wood;
import org.bukkit.material.Wool;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

//TODO: Add proper unregistering
//TODO: Allow general handlers to overide specialty, with regards to priority

//TODO: Sound for all the things
//TODO: Sound pitch variations

public class ActivatorModule implements Module, Listener {
	
	private static final Random rand = new Random();
	
	private final Map<Material, PriorityView<Priority, InteractionHandler>> typeHandlersMap = new EnumMap<>(Material.class);
	private final PriorityView<Priority, InteractionHandler> generalHandlers = new EnumMapPriorityView<>(Priority.class);
	
	private final Automation plugin;
	private DispenserClickCanceler clickCanceler;
	
	public ActivatorModule(Automation plugin) { this.plugin = plugin; }
	
	public void init() {
		clickCanceler = new DispenserClickCanceler(plugin);
		ProtocolLibrary.getProtocolManager().addPacketListener(clickCanceler);
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		registerHandler(this::handleBlockPlacing, Priority.LAST);
		registerHandler(Material.WOOD_PICKAXE, this::handleBlockBreaking);
		registerHandler(Material.STONE_PICKAXE, this::handleBlockBreaking);
		registerHandler(Material.GOLD_PICKAXE, this::handleBlockBreaking);
		registerHandler(Material.IRON_PICKAXE, this::handleBlockBreaking);
		registerHandler(Material.DIAMOND_PICKAXE, this::handleBlockBreaking);
		
		registerHandler(Material.INK_SACK, this::handleDying);
		registerHandler(Material.SHEARS, this::handleShearing);
		registerHandler(Material.BUCKET, this::handleMilking);
		registerHandler(Material.BOWL, this::handleSoupMilking);
		registerHandler(Material.SHEARS, this::handleShearingMooshroom);
		
		registerHandler(this::handleItemFramePlacing);
		registerHandler(Material.SHEARS, this::handleItemFrameRemoving);
		
		registerHandler(Material.GLASS_BOTTLE, this::handleFillBottle);
		
		registerHandler(Material.WATER_BUCKET, this::handleCauldronPut);
		registerHandler(Material.BUCKET, this::handleCauldronTake);
		registerHandler(Material.POTION, this::handleCauldronPutSome);
		registerHandler(Material.GLASS_BOTTLE, this::handleCauldronTakeSome);
		
		registerHandler(Material.SEEDS, this::handleSeedPlanting);
		registerHandler(Material.BEETROOT_SEEDS, this::handleSeedPlanting);
		registerHandler(Material.PUMPKIN_SEEDS, this::handleSeedPlanting);
		registerHandler(Material.MELON_SEEDS, this::handleSeedPlanting);
		registerHandler(Material.POTATO_ITEM, this::handleSeedPlanting);
		registerHandler(Material.CARROT_ITEM, this::handleSeedPlanting);
		registerHandler(Material.NETHER_STALK, this::handleNetherwartPlanting);
		registerHandler(Material.INK_SACK, this::handleCocoaPlanting);
	}
	
	@Override
	public void deinit() {
		HandlerList.unregisterAll(this);
		ProtocolLibrary.getProtocolManager().removePacketListener(clickCanceler);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDispenserFire(BlockDispenseEvent event) {
		if (!(event.getBlock().getState() instanceof Dispenser)) return;
		
		Dispenser dispenser = (Dispenser) event.getBlock().getState();
		ItemStack item = event.getItem();
		
		//Construct and handle interaction
		DispenserInteraction interaction = new DispenserInteraction(dispenser, item);
		boolean success = handleInteraction(interaction);
		
		//For valid interactions (mainly matching tools), cancel event even if unsuccessful
		if (!interaction.isValid()) return;
		event.setCancelled(true);
		cancelDispenserSound(event.getBlock());
		
		//Apply interaction
		if (!success) return;
		Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> {
			handleItem(dispenser, interaction);
			handleAddedItems(dispenser, interaction);
		}, 0L);
	}
	
	public void cancelDispenserSound(Block block) { clickCanceler.cancelDispenserSound(block); }
	
	private boolean handleInteraction(DispenserInteraction interaction) {
		PriorityView<Priority, InteractionHandler> typeHandlers = typeHandlersMap.get(interaction.getItem().getType());
		
		//Check general handlers
		for (InteractionHandler handler : generalHandlers) {
			if (handler.handleInteraction(interaction)) return true;
		}
		
		//Check type specific handlers
		if (typeHandlers != null) {
			for (InteractionHandler handler : typeHandlers) {
				if (handler.handleInteraction(interaction)) return true;
			}
		}
		
		//None succeeded
		return false;
	}
	
	private void handleItem(Dispenser dispenser, DispenserInteraction interaction) {
		//Remove item
		if (!interaction.willKeepItem()) {
			dispenser.getInventory().removeItem(interaction.getItem());
			return;
		}
		
		//Damage item
		if (!interaction.willDamageItem()) return;
		ItemStack damagedItem = InventoryUtil.damageItem(interaction.getItem());
		if (damagedItem.getType() == Material.AIR) dispenser.getWorld().playSound(
				dispenser.getLocation().add(0.5, 0.5, 0.5), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
		int i = InventoryUtil.getSlot(dispenser.getInventory(), interaction.getItem());
		if (i >= 0) dispenser.getInventory().setItem(i, damagedItem);
	}
	
	private void handleAddedItems(Dispenser dispenser, DispenserInteraction interaction) {
		Map<Integer, ItemStack> remaining = dispenser.getInventory().addItem(interaction.getResults());
		if (!remaining.isEmpty()) remaining.values()
				.forEach(remainingItem -> interaction.getFacingBlock().getWorld().dropItemNaturally(interaction.getFacingBlock().getLocation().add(0.5, 0.5, 0.5), remainingItem));
	}
	
	@SuppressWarnings("WeakerAccess")
	public void registerHandler(InteractionHandler handler) {
		registerHandler(handler, Priority.NORMAL);
	}
	
	@SuppressWarnings("WeakerAccess")
	public void registerHandler(InteractionHandler handler, Priority priority) {
		generalHandlers.add(priority, handler);
	}
	
	@SuppressWarnings("WeakerAccess")
	public void registerHandler(Material toolType, InteractionHandler handler) {
		registerHandler(toolType, handler, Priority.NORMAL);
	}
	
	@SuppressWarnings("WeakerAccess")
	public void registerHandler(Material toolType, InteractionHandler handler, Priority priority) {
		PriorityView<Priority, InteractionHandler> handlerList =
				typeHandlersMap.computeIfAbsent(toolType, k -> new EnumMapPriorityView<>(Priority.class));
		handlerList.add(priority, handler);
	}
	
	@SuppressWarnings("WeakerAccess")
	public void unregisterHandler(InteractionHandler handler) {
		generalHandlers.remove(handler);
	}
	
	@SuppressWarnings("WeakerAccess")
	public void unregisterHandler(Material toolType, InteractionHandler handler) {
		PriorityView<Priority, InteractionHandler> handlerList = typeHandlersMap.get(toolType);
		if (handlerList == null) return;
		handlerList.remove(handler);
		if (handlerList.isEmpty()) typeHandlersMap.remove(toolType);
	}
	
	
	/*---------------Interaction Handlers---------------*/
	private static final Map<Material, Material> itemMaterialMappings =
			MapBuilder.init(HashMap<Material, Material>::new)
					.put(Material.CAKE, Material.CAKE_BLOCK)
					.put(Material.CAULDRON_ITEM, Material.CAULDRON)
					.put(Material.BREWING_STAND_ITEM, Material.BREWING_STAND)
					.buildImmutable();
	
	/*
	 * Places any block item as long as space in front it air
	 */
	private boolean handleBlockPlacing(DispenserInteraction interaction) {
		Material itemType = interaction.getItem().getType();
		if (!itemType.isBlock() && !itemMaterialMappings.containsKey(itemType)) return false;
		
		//Skip items with block meta
		if (interaction.getItem().getItemMeta() instanceof BlockStateMeta
				&& ((BlockStateMeta)interaction.getItem().getItemMeta()).hasBlockState()) return false;
		
		interaction.validate();
		if (interaction.getFacingBlock().getType() != Material.AIR) return false;
		
		placeBlock(interaction.getFacingBlock(), interaction.getItem(), interaction.getFacing());
		
		interaction.setKeepItem(false);
		return true;
	}
	
	/*
	 * Breaks the block in front with a pick
	 */
	private boolean handleBlockBreaking(DispenserInteraction interaction) {
		interaction.validate();
		if (interaction.getFacingBlock().getType() == Material.AIR) return false;
		if (!canBreakBlock(interaction.getFacingBlock(), interaction.getItem())) return false;
		
		boolean instant = BlockUtil.isInstantlyBreakable(interaction.getFacingBlock().getType());
		ItemStack[] results = breakBlock(interaction.getFacingBlock(), interaction.getItem());
		if (!instant) interaction.setDamageItem(true);
		interaction.setResults(results);
		return true;
	}
	
	/*
	 * Shears sheep
	 */
	private boolean handleShearing(DispenserInteraction interaction) {
		interaction.validate();
		
		//Find an adult unsheared sheep
		Sheep sheep = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.SHEEP)
				.map(entity -> (Sheep) entity)
				.filter(Sheep::isAdult)
				.filter(sheeps -> !sheeps.isSheared())
				.findAny().orElse(null);
		if (sheep == null) return false;
		
		//Shear sheep
		sheep.getWorld().playSound(sheep.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0F, 1.0F);
		sheep.setSheared(true);
		
		//Results
		Wool wool = new Wool(sheep.getColor());
		interaction.setDamageItem(true);
		interaction.setResults(wool.toItemStack(1 + rand.nextInt(3)));
		return true;
	}
	
	/*
	 * Shears mooshrooms
	 */
	private boolean handleShearingMooshroom(DispenserInteraction interaction) {
		interaction.validate();
		
		//Find an adult mooshroom
		MushroomCow mushCow = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.MUSHROOM_COW)
				.map(entity -> (MushroomCow) entity)
				.filter(Cow::isAdult)
				.findAny().orElse(null);
		if (mushCow == null) return false;
		
		//Despawn mooshroom
		mushCow.remove();
		
		//Effects
		mushCow.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, mushCow.getLocation().add(0, 0.45, 0), 1);
		mushCow.getWorld().playSound(mushCow.getLocation(), Sound.ENTITY_MOOSHROOM_SHEAR, 1.0F, 1.0F);
		
		//Spawn normal cow
		Cow cow = mushCow.getWorld().spawn(mushCow.getLocation(), Cow.class);
		cow.setHealth(mushCow.getHealth());
		cow.setCustomName(mushCow.getCustomName());
		
		//Results
		interaction.setDamageItem(true);
		interaction.setResults(new ItemStack(Material.RED_MUSHROOM, 5)); //Five red mushrooms
		return true;
	}
	
	/*
	 * Dyes sheep
	 */
	private boolean handleDying(DispenserInteraction interaction) {
		//Keep from overriding bonemealing
		if (!interaction.getItem().getData().equals(new Dye(DyeColor.WHITE))) interaction.validate();
		
		//Find an adult sheep
		Sheep sheep = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.SHEEP)
				.map(entity -> (Sheep) entity)
				.filter(Sheep::isAdult)
				.findAny().orElse(null);
		if (sheep == null) return false;
		
		interaction.validate();
		
		//Dye sheep
		sheep.setColor(((Dye)interaction.getItem().getData()).getColor());
		
		//Results
		interaction.setKeepItem(false);
		return true;
	}
	
	/*
	 * Milks cows and mooshrooms
	 */
	@SuppressWarnings("deprecation")
	private boolean handleMilking(DispenserInteraction interaction) {
		//Keep from overriding vanilla behavior
		Block facingBlock = interaction.getFacingBlock();
		if (facingBlock.isLiquid() && facingBlock.getData() == 0) return false;
		
		interaction.validate();
		
		//Find an adult cow
		Cow cow = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.COW || entity.getType() == EntityType.MUSHROOM_COW)
				.map(entity -> (Cow) entity)
				.filter(Cow::isAdult)
				.findAny().orElse(null);
		if (cow == null) return false;
		
		//Sound
		cow.getWorld().playSound(cow.getLocation(), Sound.ENTITY_COW_MILK, 1.0F, 1.0F);
		
		//Results
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.MILK_BUCKET));
		return true;
	}
	
	/*
	 * Gets soup from mooshroom
	 */
	private boolean handleSoupMilking(DispenserInteraction interaction) {
		interaction.validate();
		
		//Get an adult mooshroom
		MushroomCow cow = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.MUSHROOM_COW)
				.map(entity -> (MushroomCow) entity)
				.filter(Cow::isAdult)
				.findAny().orElse(null);
		if (cow == null) return false;
		
		//Results
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.MUSHROOM_SOUP));
		return true;
	}
	
	/*
	 * Places items in item frame
	 */
	private boolean handleItemFramePlacing(DispenserInteraction interaction) {
		//Get an item frame opposite dispenser
		ItemFrame itemFrame = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.ITEM_FRAME)
				.map(entity -> (ItemFrame) entity)
				.filter(frame -> frame.getAttachedFace() == interaction.getFacing())
				.findAny().orElse(null);
		if (itemFrame == null) return false;
		
		interaction.validate();
		
		//Stop if item in frame
		if (itemFrame.getItem().getType() != Material.AIR) return false;
		
		//Put item in frame
		itemFrame.setItem(interaction.getItem());
		itemFrame.getWorld().playSound(itemFrame.getLocation(), Sound.ENTITY_ITEMFRAME_ADD_ITEM, 1.0F, 1.0F);
		
		//Results
		interaction.setKeepItem(false);
		return true;
	}
	
	/*
	 * Takes items from item frame
	 */
	private boolean handleItemFrameRemoving(DispenserInteraction interaction) {
		interaction.validate();
		
		//Get an item frame opposite dispenser
		ItemFrame itemFrame = BlockUtil.getEntitiesInBlock(interaction.getFacingBlock()).stream()
				.filter(entity -> entity.getType() == EntityType.ITEM_FRAME)
				.map(entity -> (ItemFrame) entity)
				.filter(frame -> frame.getAttachedFace() == interaction.getFacing())
				.findAny().orElse(null);
		if (itemFrame == null) return false;
		
		//Stop if no item in frame
		if (itemFrame.getItem().getType() == Material.AIR) return false;
		
		//Take item from frame
		ItemStack frameItem = itemFrame.getItem();
		itemFrame.setItem(new ItemStack(Material.AIR));
		itemFrame.getWorld().playSound(itemFrame.getLocation(), Sound.ENTITY_ITEMFRAME_REMOVE_ITEM, 1.0F, 1.0F);
		
		//Results
		interaction.setResults(frameItem);
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean handleFillBottle(DispenserInteraction interaction) {
		if (interaction.getFacingBlock().getType() != Material.STATIONARY_WATER
				&& interaction.getFacingBlock().getType() != Material.WATER) return false;
		interaction.validate();
		
		if (interaction.getFacingBlock().getData() != 0) return false;
		
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.POTION, 1));
		interaction.getDispenser().getWorld().playSound(interaction.getDispenser().getLocation(),
				Sound.ITEM_BOTTLE_FILL, 1.0F, 1.0F);
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean handleCauldronPut(DispenserInteraction interaction) {
		if (interaction.getFacingBlock().getType() != Material.CAULDRON) return false;
		interaction.validate();
		
		BlockState state = interaction.getFacingBlock().getState();
		Cauldron cauldron = (Cauldron) state.getData();
		if (cauldron.isFull()) return false;
		
		cauldron.setData((byte) 3);
		state.setData(cauldron);
		state.update();
		
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.BUCKET, 1));
		interaction.getDispenser().getWorld().playSound(interaction.getDispenser().getLocation(),
				Sound.ITEM_BUCKET_EMPTY, 1.0F, 1.0F);
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean handleCauldronTake(DispenserInteraction interaction) {
		if (interaction.getFacingBlock().getType() != Material.CAULDRON) return false;
		interaction.validate();
		
		BlockState state = interaction.getFacingBlock().getState();
		Cauldron cauldron = (Cauldron) state.getData();
		if (!cauldron.isFull()) return false;
		
		cauldron.setData((byte) 0);
		state.setData(cauldron);
		state.update();
		
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.WATER_BUCKET, 1));
		interaction.getDispenser().getWorld().playSound(interaction.getDispenser().getLocation(),
				Sound.ITEM_BUCKET_FILL, 1.0F, 1.0F);
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean handleCauldronPutSome(DispenserInteraction interaction) {
		if (interaction.getFacingBlock().getType() != Material.CAULDRON) return false;
		if (((PotionMeta)interaction.getItem().getItemMeta()).hasCustomEffects()) return false;
		interaction.validate();
		
		BlockState state = interaction.getFacingBlock().getState();
		Cauldron cauldron = (Cauldron) state.getData();
		if (cauldron.isFull()) return false;
		
		cauldron.setData((byte) (cauldron.getData() + 1));
		state.setData(cauldron);
		state.update();
		
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.GLASS_BOTTLE, 1));
		interaction.getDispenser().getWorld().playSound(interaction.getDispenser().getLocation(),
				Sound.ITEM_BOTTLE_EMPTY, 1.0F, 1.0F);
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean handleCauldronTakeSome(DispenserInteraction interaction) {
		if (interaction.getFacingBlock().getType() != Material.CAULDRON) return false;
		interaction.validate();
		
		BlockState state = interaction.getFacingBlock().getState();
		Cauldron cauldron = (Cauldron) state.getData();
		if (cauldron.isEmpty()) return false;
		
		cauldron.setData((byte) (cauldron.getData() - 1));
		state.setData(cauldron);
		state.update();
		
		interaction.setKeepItem(false);
		interaction.setResults(new ItemStack(Material.POTION, 1));
		interaction.getDispenser().getWorld().playSound(interaction.getDispenser().getLocation(),
				Sound.ITEM_BOTTLE_FILL, 1.0F, 1.0F);
		
		return true;
	}
	
	/*
	 * Plants seeds
	 */
	private boolean handleSeedPlanting(DispenserInteraction interaction) {
		interaction.validate();
		
		if (interaction.getFacingBlock().getType() != Material.AIR) return false;
		
		Block farmland = interaction.getFacingBlock().getRelative(BlockFace.DOWN);
		if (farmland.getType() != Material.SOIL) return false;
		
		interaction.getFacingBlock().setType(getPlantBlockFromSeeds(interaction.getItem().getType()));
		
		interaction.setKeepItem(false);
		return true;
	}
	
	/*
	 * Plants netherwart
	 */
	private boolean handleNetherwartPlanting(DispenserInteraction interaction) {
		interaction.validate();
		
		if (interaction.getFacingBlock().getType() != Material.AIR) return false;
		
		Block farmland = interaction.getFacingBlock().getRelative(BlockFace.DOWN);
		if (farmland.getType() != Material.SOUL_SAND) return false;
		
		interaction.getFacingBlock().setType(Material.NETHER_WARTS);
		
		interaction.setKeepItem(false);
		return true;
	}
	
	/*
	 * Plants cocoa pods
	 */
	private boolean handleCocoaPlanting(DispenserInteraction interaction) {
		if (!interaction.getItem().getData().equals(new Dye(DyeColor.BROWN))) return false;
		interaction.validate();
		
		if (interaction.getFacingBlock().getType() != Material.AIR) return false;
		
		BlockFace logFace = getValidCocoaTree(interaction.getFacingBlock(), interaction.getFacing());
		if (logFace == null) return false;
		
		BlockState plantBlockState = interaction.getFacingBlock().getState();
		plantBlockState.setType(Material.COCOA);
		plantBlockState.setData(new CocoaPlant(CocoaPlant.CocoaPlantSize.SMALL, logFace));
		plantBlockState.update(true);
		
		interaction.setKeepItem(false);
		return true;
	}
	
	/*----------------------------------------*/
	
	
	private final Map<Material, Material> seedMap = MapBuilder.init(HashMap<Material, Material>::new)
			.put(Material.SEEDS, Material.CROPS)
			.put(Material.POTATO_ITEM, Material.POTATO)
			.put(Material.CARROT_ITEM, Material.CARROT)
			.put(Material.BEETROOT_SEEDS, Material.BEETROOT_BLOCK)
			.put(Material.MELON_SEEDS, Material.MELON_STEM)
			.put(Material.PUMPKIN_SEEDS, Material.PUMPKIN_STEM)
			.buildImmutable();
	private Material getPlantBlockFromSeeds(Material seedMaterial) {
		return seedMap.get(seedMaterial);
	}
	
	private BlockFace getValidCocoaTree(Block plantingBlock, BlockFace preferred) {
		if (preferred != BlockFace.UP && preferred != BlockFace.DOWN
				&& isJungleLog(plantingBlock.getRelative(preferred))) return preferred;
		return Stream.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
				.filter(dir -> isJungleLog(plantingBlock.getRelative(dir)))
				.findAny()
				.orElse(null);
	}
	
	private boolean isJungleLog(Block block) {
		return block.getType() == Material.LOG
				&& ((Wood) block.getState().getData()).getSpecies() == TreeSpecies.JUNGLE;
	}
	
	private void placeBlock(Block block, ItemStack blockItem, BlockFace facing) {
		Material blockType = itemMaterialMappings.getOrDefault(blockItem.getType(), blockItem.getType());
		
		//Play sound
		BlockUtil.getSound(blockType).getPlaceEffect().play(block.getLocation());
		
		//Place block
		BlockState blockState = block.getState();
		blockState.setType(blockType);
		if (blockItem.getType() == blockType) {
			MaterialData newData = blockItem.getData();
			if (newData instanceof Directional) ((Directional) newData).setFacingDirection(facing);
			if (newData instanceof Tree)
				((Tree) newData).setDirection(facing); //Tree does not implement directional, for whatever reason...
			//FIXME: Anvil rotation
			blockState.setData(newData);
		}
		blockState.update(true);
	}
	
	private boolean canBreakBlock(Block block, ItemStack tool) {
		return !BlockUtil.isUnbreakable(block.getType())
				&& BlockUtil.blockCanBeBrokenByTool(block, tool);
	}
	
	private ItemStack[] breakBlock(Block block, ItemStack tool) {
		//Play sound and particles
		block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType()); //FIXME: Doesn't take data into account
		
		//TODO: Implement in BlockBreaking.getDrops() instead
		if (block.getState() instanceof ShulkerBox) {
			ItemStack result = new ItemStack(block.getType());
			BlockStateMeta meta = (BlockStateMeta) result.getItemMeta();
			ShulkerBox box = (ShulkerBox) block.getState();
			meta.setBlockState(box);
			meta.setDisplayName(box.getCustomName());
			result.setItemMeta(meta);
			
			block.setType(Material.AIR);
			return new ItemStack[]{result};
		}
		
		ItemStack[] results = BlockUtil.getDrops(block, tool).toArray(new ItemStack[0]);
		block.setType(Material.AIR);
		return results;
	}
	
}
