package com.azulc.ongakumod.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class ControllerRegistry extends SavedData
{
    private final Map<UUID, GlobalPos> controllers = new HashMap<>();
    private final Map<UUID, ControllerSnapshot> snapshots = new HashMap<>();

    public record ControllerSnapshot(
        UUID networkId,
        BlockPos Pos,
        String currentDisc,
        int playlistIndex,
        boolean autoplay,
        long songStartTick,
        long songDurationTicks
    ){}
    
    public static ControllerRegistry get(ServerLevel level)
    {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(ControllerRegistry::new,ControllerRegistry::load),"ongaku_controller_registry");
    }

    public void register(UUID id, GlobalPos pos)
    {
        controllers.put(id, pos);
        setDirty();
    }

    public void unregister(UUID id)
    {
        controllers.remove(id);
        setDirty();
    }

    public GlobalPos get(UUID id)
    {
        return controllers.get(id);
    }

    public void updateSnapshot(UUID id,ControllerSnapshot snapshot)
    {
        snapshots.put(id, snapshot);
        setDirty();
    }

    public ControllerSnapshot getSnapshot(UUID id)
    {
        return snapshots.get(id);
    }
    
    @Override
    public CompoundTag save(CompoundTag tag,Provider registries)
    {
        ListTag list = new ListTag();
        for(var entry : controllers.entrySet())
        {
            CompoundTag controllerTag = new CompoundTag();
            controllerTag.putUUID("Id", entry.getKey());
            controllerTag.putString("Dimension",entry.getValue().dimension().location().toString());
            controllerTag.putLong("Pos",entry.getValue().pos().asLong());
            list.add(controllerTag);
        }
        tag.put("Controllers", list);
        ListTag snapshotList = new ListTag();

        for(var entry : snapshots.entrySet())
        {
            ControllerSnapshot snapshot = entry.getValue();
            CompoundTag snapshotTag = new CompoundTag();
            snapshotTag.putUUID("Id", snapshot.networkId());
            snapshotTag.putLong("Pos",snapshot.Pos().asLong());
            snapshotTag.putString("CurrentDisc",snapshot.currentDisc());
            snapshotTag.putInt("PlaylistIndex",snapshot.playlistIndex());
            snapshotTag.putBoolean( "Autoplay", snapshot.autoplay());
            snapshotTag.putLong("SongStartTick",snapshot.songStartTick());
            snapshotTag.putLong("SongDurationTicks",snapshot.songDurationTicks());
            snapshotList.add(snapshotTag);
        }
        tag.put("Snapshots", snapshotList);
        return tag;
    }

    private static ControllerRegistry load(CompoundTag tag,Provider registries)
    {
        ControllerRegistry registry = new ControllerRegistry();
        ListTag list = tag.getList("Controllers", Tag.TAG_COMPOUND);
        for(Tag element : list)
        {
            CompoundTag controllerTag = (CompoundTag) element;
            UUID id = controllerTag.getUUID("Id");
            GlobalPos pos = GlobalPos.of
            (
                net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(controllerTag.getString("Dimension"))),
                net.minecraft.core.BlockPos.of(controllerTag.getLong("Pos"))
            );
            registry.controllers.put(id, pos);
        }
        ListTag snapshotList = tag.getList("Snapshots", Tag.TAG_COMPOUND);
        for(Tag element : snapshotList)
        {
            CompoundTag snapshotTag =(CompoundTag) element;
            UUID id = snapshotTag.getUUID("Id");
            ControllerSnapshot snapshot =
            new ControllerSnapshot(
                id,
                BlockPos.of(
                    snapshotTag.getLong("Pos")
                ),
                snapshotTag.getString("CurrentDisc"),
                snapshotTag.getInt("PlaylistIndex"),
                snapshotTag.getBoolean("Autoplay"),
                snapshotTag.getLong("SongStartTick"),
                snapshotTag.getLong("SongDurationTicks")
            );
            registry.snapshots.put(id, snapshot);
        }
        return registry;
    }
}