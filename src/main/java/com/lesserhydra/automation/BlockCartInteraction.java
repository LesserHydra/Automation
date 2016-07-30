package com.lesserhydra.automation;

import org.bukkit.inventory.ItemStack;

public class BlockCartInteraction {
	
	public enum Action {ADD, REMOVE}
	
	private final Action action;
	private final ItemStack item;
	
	private ItemStack result;
	private boolean itemUsed;
	private boolean success;
	
	
	public BlockCartInteraction(ItemStack item, Action action) {
		this.action = action;
		this.item = item.clone();
		
		this.result = null;
		this.itemUsed = false;
		this.success = false;
	}
	
	public Action getAction() {
		return action;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public boolean hasResult() {
		return result != null;
	}
	
	public ItemStack getResult() {
		return result;
	}
	
	public void setResult(ItemStack result) {
		this.result = result;
	}
	
	public boolean isItemUsed() {
		return itemUsed;
	}
	
	public void setItemUsed(boolean usedItem) {
		this.itemUsed = usedItem;
	}

	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}

}
