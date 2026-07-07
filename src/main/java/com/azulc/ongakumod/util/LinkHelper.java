package com.azulc.ongakumod.util;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.blockentity.SpeakerBlockEntity;
import com.azulc.ongakumod.network.AudioPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class LinkHelper {
    //#region Rack Link
    public static boolean addLinkedRack(AutoplayControllerBlockEntity Controller, BlockPos rackPos) 
    {
        if (Controller.getLevel().getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) 
        {
            BlockPos existingController = rack.getControllerPos();
            // CASE 1: TOGGLE OFF - If already linked to THIS controller, disconnect it
            if (Controller.getBlockPos().equals(existingController)) {
                removeLinkedRack(Controller,rackPos);
                return false; // Return false to indicate "Disconnected"
            }
            // CASE 2: REPLACE - If linked to a DIFFERENT controller, force a transfer
            if (existingController != null) {
                if (Controller.getLevel().getBlockEntity(existingController) instanceof AutoplayControllerBlockEntity oldController) 
                {
                    removeLinkedRack(oldController,rackPos);
                }
            }
            // CASE 3: CONNECT - Establish the new link
            rack.setControllerPos(Controller.getBlockPos());
            CtrlHelper.getLinkedRackPositions(Controller).add(rackPos);
            // Finalize sync
            Controller.setChanged();
            Controller.getLevel().sendBlockUpdated(Controller.getBlockPos(), Controller.getBlockState(), Controller.getBlockState(), 3);
            rack.setChanged();
            Controller.getLevel().sendBlockUpdated(rackPos, rack.getBlockState(), rack.getBlockState(), 3);
            return true; 
        }
        return false;
    }

    public static void removeLinkedRack(AutoplayControllerBlockEntity Controller, BlockPos rackPos) 
    {
        // 1. Remove from Controller's memory
        boolean removed =  CtrlHelper.getLinkedRackPositions(Controller).remove(rackPos);
        // 2. IMPORTANT: Clear the Rack's internal memory
        if (Controller.getLevel().getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) {
            // Only clear it if the rack actually thinks it belongs to THIS controller
            if (Controller.getBlockPos().equals(rack.getControllerPos())) {
                rack.setControllerPos(null);
                rack.setChanged();
                Controller.getLevel().sendBlockUpdated(rackPos, rack.getBlockState(), rack.getBlockState(), 3);
            }
        }
        // 3. Refresh playlist and sync
        if (removed) {
            if (Controller.getLevel() != null && !Controller.getLevel().isClientSide) {
                Controller.getLevel().sendBlockUpdated(Controller.getBlockPos(), Controller.getBlockState(), Controller.getBlockState(), 3);
            }
            Controller.setChanged();
        }
    }

    public static void clearLinkedRacks(AutoplayControllerBlockEntity Controller) 
    {
        CtrlHelper.getLinkedRackPositions(Controller).clear();
        Controller.setChanged();
    }

    public static DiscRackBlockEntity getRack(AutoplayControllerBlockEntity Controller,BlockPos pos) 
    {
        Level level = Controller.getLevel();
        if (level == null) { return null; }
        if (!level.isLoaded(pos)) {return null;}

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DiscRackBlockEntity rack) {
            return rack;
        }
        return null;
    }
    //#endregion
    //#region Speaker Link
    public static boolean addLinkedSpeaker(AutoplayControllerBlockEntity Controller, BlockPos speakerPos) 
    {
        if (Controller.getLevel() == null || Controller.getLevel().isClientSide) return false;

        // Stop music whenever a connection state changes (as requested)
        CtrlHelper.StopJukebox(Controller); 
        if (CtrlHelper.getLinkedSpeakerPositions(Controller).contains(speakerPos)) 
        {
            CtrlHelper.getLinkedSpeakerPositions(Controller).remove(speakerPos);
            if (Controller.getLevel().getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) 
            {
                speaker.setControllerPos(null);
                speaker.setPlaying(false); // Stop particles immediately
            }
            Controller.setChanged();
            return false;
        } else {
            CtrlHelper.getLinkedSpeakerPositions(Controller).add(speakerPos);
            if (Controller.getLevel().getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) {
                speaker.setControllerPos(Controller.getBlockPos());
                // Don't set playing to true yet; wait for the next song to start
            }
            Controller.setChanged();
            return true;
        }
    }

    public static void removeLinkedSpeaker(AutoplayControllerBlockEntity Controller,BlockPos speakerPos) {
        boolean removed = CtrlHelper.getLinkedSpeakerPositions(Controller).remove(speakerPos);
        if (Controller.getLevel().getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) {
            if (Controller.getBlockPos().equals(speaker.getControllerPos())) {
                speaker.setControllerPos(null);
                speaker.setChanged();
                Controller.getLevel().sendBlockUpdated(speakerPos, speaker.getBlockState(), speaker.getBlockState(), 3);
            }
        }
        if (removed) {
            broadcastToSpeakers(Controller,false, null);
            Controller.setChanged();
        }
    }

    @SuppressWarnings("static-access")
    public static void broadcastToSpeakers(AutoplayControllerBlockEntity Controller, boolean isPlaying, @Nullable ItemStack disc) {
        Level level = Controller.getLevel();
        var linkedSpeakers = CtrlHelper.getLinkedSpeakerPositions(Controller);
            if (level == null || level.isClientSide) return;
        UUID NetID = Controller.getNetworkId(Controller);
        for (BlockPos speakerPos : linkedSpeakers) {
            if (level.isLoaded(speakerPos)) {
                BlockEntity be = level.getBlockEntity(speakerPos);
                if (be instanceof SpeakerBlockEntity speaker) {
                    // Update the BE state so particles work!
                    speaker.setPlaying(isPlaying);
                    AudioPayload packet = new AudioPayload(NetID, Optional.ofNullable(disc), isPlaying, true, Optional.of(speakerPos), Optional.empty());
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel)level, new ChunkPos(speakerPos), packet);
                }
                else
                {
                    removeLinkedSpeaker(Controller,speakerPos);
                }
            }
        }
    }

    //#endregion
    public static boolean ControllerExist(UUID UUIDgiven, Level level, GlobalPos BE)
    {
        if (UUIDgiven == null || level == null || BE == null) {return false;}
        net.minecraft.server.MinecraftServer server = level.getServer();
        if (server == null) {return false;}
        ServerLevel controllerLevel = server.getLevel(BE.dimension());
        if (controllerLevel == null || !controllerLevel.isLoaded(BE.pos())) {return false;}
        BlockEntity ctrl = controllerLevel.getBlockEntity(BE.pos());
        if (ctrl instanceof AutoplayControllerBlockEntity controller) {
            return AutoplayControllerBlockEntity.getNetworkId(controller).equals(UUIDgiven);
        }
        return false;
    }

    public static boolean hasComponentByString(ItemStack stack, String componentId) {
        ResourceLocation location = ResourceLocation.parse(componentId);
        Optional<DataComponentType<?>> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(location);
        return componentType.map(stack::has).orElse(false);
    }
}
