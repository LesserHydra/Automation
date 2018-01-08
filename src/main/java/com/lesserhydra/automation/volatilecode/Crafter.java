package com.lesserhydra.automation.volatilecode;

import com.lesserhydra.bukkitutil.nms.NMSTileEntityUtil;
import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.Container;
import net.minecraft.server.v1_12_R1.CraftingManager;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.IRecipe;
import net.minecraft.server.v1_12_R1.InventoryCraftResult;
import net.minecraft.server.v1_12_R1.InventoryCrafting;
import net.minecraft.server.v1_12_R1.Item;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class Crafter {
	
	public static int getMissingStack(org.bukkit.block.Dispenser dispenser, org.bukkit.inventory.ItemStack missingItem) {
		ItemStack nmsMissing = CraftItemStack.asNMSCopy(missingItem);
		List<ItemStack> contents = NMSTileEntityUtil.getTileEntity((CraftDispenser)dispenser).getContents();
		for (int i = 0; i < 9; ++i) {
			ItemStack stack = contents.get(i);
			if (stack.getCount() == 0 && stack.doMaterialsMatch(nmsMissing)) return i;
		}
		return -1;
	}
	
	public static List<org.bukkit.inventory.ItemStack> craft(org.bukkit.inventory.ItemStack[] contents, org.bukkit.World bukkitWorld) {
		//Create inventory
		InventoryCrafting invCraft = new InventoryCrafting(new Container() {
			@Override
			public InventoryView getBukkitView() {
				//TODO: How?
				return null;
			}
			@Override
			public boolean canUse(EntityHuman entityHuman) {
				return false;
			}
		}, 3, 3);
		invCraft.resultInventory = new InventoryCraftResult();
		for (int i = 0; i < 9; i++) {
			invCraft.setItem(i, CraftItemStack.asNMSCopy(contents[i]));
		}
		
		//Find recipe
		World world = ((CraftWorld) bukkitWorld).getHandle();
		//TODO: Sure, it may work, but shouldn't there be a better impl? Spliterator is defaulted.
		Optional<IRecipe> optionalRecipe = StreamSupport.stream(CraftingManager.recipes.spliterator(), false)
				.filter(recipe -> recipe.a(invCraft, world)) //OBF: Line 21, Recipe matches
				.findAny();
		if (!optionalRecipe.isPresent()) return Collections.emptyList();
		IRecipe recipe = optionalRecipe.get();
		
      	//Set result from recipe
		invCraft.resultInventory.setItem(0, recipe.craftItem(invCraft)); //Gets result, does not use items
  
		//Run through Bukkit PrepareItemCraftEvent
		CraftingInventory craftingInventory = new CraftInventoryCrafting(invCraft, invCraft.resultInventory);
		PrepareItemCraftEvent prepareEvent = new PrepareItemCraftEvent(craftingInventory, null, false);
		Bukkit.getPluginManager().callEvent(prepareEvent);
		ItemStack craftingResult = invCraft.resultInventory.getItem(0);
		
		//TODO: Run through Bukkit CraftItemEvent?
  
		//Cancel if no result
		if (craftingResult.getItem() == Item.getItemOf(Blocks.AIR)) return Collections.emptyList();
		
		//Build results list
		List<org.bukkit.inventory.ItemStack> results = new LinkedList<>();
		results.add(CraftItemStack.asBukkitCopy(craftingResult));
		recipe.b(invCraft).stream() //OBF: Line 27, Get inventory remainder
				.filter(Objects::nonNull)
				.map(CraftItemStack::asBukkitCopy)
				.filter(item -> item.getType() != Material.AIR)
				.forEach(results::add);
		
		return results;
	}
	
}
