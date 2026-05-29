package com.azulc.ongakumod.datagen;

import java.util.concurrent.CompletableFuture;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

public class ongakumod_recipeprovider extends RecipeProvider implements IConditionBuilder{

    public ongakumod_recipeprovider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) 
    { 
/*         ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, "autoplay_controller")
        .pattern("SSS")
        .pattern("SAS")
        .pattern("SBS")
        .define('S', Items.BAMBOO_PLANKS)
        .unlockedBy(getName(), has(Items.STICK))
        .save(recipeOutput, BuiltInRegistries.BLOCK.getKey(OngakuMod.BLOCKS.).getPath()); */
    }
    
}
