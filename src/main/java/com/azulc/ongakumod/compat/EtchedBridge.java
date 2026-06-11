package com.azulc.ongakumod.compat;

import java.util.Locale;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.util.DiscColorCache;
import com.azulc.ongakumod.util.DiscColorCache.DiscColors;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.core.registry.EtchedComponents;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

public class EtchedBridge 
{
    public static Boolean checkEtchedComponent(ItemStack stack)
    {
        return stack.has(EtchedComponents.MUSIC);
    }
    public static String getEtchedUrl(ItemStack stack) 
    {
        var musicData = stack.get(EtchedComponents.MUSIC); 
        if (musicData != null) {
            TrackData extractedUrl = musicData.tracks().get(0); 
            if (extractedUrl != null) {
                return extractedUrl.url();
            }
        }
        return null;
    }

    public static void EtchedCompatTextureSolver(ResourceManager manager) 
    {
        ResourceLocation jsonLocation = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/vvs_decor_custom/etched/etched_list.json");
        Optional<Resource> jsonResource = manager.getResource(jsonLocation);
        if (jsonResource.isEmpty()) 
        {
            OngakuMod.LOGGER.warn("Missing etched alias file at: {}", jsonLocation);
            return;
        }

        try (Reader _reader = jsonResource.get().openAsReader())
        {
            Gson Gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> aliasMap = Gson.fromJson(_reader, mapType);
            if (aliasMap == null) return;
            for (Map.Entry<String, String> entry : aliasMap.entrySet()) 
            {
                String urlKey = entry.getKey();
                String aliasValue = entry.getValue().toLowerCase(Locale.ROOT);

                ResourceLocation vinylResource = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/vvs_decor_custom/etched/etched_music_disc_" + aliasValue + ".png");
                ResourceLocation sleeveResource = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/vvs_decor_custom/etched/etched_music_disc_" + aliasValue + "_cover.png");

                if (manager.getResource(vinylResource).isPresent() && manager.getResource(sleeveResource).isPresent()) 
                {
                    DiscColorCache.ETCHED_CACHE.put(urlKey, new DiscColors(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "vvs_decor_custom/etched/etched_music_disc_" + aliasValue),ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "vvs_decor_custom/etched/etched_music_disc_" + aliasValue + "_cover"),0, 0, 0, 0, 0, 0, 0, 0));
                    OngakuMod.LOGGER.info("Successfully mapped URL [{}] to alias textures [{}]", urlKey, aliasValue);
                } 
                else 
                {
                    OngakuMod.LOGGER.error("Textures were missing for Etched asset: {}", aliasValue);
                }
            }
        } 
        catch (Exception e) 
        {
            OngakuMod.LOGGER.error("Failed to parse etched alias JSON: {}", e.getMessage());
        }
    }

    public static boolean hasEtchedMusic(ItemStack stack) {
        return PlayableRecord.isPlayableRecord(stack);
    }

    public static Optional<SoundInstance> createEtchedBlockSound(BlockPos pos, ItemStack stack, LevelAccessor level) {
        // '0' designates the first track on the album
        return PlayableRecord.createBlockSound(stack, level, pos, 0); 
    }

    public static Optional<SoundInstance> createEtchedEntitySound(Entity entity, ItemStack stack) {
        return PlayableRecord.createEntitySound(stack, entity, 0);
    }
}
