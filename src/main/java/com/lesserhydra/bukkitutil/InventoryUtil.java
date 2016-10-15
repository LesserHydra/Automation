package com.lesserhydra.bukkitutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.lesserhydra.util.MapBuilder;
import com.lesserhydra.util.SetBuilder;

/**
 * A collection of misc utilities dealing with inventories and items.
 * @author Justin Lawen
 */
public class InventoryUtil {
	
	/**
	 * Checks whether of not the given inventory has room for the given item stack.
	 * Note that this assumes the item stack fits in a single empty space.
	 * @param inv The inventory
	 * @param item The item stack
	 * @return Whether the inventory has room
	 */
	public static boolean inventoryHasRoom(Inventory inv, ItemStack item) {
		int numRemaining = item.getAmount();
		ItemStack[] contents = inv.getStorageContents();
		for (ItemStack currentItem : contents) {
			if (currentItem == null || currentItem.getType() == Material.AIR) return true;
			if (!item.isSimilar(currentItem)) continue;
			
			numRemaining -= currentItem.getMaxStackSize() - currentItem.getAmount();
			if (numRemaining <= 0) return true;
		}
		return false;
	}

	public static ItemStack subtractItem(ItemStack item) {
		if (itemIsInvalid(item)) return new ItemStack(Material.AIR);
		
		int newAmount = item.getAmount() - 1;
		if (newAmount < 1) return new ItemStack(Material.AIR);
		
		ItemStack result = item.clone();
		result.setAmount(newAmount);
		return result;
	}

	public static void givePlayerItem(Player player, ItemStack item) {
		PlayerInventory inv = player.getInventory();
		ItemStack handItem = inv.getItemInMainHand();
		
		if (itemIsInvalid(handItem)) {
			inv.setItemInMainHand(item);
			return;
		}
		
		Map<Integer, ItemStack> remaining = inv.addItem(item);
		if (remaining.isEmpty()) return;
		remaining.values().forEach(remainingItem -> player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), remainingItem));
	}
	
	public static boolean itemIsValid(ItemStack item) {
		return (item != null && item.getType() != Material.AIR);
	}
	
	public static boolean itemIsInvalid(ItemStack item) {
		return (item == null || item.getType() == Material.AIR);
	}
	
	private static Set<Material> liquids = SetBuilder.init(HashSet<Material>::new)
			.add(Material.WATER)
			.add(Material.STATIONARY_WATER)
			.add(Material.LAVA)
			.add(Material.STATIONARY_LAVA)
			.buildImmutable();
	public static boolean isLiquid(Material material) {
		return liquids.contains(material);
	}
	
	private static Map<Material, Material> liquidBucketMap = MapBuilder.init(HashMap<Material, Material>::new)
			.put(Material.WATER,			Material.WATER_BUCKET)
			.put(Material.STATIONARY_WATER,	Material.WATER_BUCKET)
			.put(Material.LAVA,				Material.LAVA_BUCKET)
			.put(Material.STATIONARY_LAVA,	Material.LAVA_BUCKET)
			.buildImmutable();
	public static Material getBucket(Material liquidMaterial) {
		return liquidBucketMap.get(liquidMaterial);
	}
	
	public static boolean isLiquidBucket(Material material) {
		return (material == Material.WATER_BUCKET || material == Material.LAVA_BUCKET);
	}
	
	public static Material getLiquid(Material bucketMaterial) {
		if (bucketMaterial == Material.WATER_BUCKET) return Material.WATER;
		if (bucketMaterial == Material.LAVA_BUCKET) return Material.LAVA;
		throw new IllegalArgumentException("Unknown bucket material: " + bucketMaterial);
	}
	
	private static Random rand = new Random();
	public static ItemStack damageItem(ItemStack item) {
		int unbreaking = item.getEnchantmentLevel(Enchantment.DURABILITY);
		if (rand.nextDouble() >= 1.0/(unbreaking+1)) return item;
		
		short durability = (short) (item.getDurability() + 1);
		if (durability > item.getType().getMaxDurability()) return new ItemStack(Material.AIR);
		ItemStack result = item.clone();
		result.setDurability(durability);
		return result;
	}
	
	public static int getSlot(Inventory inventory, ItemStack item) {
		ItemStack[] contents = inventory.getContents();
		for (int i = 0; i < contents.length; i++) {
			if (!item.equals(contents[i])) continue;
			return i;
		}
		return -1;
	}
	
}
