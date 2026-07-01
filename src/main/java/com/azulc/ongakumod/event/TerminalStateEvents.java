package com.azulc.ongakumod.event;

import java.util.Optional;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.item.TerminalBlockItem; // Adjust to your actual item class name
import com.azulc.ongakumod.util.TerminalControlHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

@EventBusSubscriber(modid = OngakuMod.MODID)
public class TerminalStateEvents {

    // 1. STOP AUDIO WHEN TERMINAL ITEM IS DROPPED
    @SubscribeEvent
    public static void onItemDrop(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer) 
        {
            ItemStack thrownItem = event.getEntity().getItem();
            if (thrownItem.getItem() instanceof TerminalBlockItem terminal) 
            {    
                for (ServerPlayer _player : event.getPlayer().level().getServer().getPlayerList().getPlayers()) {//
                    TerminalControlHandler.broadcastToTerminalOffline(_player, terminal.getNetworkId(thrownItem), Optional.empty(), true, false,Optional.empty());
                }
            }
        }
    }
}