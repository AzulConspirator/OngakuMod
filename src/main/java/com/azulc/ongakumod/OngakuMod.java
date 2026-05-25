package com.azulc.ongakumod;

import org.slf4j.Logger;

import com.azulc.ongakumod.block.DiscRackBlock;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.container.DiscContainer;
import com.azulc.ongakumod.util.DiscColorCache;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(OngakuMod.MODID)
public class OngakuMod 
{
    public static final String MODID                                            = "ongakumod";
    public static final Logger LOGGER                                           = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS                          = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS                            = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS                     = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES     = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS    = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    
    public static final DeferredHolder<MenuType<?>, MenuType<DiscContainer>> DISC_MENU = MENUS.register("disc_rack", () -> new MenuType<>(DiscContainer::new, FeatureFlagSet.of()));
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiscRackBlockEntity>> DISCRACK_BLOCK_ENTITY = BLOCK_ENTITIES.register("discrack_block_entity", () -> BlockEntityType.Builder.of(DiscRackBlockEntity::new, BLOCKS.getEntries().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    public static final DeferredBlock<DiscRackBlock> DISC_RACK = BLOCKS.register("disc_rack", () -> new DiscRackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())); // noOcclusion is important for non-full blocks like racks
    public static final DeferredItem<BlockItem> DISC_RACK_ITEM = ITEMS.registerSimpleBlockItem("disc_rack", DISC_RACK);

    public OngakuMod(IEventBus modEventBus, ModContainer modContainer) 
    {
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        //modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onAddReloadListeners);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
    }

    private void onAddReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller1, executor, executor1) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> { DiscColorCache.update(resourceManager); }, executor1);
        });
    }
    
    private void commonSetup(FMLCommonSetupEvent event) 
    {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
