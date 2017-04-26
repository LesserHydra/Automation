package com.lesserhydra.automation.activator;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

class DispenserClickCanceler extends PacketAdapter {
	
	private Set<Block> canceledBlocks = new HashSet<>();
	
	DispenserClickCanceler(Plugin plugin) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT);
	}
	
	@Override
	public void onPacketSending(PacketEvent event) {
		PacketContainer packet = event.getPacket();
		
		@SuppressWarnings("deprecation")
		Effect effect = Effect.getById(packet.getIntegers().read(0));
		if (effect != Effect.CLICK2) return;
		
		BlockPosition pos = packet.getBlockPositionModifier().read(0);
		Location loc = pos.toLocation(event.getPlayer().getWorld());
		Block block = loc.getBlock();
		
		// Cancel dispenser clicks
		if (!canceledBlocks.remove(block)) return;
		event.setCancelled(true);
	}
	
	void cancelDispenserSound(Block block) {
		canceledBlocks.add(block);
	}
	
}
