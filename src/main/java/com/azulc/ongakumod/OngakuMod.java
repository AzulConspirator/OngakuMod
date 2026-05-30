package com.azulc.ongakumod;

import java.util.List;

import org.slf4j.Logger;

import com.azulc.ongakumod.block.AutoplayControllerBlock;
import com.azulc.ongakumod.block.DiscRackBlock;
import com.azulc.ongakumod.block.DiscRackBoxBlock;
import com.azulc.ongakumod.block.DiscRackWallBlock;
import com.azulc.ongakumod.block.SpeakerBlock;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.blockentity.SpeakerBlockEntity;
import com.azulc.ongakumod.container.AutoplayMenu;
import com.azulc.ongakumod.container.DiscContainer;
import com.azulc.ongakumod.item.TuningWrenchItem;
import com.azulc.ongakumod.network.ClientPayloadHandler;
import com.azulc.ongakumod.network.ManagePlaylistPayload;
import com.azulc.ongakumod.network.PlayDiscPayload;
import com.azulc.ongakumod.network.ServerPayloadHandler;
import com.azulc.ongakumod.network.StopDiscPayload;
import com.azulc.ongakumod.network.SyncPlaylistPayload;
import com.azulc.ongakumod.util.DiscColorCache;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(OngakuMod.MODID)
public class OngakuMod 
{
    public static final String MODID                                                = "ongakumod";
    public static final Logger LOGGER                                               = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS                              = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS                                = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS                         = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES         = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS        = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "ongaku");
   
    // Container
    public static final DeferredHolder<MenuType<?>, MenuType<DiscContainer>> DISC_MENU                                              = MENUS.register("disc_rack", () -> new MenuType<>(DiscContainer::new, FeatureFlagSet.of()));
    public static final DeferredHolder<MenuType<?>, MenuType<AutoplayMenu>> AUTOPLAY_MENU = 
    MENUS.register("autoplay_controller", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> {
            BlockPos pos = data.readBlockPos();
            // 1.21.1 helper to read a list of items from the network
            List<ItemStack> discs = ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(data); 
            return new AutoplayMenu(windowId, inv, pos, discs);
        }));
    // Disc Rack
    public static final DeferredBlock<DiscRackBlock> DISC_RACK                                                                      = BLOCKS.register("disc_rack", () -> new DiscRackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion()));
    public static final DeferredItem<BlockItem> DISC_RACK_ITEM                                                                      = ITEMS.registerSimpleBlockItem("disc_rack", DISC_RACK);
    public static final DeferredBlock<Block> DISC_BOX                                                                               = BLOCKS.register("disc_box", () -> new DiscRackBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion()));
    public static final DeferredItem<BlockItem> DISC_BOX_ITEM                                                                       = ITEMS.registerSimpleBlockItem("disc_box", DISC_BOX);
    public static final DeferredBlock<Block> DISC_WALL_RACK                                                                         = BLOCKS.register("disc_wallmount", () -> new DiscRackWallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion()));
    public static final DeferredItem<BlockItem> DISC_WALL_RACK_ITEM                                                                 = ITEMS.registerSimpleBlockItem("disc_wallmount", DISC_WALL_RACK);
    public static final DeferredBlock<SpeakerBlock> SPEAKER                                                                        = BLOCKS.register("speaker", () -> new SpeakerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion()));
    public static final DeferredItem<BlockItem> SPEAKER_ITEM                                                                        = ITEMS.registerSimpleBlockItem("speaker", SPEAKER);
    //
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpeakerBlockEntity>> SPEAKER_BLOCK_ENTITY                = BLOCK_ENTITIES.register("speaker_block_entity", () -> BlockEntityType.Builder.of(SpeakerBlockEntity::new, BLOCKS.getEntries().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiscRackBlockEntity>> DISCRACK_BLOCK_ENTITY              = BLOCK_ENTITIES.register("discrack_block_entity", () -> BlockEntityType.Builder.of(DiscRackBlockEntity::new, BLOCKS.getEntries().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    // Controller
    public static final DeferredBlock<Block> AUTOPLAY_CONTROLLER                                                                    = BLOCKS.register("autoplay_controller", () -> new AutoplayControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion()));//() -> new AutoplayControllerBlock(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)));
    public static final DeferredItem<BlockItem> AUTOPLAY_CONTROLLER_ITEM                                                            = ITEMS.register("autoplay_controller", () -> new BlockItem(AUTOPLAY_CONTROLLER.get(), new BlockItem.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoplayControllerBlockEntity>> AUTOPLAY_BLOCK_ENTITY    = BLOCK_ENTITIES.register("autoplay_block_entity", () -> BlockEntityType.Builder.of(AutoplayControllerBlockEntity::new, AUTOPLAY_CONTROLLER.get()).build(null));
    //Wrench
    public static final DeferredItem<Item> TUNING_WRENCH                                                                            = ITEMS.register("tuning_wrench", () -> new TuningWrenchItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> SAVED_LOCATION                           = DATA_COMPONENT_TYPES.register("saved_location", () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
    // Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ONGAKU_TAB = CREATIVE_MODE_TABS.register("ongaku_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.ongakumod")).icon(() -> 
    new ItemStack(DISC_RACK_ITEM.get()))
    .displayItems((parameters, output) -> {
        output.accept(TUNING_WRENCH.get());
        output.accept(AUTOPLAY_CONTROLLER_ITEM.get());
        output.accept(DISC_RACK_ITEM.get());
        output.accept(DISC_BOX_ITEM.get());
        output.accept(DISC_WALL_RACK_ITEM.get());
        output.accept(SPEAKER_ITEM.get());
    }).build());
    //

    public OngakuMod(IEventBus modEventBus, ModContainer modContainer) 
    {
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        DATA_COMPONENT_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerNetworking);
        modEventBus.addListener(this::onAddReloadListeners);
        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
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
    
    public void registerNetworking(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);
        
        // Register the packet to only flow from Client -> Server
        registrar.playToServer(
            PlayDiscPayload.TYPE, 
            PlayDiscPayload.STREAM_CODEC, 
            ServerPayloadHandler::handlePlayDisc
        );
        registrar.playToServer(
            StopDiscPayload.TYPE, 
            StopDiscPayload.STREAM_CODEC, 
            ServerPayloadHandler::handleStopDisc
        );
        registrar.playToClient(
            SyncPlaylistPayload.TYPE,
            SyncPlaylistPayload.STREAM_CODEC,
            ClientPayloadHandler::handleSyncPlaylist
        );
        registrar.playToServer(
            ManagePlaylistPayload.TYPE,
            ManagePlaylistPayload.STREAM_CODEC,
            ServerPayloadHandler::handlePlaylistAction
        );
    }
}
