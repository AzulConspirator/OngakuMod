package com.azulc.ongakumod.datagen;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = OngakuMod.MODID)
public class datagenerator {
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) 
    {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        @SuppressWarnings("unused")
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
        //data providers
        generator.addProvider(event.includeServer(),new LootTableProvider(output, Collections.emptySet(),List.of(new LootTableProvider.SubProviderEntry(blockloottableprovider::new, LootContextParamSets.BLOCK)), lookupProvider));
        generator.addProvider(event.includeServer(), new ongakumod_recipeprovider(output, lookupProvider));
        //asset providers
        //generator.addProvider(event.includeClient(), new blockstateprovider(output, existingFileHelper));
        //generator.addProvider(event.includeClient(), new Itemmodelprovider(output, existingFileHelper));
    }
}
