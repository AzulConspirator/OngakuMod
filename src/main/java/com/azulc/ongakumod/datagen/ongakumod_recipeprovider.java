package com.azulc.ongakumod.datagen;

import java.util.concurrent.CompletableFuture;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

public class ongakumod_recipeprovider extends RecipeProvider implements IConditionBuilder{

    public ongakumod_recipeprovider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) 
    { 
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, OngakuMod.AUTOPLAY_CONTROLLER.asItem())
        .pattern("SAS")
        .pattern("DBD")
        .pattern("SCS")
        .define('A', Items.JUKEBOX)
        .define('B', Items.REDSTONE_BLOCK)
        .define('C', Items.IRON_BLOCK)
        .define('D', Items.NOTE_BLOCK)
        .define('S', ItemTags.PLANKS)
        .unlockedBy(getName(), has(Items.STICK))
        .save(recipeOutput, OngakuMod.AUTOPLAY_CONTROLLER.getId()); 

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, OngakuMod.TUNING_WRENCH.asItem())
        .pattern("S S")
        .pattern(" A ")
        .pattern(" S ")
        .define('S', Items.IRON_NUGGET)
        .define('A', Items.STICK)
        .unlockedBy(getName(), has(Items.STICK))
        .save(recipeOutput, OngakuMod.TUNING_WRENCH.getId()); 

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, OngakuMod.SPEAKER.asItem())
        .pattern("SAS")
        .define('S', Items.IRON_NUGGET)
        .define('A', Items.NOTE_BLOCK)
        .unlockedBy(getName(), has(Items.STICK))
        .save(recipeOutput, OngakuMod.SPEAKER.getId()); 

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ItemTags.LOGS),RecipeCategory.DECORATIONS,   // Recipe Category
        OngakuMod.DISC_RACK.asItem(),1)
        .unlockedBy(getName(), has(ItemTags.PLANKS))
        .save(recipeOutput, OngakuMod.DISC_RACK.getId());

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ItemTags.LOGS),RecipeCategory.DECORATIONS,   // Recipe Category
        OngakuMod.DISC_BOX.asItem(),1)
        .unlockedBy(getName(), has(ItemTags.PLANKS))
        .save(recipeOutput, OngakuMod.DISC_BOX.getId());

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(ItemTags.LOGS),RecipeCategory.DECORATIONS,   // Recipe Category
        OngakuMod.DISC_WALL_RACK.asItem(),1)
        .unlockedBy(getName(), has(ItemTags.PLANKS))
        .save(recipeOutput, OngakuMod.DISC_WALL_RACK.getId());
    }
    
}
