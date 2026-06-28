package com.azulc.ongakumod.datagen;

import java.util.concurrent.CompletableFuture;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;


public class ongakumod_blocktagsprovider extends IntrinsicHolderTagsProvider<Block> 
{
	@SuppressWarnings("deprecation")
	public ongakumod_blocktagsprovider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) 
	{
		super(packOutput, Registries.BLOCK, registries, block -> block.builtInRegistryHolder().key(), OngakuMod.MODID,existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(OngakuMod.AUTOPLAY_CONTROLLER.get());
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(OngakuMod.TERMINAL_BLOCK.get());
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(OngakuMod.DISC_BOX.get());
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(OngakuMod.DISC_RACK.get());
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(OngakuMod.DISC_WALL_RACK.get());
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(OngakuMod.SPEAKER.get());
	}
}