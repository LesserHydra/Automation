package com.lesserhydra.automation.volatilecode;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.inventory.InventoryView;
import net.minecraft.server.v1_10_R1.Container;
import net.minecraft.server.v1_10_R1.CraftingManager;
import net.minecraft.server.v1_10_R1.EntityHuman;
import net.minecraft.server.v1_10_R1.IRecipe;
import net.minecraft.server.v1_10_R1.InventoryCraftResult;
import net.minecraft.server.v1_10_R1.InventoryCrafting;
import net.minecraft.server.v1_10_R1.ItemStack;
import net.minecraft.server.v1_10_R1.World;

public class Crafter {
	
	public static List<org.bukkit.inventory.ItemStack> craft(org.bukkit.inventory.ItemStack[] contents, org.bukkit.World bukkitWorld) {
		//Create inventory
		InventoryCrafting invCraft = new InventoryCrafting(new Container() {
			@Override
			public InventoryView getBukkitView() {
				//TODO: How???
				return null;
			}
			@Override
			public boolean a(EntityHuman arg0) {
				//Whether the given entity can reach this container?
				return false;
			}
		}, 3, 3);
		invCraft.resultInventory = new InventoryCraftResult();
		for (int i = 0; i < 9; i++) {
			invCraft.setItem(i, CraftItemStack.asNMSCopy(contents[i]));
		}
		
		//Find recipe
		World world = ((CraftWorld) bukkitWorld).getHandle();
		Optional<IRecipe> optionalRecipe = CraftingManager.getInstance().getRecipes().stream()
				.filter(recipe -> recipe.a(invCraft, world)) //OBF: Recipe matches
				.findAny();
		if (!optionalRecipe.isPresent()) return Collections.emptyList();
		IRecipe recipe = optionalRecipe.get();
		
		//Do crafting
		ItemStack craftingResult = recipe.craftItem(invCraft); //Gets result, does not use items
		//TODO: Should this send the pre-crafting event?
		
		//Build results list
		List<org.bukkit.inventory.ItemStack> results = new LinkedList<>();
		results.add(CraftItemStack.asBukkitCopy(craftingResult));
		Arrays.stream(recipe.b(invCraft)) //OBF: Get inventory remainder
				.filter(Objects::nonNull)
				.map(CraftItemStack::asBukkitCopy)
				.forEach(results::add);
		
		return results;
	}
	
}
