package com.lesserhydra.automation;

import org.bukkit.entity.Minecart;
import org.bukkit.inventory.ItemStack;

public class BlockCartInteraction {
	
	public enum Action {ADD, REMOVE, INTERACT}
	
	private final Action action;
	private final ItemStack item;
	
	private Minecart minecart;
	private boolean changed = false;
	
	private ItemStack result = null;
	private boolean itemUsed = false;
	private boolean success = false;
	
	
	public BlockCartInteraction(Minecart minecart, ItemStack item, Action action) {
		this.minecart = minecart;
		this.action = action;
		this.item = item.clone();
	}
	
	public Minecart getMinecart() { return minecart; }
	
	public void setMinecart(Minecart minecart) {
		this.minecart = minecart;
		changed = true;
	}
	
	public boolean wasMinecartChanged() { return changed; }
	
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
