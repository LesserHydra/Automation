package com.lesserhydra.automation.activator;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Directional;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;
import com.lesserhydra.automation.Automation;
import com.lesserhydra.automation.volatilecode.BlockBreaking;
import com.lesserhydra.automation.volatilecode.BlockSoundType;
import com.lesserhydra.bukkitutil.BlockUtil;
import com.lesserhydra.bukkitutil.InventoryUtil;
import com.lesserhydra.util.EnumMapPriorityView;
import com.lesserhydra.util.PriorityView;

//TODO: Add proper unregistering
//TODO: Allow general handlers to overide specialty, with regards to priority
public class ActivatorListener implements Listener {
	
	private static final Random rand = new Random();
	
	private final Map<Material, PriorityView<Priority, InteractionHandler>> typeHandlersMap = new EnumMap<>(Material.class);
	private final PriorityView<Priority, InteractionHandler> generalHandlers = new EnumMapPriorityView<>(Priority.class);
	
	
	public void init() {
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
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDispenserFire(BlockDispenseEvent event) {
		if (!(event.getBlock().getState() instanceof Dispenser)) return;
		
		Dispenser dispenser = (Dispenser) event.getBlock().getState();
		ItemStack item = event.getItem();
		
		//Construct and handle interaction
		DispenserInteraction interaction = new DispenserInteraction(dispenser, item);
		boolean success = handleInteraction(interaction);
		
		//For valid interactions (mainly matching tools), cancel event even is unsuccessful
		if (!interaction.isValid()) return;
		event.setCancelled(true);
		
		//Apply interaction
		if (!success) return;
		Bukkit.getScheduler().runTaskLater(Automation.instance(), () -> {
			handleItem(dispenser, interaction);
			handleAddedItems(dispenser, interaction);
		}, 0L);
	}
	
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
		if (!remaining.isEmpty()) remaining.values().stream()
				.forEach(remainingItem -> interaction.getFacingBlock().getWorld().dropItemNaturally(interaction.getFacingBlock().getLocation().add(0.5, 0.5, 0.5), remainingItem));
	}
	
	public void registerHandler(InteractionHandler handler) {
		registerHandler(handler, Priority.NORMAL);
	}
	
	public void registerHandler(InteractionHandler handler, Priority priority) {
		generalHandlers.add(priority, handler);
	}
	
	public void registerHandler(Material toolType, InteractionHandler handler) {
		registerHandler(toolType, handler, Priority.NORMAL);
	}
	
	public void registerHandler(Material toolType, InteractionHandler handler, Priority priority) {
		PriorityView<Priority, InteractionHandler> handlerList = typeHandlersMap.get(toolType);
		if (handlerList == null) {
			handlerList = new EnumMapPriorityView<>(Priority.class);
			typeHandlersMap.put(toolType, handlerList);
		}
		handlerList.add(priority, handler);
	}
	
	public void unregisterHandler(InteractionHandler handler) {
		generalHandlers.remove(handler);
	}
	
	public void unregisterHandler(Material toolType, InteractionHandler handler) {
		PriorityView<Priority, InteractionHandler> handlerList = typeHandlersMap.get(toolType);
		if (handlerList == null) return;
		handlerList.remove(handler);
		if (handlerList.isEmpty()) typeHandlersMap.remove(toolType);
	}
	
	
	/*---------------Interaction Handlers---------------*/
	
	/*
	 * Places any block item as long as space in front it air
	 */
	private boolean handleBlockPlacing(DispenserInteraction interaction) {
		if (!interaction.getItem().getType().isBlock()) return false;
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
		
		boolean instant = BlockBreaking.isInstantlyBreakable(interaction.getFacingBlock());
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
		
		//Five red mushrooms
		ItemStack resultItems = new ItemStack(Material.RED_MUSHROOM);
		resultItems.setAmount(5);
		
		//Results
		interaction.setDamageItem(true);
		interaction.setResults(resultItems);
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
		interaction.setDamageItem(true);
		return true;
	}
	/*----------------------------------------*/
	
	
	private void placeBlock(Block block, ItemStack blockItem, BlockFace facing) {
		//Play sound
		BlockSoundType.fromType(blockItem.getType()).getPlaceEffect().play(block.getLocation());
		
		//Place block
		BlockState blockState = block.getState();
		blockState.setType(blockItem.getType());
		MaterialData newData = blockItem.getData();
		if (newData instanceof Directional) ((Directional) newData).setFacingDirection(facing);
		if (newData instanceof Tree) ((Tree) newData).setDirection(facing); //Tree does not implement directional, for whatever reason...
		//FIXME: Anvil rotation
		blockState.setData(newData);
		blockState.update(true);
	}
	
	private boolean canBreakBlock(Block block, ItemStack tool) {
		if (BlockBreaking.isUnbreakable(block)) return false;
		if (!BlockUtil.blockCanBeBrokenByTool(block, tool)) return false;
		return true;
	}
	
	private ItemStack[] breakBlock(Block block, ItemStack tool) {
		//Play sound and particles
		block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType()); //FIXME: Doesn't take data into account
		ItemStack[] results = BlockBreaking.getDrops(block, tool).toArray(new ItemStack[0]);
		block.setType(Material.AIR);
		return results;
	}
	
}
