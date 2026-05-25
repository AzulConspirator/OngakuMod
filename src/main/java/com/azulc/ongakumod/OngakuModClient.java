package com.azulc.ongakumod;

import com.azulc.ongakumod.client.DiscRackRenderer;
import com.azulc.ongakumod.client.screen.DiscRackScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = OngakuMod.MODID, value = Dist.CLIENT)
public class OngakuModClient {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // This links your Container to your Screen
        event.register(OngakuMod.DISC_MENU.get(), DiscRackScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // This links your BlockEntity to your Custom Renderer
        event.registerBlockEntityRenderer(OngakuMod.DISCRACK_BLOCK_ENTITY.get(), DiscRackRenderer::new);
    }
}