package com.lesserhydra.automation;

import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryMoveItemAutocrafterEvent extends InventoryMoveItemEvent {
	public InventoryMoveItemAutocrafterEvent(Inventory sourceInventory, ItemStack itemStack, Inventory autocrafterInventory) {
		super(sourceInventory, itemStack, autocrafterInventory, true);
	}
}
