package com.azulc.ongakumod.network;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handlePlayDisc(final PlayDiscPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.getBlockEntity(payload.controllerPos()) instanceof AutoplayControllerBlockEntity controller) {
                    controller.tryPlayDisc(payload.slotIndex());
                }
            }
        });
    }

    public static void handleStopDisc(final StopDiscPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(payload.controllerPos()) instanceof AutoplayControllerBlockEntity controller) {
                    controller.stopAndReturnDisc();
                }
            }
        });
    }
}