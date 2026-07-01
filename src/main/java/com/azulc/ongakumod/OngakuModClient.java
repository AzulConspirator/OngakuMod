package com.azulc.ongakumod;

import com.azulc.ongakumod.client.AutoplayControllerRenderer;
import com.azulc.ongakumod.client.DiscRackRenderer;
import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.client.screen.DiscRackScreen;
import com.azulc.ongakumod.client.screen.TerminalScreen;
import com.azulc.ongakumod.util.DiscColorCache;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = OngakuMod.MODID, value = Dist.CLIENT)
public class OngakuModClient {

    public static final ResourceLocation BUTTON_ICONS = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/controller.png");
    public static final ResourceLocation PROGRESS_BAR = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/playback_progress.png");
    public static final ResourceLocation BG_SPRITE = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "ui_bg");
    public static final ResourceLocation BUTTON_SPRITE = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "ui_button");
    public static final ResourceLocation ENTRY_ACTIVE = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "ui_entry_active");
    public static final ResourceLocation ENTRY_INACTIVE = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "ui_entry_inactive");
    public static ModelResourceLocation VinylModel = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "block/vinyl"), "standalone");
    public static ModelResourceLocation SleeveModel = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "block/vinyl_sleeve"), "standalone");
    
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(OngakuMod.DISC_MENU.get(), DiscRackScreen::new);
        event.register(OngakuMod.AUTOPLAY_MENU.get(), AutoplayScreen::new);
        event.register(OngakuMod.TERMINAL_MENU.get(),TerminalScreen::new);
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterAdditional event) {
        event.register(VinylModel);
        event.register(SleeveModel);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OngakuMod.DISCRACK_BLOCK_ENTITY.get(), DiscRackRenderer::new);
        event.registerBlockEntityRenderer(OngakuMod.AUTOPLAY_BLOCK_ENTITY.get(), AutoplayControllerRenderer::new);
    }

     @SubscribeEvent
    public static void onAddReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller1, executor, executor1) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> { DiscColorCache.update(resourceManager); }, executor1);
        });
    }
}