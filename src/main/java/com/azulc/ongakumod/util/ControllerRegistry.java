/* package com.azulc.ongakumod.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

public class ControllerRegistry extends SavedData {
    private final Map<UUID, GlobalPos> controllers = new HashMap<>();
    private final Map<UUID, ControllerSnapshot> snapshots = new HashMap<>();
    // Corrected 'Pos' to camelCase 'pos' for clean Java compliance
    public record ControllerSnapshot(
    UUID networkId,
    BlockPos pos,
    @Nullable String currentDisc,
    List<String> playlist,
    int playlistIndex,
    boolean autoplay,
    long songStartTick,
    long songDurationTicks
    ){
        /**
         * Writes the snapshot instance to a network byte stream.
         
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(this.networkId);
            buf.writeBlockPos(this.pos);
            
            // Defensively handle null strings across the network wire
            buf.writeBoolean(this.currentDisc != null);
            if (this.currentDisc != null) {
                buf.writeUtf(this.currentDisc);
            }
            buf.writeInt(this.playlist.size());
            for(String disc : this.playlist)
            {
                buf.writeUtf(disc);
            }
            buf.writeInt(this.playlistIndex);
            buf.writeBoolean(this.autoplay);
            buf.writeLong(this.songStartTick);
            buf.writeLong(this.songDurationTicks);
        }

        /**
         * Reads and reconstructs a snapshot from a network byte stream.
         
        public static ControllerSnapshot read(FriendlyByteBuf buf) {
            UUID networkId = buf.readUUID();
            BlockPos pos = buf.readBlockPos();
            
            String currentDisc = null;
            if (buf.readBoolean()) { // Read the safety indicator flag
                currentDisc = buf.readUtf();
            }
            int playlistSize = buf.readInt();
            List<String> playlist = new ArrayList<>();

            for(int i = 0; i < playlistSize; i++)
            {
                playlist.add(buf.readUtf());
            }
            int playlistIndex = buf.readInt();
            boolean autoplay = buf.readBoolean();
            long songStartTick = buf.readLong();
            long songDurationTicks = buf.readLong();

            return new ControllerSnapshot(networkId,pos,currentDisc, playlist,playlistIndex,autoplay,songStartTick,songDurationTicks);
        }
    }
    
    public static ControllerRegistry get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new Factory<>(ControllerRegistry::new, ControllerRegistry::load), "ongaku_controller_registry"
        );
    }

    public void register(UUID id, GlobalPos pos) {
        controllers.put(id, pos);
        setDirty();
    }

    public void unregister(UUID id) {
        controllers.remove(id);
        setDirty();
    }

    public GlobalPos get(UUID id) {
        return controllers.get(id);
    }

    public void updateSnapshot(UUID id, ControllerSnapshot snapshot) {
        snapshots.put(id, snapshot);
        setDirty();
    }

    public ControllerSnapshot getSnapshot(UUID id) {
        return snapshots.get(id);
    }

    @Override
    public CompoundTag save(CompoundTag tag, Provider registries) {
        ListTag list = new ListTag();
        for (var entry : controllers.entrySet()) {
            CompoundTag controllerTag = new CompoundTag();
            controllerTag.putUUID("Id", entry.getKey());
            controllerTag.putString("Dimension", entry.getValue().dimension().location().toString());
            controllerTag.putLong("Pos", entry.getValue().pos().asLong());
            list.add(controllerTag);
        }
        tag.put("Controllers", list);
        
        ListTag snapshotList = new ListTag();
        for (var entry : snapshots.entrySet()) {
            ControllerSnapshot snapshot = entry.getValue();
            CompoundTag snapshotTag = new CompoundTag();
            snapshotTag.putUUID("Id", snapshot.networkId());
            snapshotTag.putLong("Pos", snapshot.pos().asLong());
            
            // Safeguard NBT persistence file saving against NullPointerExceptions
            if (snapshot.currentDisc() != null)
            {
                snapshotTag.putString("CurrentDisc", snapshot.currentDisc());
            } 
            else 
            {
                snapshotTag.putString("CurrentDisc", "");
            }
            ListTag playlistTag = new ListTag();
            for(String disc : snapshot.playlist())
            {
                playlistTag.add(StringTag.valueOf(disc));
            }
            snapshotTag.put("Playlist", playlistTag);
            snapshotTag.putInt("PlaylistIndex", snapshot.playlistIndex());
            snapshotTag.putBoolean("Autoplay", snapshot.autoplay());
            snapshotTag.putLong("SongStartTick", snapshot.songStartTick());
            snapshotTag.putLong("SongDurationTicks", snapshot.songDurationTicks());
            snapshotList.add(snapshotTag);
        }
        tag.put("Snapshots", snapshotList);
        return tag;
    }

    private static ControllerRegistry load(CompoundTag tag, Provider registries) {
        ControllerRegistry registry = new ControllerRegistry();
        ListTag list = tag.getList("Controllers", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag controllerTag = (CompoundTag) element;
            UUID id = controllerTag.getUUID("Id");
            GlobalPos pos = GlobalPos.of(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(controllerTag.getString("Dimension"))),BlockPos.of(controllerTag.getLong("Pos")));
            registry.controllers.put(id, pos);
        }
        
        ListTag snapshotList = tag.getList("Snapshots", Tag.TAG_COMPOUND);
        for (Tag element : snapshotList) {
            CompoundTag snapshotTag = (CompoundTag) element;
            UUID id = snapshotTag.getUUID("Id");
            List<String> playlist = new ArrayList<>();
            ListTag playlistTag = snapshotTag.getList("Playlist", Tag.TAG_STRING);
            for(int i = 0; i < playlistTag.size(); i++)
            {
                playlist.add(playlistTag.getString(i));
            }
            ControllerSnapshot snapshot = new ControllerSnapshot(
                    id,
                    BlockPos.of(snapshotTag.getLong("Pos")),
                    snapshotTag.getString("CurrentDisc").isEmpty()? null: snapshotTag.getString("CurrentDisc"),
                    playlist,
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

*/
package com.azulc.ongakumod.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

public class ControllerRegistry extends SavedData {
    private final Map<UUID, GlobalPos> controllers = new HashMap<>();
    private final Map<UUID, ControllerSnapshot> snapshots = new HashMap<>();

    public record ControllerSnapshot(
        UUID networkId,
        BlockPos pos,
        ItemStack currentDisc, // Swapped to safe ItemStack
        List<ItemStack> playlist, // Swapped to safe ItemStack collection
        int playlistIndex,
        boolean autoplay,
        long songStartTick,
        long songDurationTicks
    ) {
        /**
         * Construct a completely immutable snapshot using defensive copies
         */
        public ControllerSnapshot(
            UUID networkId, BlockPos pos, @Nullable ItemStack currentDisc,
            List<ItemStack> playlist, int playlistIndex, boolean autoplay,
            long songStartTick, long songDurationTicks
        ) {
            this.networkId = networkId;
            this.pos = pos;
            // Prevent downstream mutability from bleeding into our snapshot records
            this.currentDisc = currentDisc == null ? ItemStack.EMPTY : currentDisc.copy();
            
            List<ItemStack> safePlaylist = new ArrayList<>();
            for (ItemStack stack : playlist) {
                safePlaylist.add(stack.copy());
            }
            this.playlist = List.copyOf(safePlaylist); // Lock down list collection completely
            
            this.playlistIndex = playlistIndex;
            this.autoplay = autoplay;
            this.songStartTick = songStartTick;
            this.songDurationTicks = songDurationTicks;
        }

        /**
         * Writes the snapshot instance to a network registry wire stream.
         * Requires RegistryFriendlyByteBuf to preserve modern item data components safely.
         */
        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(this.networkId);
            buf.writeBlockPos(this.pos);
            
            // Native stream codecs handle data components perfectly across the wire
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.currentDisc);
            
            buf.writeInt(this.playlist.size());
            for (ItemStack disc : this.playlist) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, disc);
            }
            
            buf.writeInt(this.playlistIndex);
            buf.writeBoolean(this.autoplay);
            buf.writeLong(this.songStartTick);
            buf.writeLong(this.songDurationTicks);
        }

        /**
         * Reads and reconstructs a snapshot from a network registry stream.
         */
        public static ControllerSnapshot read(RegistryFriendlyByteBuf buf) {
            UUID networkId = buf.readUUID();
            BlockPos pos = buf.readBlockPos();
            ItemStack currentDisc = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            
            int playlistSize = buf.readInt();
            List<ItemStack> playlist = new ArrayList<>();
            for (int i = 0; i < playlistSize; i++) {
                playlist.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
            
            int playlistIndex = buf.readInt();
            boolean autoplay = buf.readBoolean();
            long songStartTick = buf.readLong();
            long songDurationTicks = buf.readLong();

            return new ControllerSnapshot(networkId, pos, currentDisc, playlist, playlistIndex, autoplay, songStartTick, songDurationTicks);
        }
    }
    
    public static ControllerRegistry get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new Factory<>(ControllerRegistry::new, ControllerRegistry::load), "ongaku_controller_registry"
        );
    }

    public void register(UUID id, GlobalPos pos) {
        controllers.put(id, pos);
        setDirty();
    }

    public void unregister(UUID id) {
        controllers.remove(id);
        setDirty();
    }

    public GlobalPos get(UUID id) {
        return controllers.get(id);
    }

    public void updateSnapshot(UUID id, ControllerSnapshot snapshot) {
        // Enforce defensive copy via constructor during storage updates
        snapshots.remove(id);
        snapshots.put(id, new ControllerSnapshot(
            snapshot.networkId(), snapshot.pos(), snapshot.currentDisc(),
            snapshot.playlist(), snapshot.playlistIndex(), snapshot.autoplay(),
            snapshot.songStartTick(), snapshot.songDurationTicks()
        ));
        setDirty();
    }

    public ControllerSnapshot getSnapshot(UUID id) {
        return snapshots.get(id);
    }

    @Override
    public CompoundTag save(CompoundTag tag, Provider registries) {
        ListTag list = new ListTag();
        for (var entry : controllers.entrySet()) {
            CompoundTag controllerTag = new CompoundTag();
            controllerTag.putUUID("Id", entry.getKey());
            controllerTag.putString("Dimension", entry.getValue().dimension().location().toString());
            controllerTag.putLong("Pos", entry.getValue().pos().asLong());
            list.add(controllerTag);
        }
        tag.put("Controllers", list);
        
        ListTag snapshotList = new ListTag();
        for (var entry : snapshots.entrySet()) {
            ControllerSnapshot snapshot = entry.getValue();
            CompoundTag snapshotTag = new CompoundTag();
            snapshotTag.putUUID("Id", snapshot.networkId());
            snapshotTag.putLong("Pos", snapshot.pos().asLong());
            
            // Modern native serialization utilizing provider lookup context
            if (!snapshot.currentDisc().isEmpty()) {
                snapshotTag.put("CurrentDiscItem", snapshot.currentDisc().save(registries));
            }
            
            ListTag playlistTag = new ListTag();
            for (ItemStack disc : snapshot.playlist()) {
                if (!disc.isEmpty()) {
                    playlistTag.add(disc.save(registries));
                }
            }
            snapshotTag.put("PlaylistItems", playlistTag);
            snapshotTag.putInt("PlaylistIndex", snapshot.playlistIndex());
            snapshotTag.putBoolean("Autoplay", snapshot.autoplay());
            snapshotTag.putLong("SongStartTick", snapshot.songStartTick());
            snapshotTag.putLong("SongDurationTicks", snapshot.songDurationTicks());
            snapshotList.add(snapshotTag);
        }
        tag.put("Snapshots", snapshotList);
        return tag;
    }

    private static ControllerRegistry load(CompoundTag tag, Provider registries) {
        ControllerRegistry registry = new ControllerRegistry();
        ListTag list = tag.getList("Controllers", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag controllerTag = (CompoundTag) element;
            UUID id = controllerTag.getUUID("Id");
            GlobalPos pos = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(controllerTag.getString("Dimension"))), BlockPos.of(controllerTag.getLong("Pos")));
            registry.controllers.put(id, pos);
        }
        
        ListTag snapshotList = tag.getList("Snapshots", Tag.TAG_COMPOUND);
        for (Tag element : snapshotList) {
            CompoundTag snapshotTag = (CompoundTag) element;
            UUID id = snapshotTag.getUUID("Id");
            
            ItemStack currentDisc = ItemStack.EMPTY;
            if (snapshotTag.contains("CurrentDiscItem", Tag.TAG_COMPOUND)) {
                currentDisc = ItemStack.parse(registries, snapshotTag.getCompound("CurrentDiscItem")).orElse(ItemStack.EMPTY);
            }

            List<ItemStack> playlist = new ArrayList<>();
            ListTag playlistTag = snapshotTag.getList("PlaylistItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < playlistTag.size(); i++) {
                ItemStack.parse(registries, playlistTag.getCompound(i)).ifPresent(playlist::add);
            }

            ControllerSnapshot snapshot =
            new ControllerSnapshot(id, BlockPos.of(snapshotTag.getLong("Pos")), currentDisc, playlist, snapshotTag.getInt("PlaylistIndex"), snapshotTag.getBoolean("Autoplay"), snapshotTag.getLong("SongStartTick"), snapshotTag.getLong("SongDurationTicks"));
            registry.snapshots.put(id, snapshot);
        }
        return registry;
    }
}