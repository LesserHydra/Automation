package com.lesserhydra.automation.activator;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Directional;

public class DispenserInteraction {
	
	private final Dispenser dispenser;
	private final BlockFace facing;
	private final Block facingBlock;
	private final ItemStack usedItem;
	
	private boolean valid = false;
	private boolean keepItem = true;
	private boolean damageItem = false;
	private ItemStack[] addedItems = new ItemStack[0];
	

	public DispenserInteraction(Dispenser dispenser, ItemStack usedItem) {
		this.dispenser = dispenser;
		this.facing = ((Directional) dispenser.getData()).getFacing();
		this.facingBlock = dispenser.getBlock().getRelative(facing);
		this.usedItem = usedItem;
	}
	
	public void validate() {
		this.valid = true;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public Dispenser getDispenser() {
		return dispenser;
	}
	
	public BlockFace getFacing() {
		return facing;
	}
	
	public Block getFacingBlock() {
		return facingBlock;
	}
	
	public ItemStack getItem() {
		return usedItem;
	}
	
	public boolean willKeepItem() {
		return keepItem;
	}
	
	public void setKeepItem(boolean keepItem) {
		this.keepItem = keepItem;
	}
	
	public boolean willDamageItem() {
		return damageItem;
	}
	
	public void setDamageItem(boolean damageItem) {
		this.damageItem = damageItem;
	}
	
	public ItemStack[] getResults() {
		return addedItems;
	}
	
	public void setResults(ItemStack...addedItems) {
		this.addedItems = addedItems;
	}
	
}
