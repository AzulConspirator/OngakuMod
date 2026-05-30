package com.azulc.ongakumod.util;

import com.azulc.ongakumod.OngakuMod;
import com.google.common.base.Predicate;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DiscColorCache 
{
    private static final Map<Item, DiscColors> CACHE = new HashMap<>();
    public static final Predicate<ItemStack> FILTER = stack -> {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    };

    public record DiscColors(
        @javax.annotation.Nullable ResourceLocation customVinylTex,
        @javax.annotation.Nullable ResourceLocation customSleeveTex,
        int Index1Color, 
        int Index2Color, 
        int Index3Color,
        int Index4Color, 
        int Index5Color,
        int Index6Color, 
        int Index7Color,
        int Index8Color
    ) {}

    public static void update(ResourceManager manager) {
        CACHE.clear();
        var playableDiscs = BuiltInRegistries.ITEM.stream()
            .filter(item -> item.getDefaultInstance().has(DataComponents.JUKEBOX_PLAYABLE))
            .toList();

        for (Item item : playableDiscs) {
            ResourceLocation location = BuiltInRegistries.ITEM.getKey(item);
            // custom texture path
            ResourceLocation customVinylPath = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/vvs_decor_custom/" + location.getPath() + ".png");
            ResourceLocation customSleevePath = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/vvs_decor_custom/" + location.getPath() + "_cover.png");
            if (manager.getResource(customVinylPath).isPresent() & manager.getResource(customVinylPath).isPresent()) {
                CACHE.put(item, new DiscColors(customVinylPath,customSleevePath,0,0,0,0,0,0,0,0));
                OngakuMod.LOGGER.info("Using custom texture for: {}", location);
                continue;
            }
            //Fallback: Custom Item Texture Color Sampling
            ResourceLocation texturePath = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "textures/item/" + location.getPath() + ".png");
            manager.getResource(texturePath).ifPresentOrElse(resource -> 
            {
                try (InputStream is = resource.open(); NativeImage image = NativeImage.read(is)) 
                {
                    int Index1Color    = sampleFromGrid(image, 2, 5); // Vinyl
                    int Index2Color    = sampleFromGrid(image, 9, 5); // Vinyl 1
                    int Index3Color    = sampleFromGrid(image, 6, 6); // Center Left
                    int Index4Color    = sampleFromGrid(image, 9, 7); // Center Right
                    int Index5Color    = sampleFromGrid(image, 7, 6); // Center Up
                    int Index6Color    = sampleFromGrid(image, 1, 9); // Outline Up
                    int Index7Color    = sampleFromGrid(image, 13, 9); // Outline Down
                    int Index8Color    = sampleFromGrid(image, 7, 11); // Outline Inbetween
                    CACHE.put(item, new DiscColors(
                        null,
                        null,
                        formatColor(Index1Color),
                        formatColor(Index2Color), 
                        formatColor(Index3Color),
                        formatColor(Index4Color),
                        formatColor(Index5Color),
                        formatColor(Index6Color), 
                        formatColor(Index7Color),
                        formatColor(Index8Color)
                    ));
                    
                    OngakuMod.LOGGER.info("SUCCESS: Sampled {}", location );
                } 
                catch (Exception e) 
                {
                    OngakuMod.LOGGER.error("ERROR reading texture for {}: {}", location, e.getMessage());
                }
            }, 
            () -> {
                OngakuMod.LOGGER.warn("MISSING texture file at: {}", texturePath);
            });
        }
    }
    private static int sampleFromGrid(NativeImage image, int x, int y) 
    {
        int width = image.getWidth();
        int height = image.getHeight();

        // The 0.5 ensures we hit the center of the pixel to avoid edge-bleeding
        float relativeX = (x + 0.5f) / 16f;
        float relativeY = (y + 0.5f) / 16f;

        int sampledX = (int)(width * relativeX);
        int sampledY = (int)(height * relativeY);
        
        // Clamp values to ensure we don't go out of bounds on the far edges
        sampledX = Math.min(sampledX, width - 1);
        sampledY = Math.min(sampledY, height - 1);

        return image.getPixelRGBA(sampledX, sampledY);
    }
    
    private static int formatColor(int abgr) 
    {
        // NativeImage gives 0xAABBGGRR
        int a = (abgr >> 24) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int r = abgr & 0xFF;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static DiscColors getColors(Item item) 
    {
        int _FF22 = 0xFF222222;
        int _FFFF = 0xFFFFFFFF;
        return CACHE.getOrDefault(item, new DiscColors(null,null,_FF22, _FFFF,_FF22,_FF22, _FFFF,_FFFF,_FFFF,_FFFF));
    }
}