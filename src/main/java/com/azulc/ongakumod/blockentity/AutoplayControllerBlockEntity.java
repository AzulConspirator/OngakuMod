package com.azulc.ongakumod.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.network.TerminalControlHandler;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.CtrlHelper;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;
import com.azulc.ongakumod.util.JukeboxHelper;
import com.azulc.ongakumod.util.LinkHelper;
import com.azulc.ongakumod.util.PlaylistHelper;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentity;
import com.azulc.ongakumod.util.PlaylistHelper.PlaylistEntry;

public class AutoplayControllerBlockEntity extends BlockEntity 
{
    private UUID networkId = UUID.randomUUID();
    public int tickCounter = 0;
    public int ticksSinceLoad = 0;
    public PlaylistEntry currentlyPlayingEntry = null;
    public int currentPlaylistIndex = -1;
    public long songStartTick = -1;
    public int songDurationTicks = 0;
    public boolean autoplayEnabled = false;
    public int cachedStatus = 0;
    public BlockPos JukeboxPosition;
    public final Set<BlockPos> linkedRackPositions = new LinkedHashSet<>();
    public final Set<BlockPos> linkedSpeakers = new LinkedHashSet<>();
    public final List<DiscIdentity> customQueueOrder = new ArrayList<>();
    public final Set<DiscIdentity> excludedTracks = new HashSet<>();

    public AutoplayControllerBlockEntity(BlockPos pos, BlockState state) 
    {
        super(OngakuMod.AUTOPLAY_BLOCK_ENTITY.get(), pos, state);
    }
    //#region Container Data
    public final ContainerData data = new ContainerData() 
    {
        @Override
        public int get(int index) 
        {
            return switch (index) 
            {
                case 0 -> linkedRackPositions.isEmpty() ? 0 : 1;
                case 1 -> CtrlHelper.GetCurrentPlayingIndex(AutoplayControllerBlockEntity.this);
                case 2 -> JukeboxHelper.CheckJukeStatus(AutoplayControllerBlockEntity.this, JukeboxHelper.findJukebox(AutoplayControllerBlockEntity.this));
                case 3 -> autoplayEnabled ? 1 : 0;
                case 4 -> (int)(level.getGameTime() - songStartTick);
                case 5 -> songDurationTicks;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) 
        {
            if (index == 1) {
                currentPlaylistIndex = value;
            }
            if (index == 2) {
                cachedStatus = value;
                AutoplayControllerBlockEntity.this.setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
            if (index == 3) {
                autoplayEnabled = (value == 1);
                AutoplayControllerBlockEntity.this.setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
        @Override
        public int getCount() 
        {
            return 6;
        }
    };
    //#endregion
    //#region Update Tag
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries); 
        return tag;
    }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }
    //#endregion
    //#region Server Tick
    @Override
    public void onLoad() {
        super.onLoad();
        getNetworkId(this);
        if(level instanceof ServerLevel serverLevel) {
            ControllerRegistry registry = ControllerRegistry.get(serverLevel);
            ControllerSnapshot existing = registry.getSnapshot(this.networkId);
            if (existing != null) {
                this.autoplayEnabled = existing.autoplay();
                this.currentPlaylistIndex = existing.playlistIndex();
                this.setChanged();
            }
            registry.register(networkId, GlobalPos.of(serverLevel.dimension(), worldPosition));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoplayControllerBlockEntity entity) {
        entity.ticksSinceLoad++;
        entity.tickCounter++;
        if (entity.tickCounter % 20 == 0) {
            if (entity.ticksSinceLoad > 100) { // ~5 seconds grace period
                entity.validateAndProcess(level, pos);
            }
            if (level instanceof ServerLevel serverLevel) {
                ControllerRegistry registry = ControllerRegistry.get(serverLevel);
                UUID id = getNetworkId(entity);
                if (entity.currentlyPlayingEntry == null && entity.songStartTick == -1)
                {
                    ControllerSnapshot remote = registry.getSnapshot(id);
                    if (remote != null) {
                        entity.currentPlaylistIndex = remote.playlistIndex();
                        entity.autoplayEnabled = remote.autoplay();
                    }
                }
                ControllerRegistry.get(serverLevel).updateSnapshot(getNetworkId(entity), createSnapshot(entity));
            }
        }
        // Autoplay Engine (Runs every tick)
        if (entity.autoplayEnabled) {
            // Something is playing, wait for it to end
            if (entity.songStartTick != -1 && entity.songDurationTicks > 0) {
                long elapsed = level.getGameTime() - entity.songStartTick;
                if (elapsed >= entity.songDurationTicks + 10) {
                    entity.playNextInQueue();
                }
            } 
            //Nothing is playing, but Autoplay is ON. Start the first disc!
            else if (entity.songStartTick == -1) {
                entity.playNextInQueue();
            }
        }
    }

    public static ControllerSnapshot createSnapshot(AutoplayControllerBlockEntity ctrl)
    {
        ItemStack discId = null;
        if(ctrl.currentlyPlayingEntry != null && ctrl.currentlyPlayingEntry.stack() != null)
        {
            discId = ctrl.currentlyPlayingEntry.stack();
        }
        List<ItemStack> playlistIds = new ArrayList<>();
        for(PlaylistEntry entry : PlaylistHelper.buildPlaylist(ctrl))
        {
            playlistIds.add(entry.stack());
        }
        return new ControllerSnapshot(
            ctrl.networkId,
            ctrl.getBlockPos(),
            discId,
            playlistIds,
            ctrl.currentPlaylistIndex,
            ctrl.autoplayEnabled,
            ctrl.songStartTick,
            ctrl.songDurationTicks
        );
    }

    private void validateAndProcess(Level level, BlockPos pos)
    {
        if (!this.linkedRackPositions.isEmpty())
        {
            Iterator<BlockPos> iterator = this.linkedRackPositions.iterator();
            while (iterator.hasNext()) {
                BlockPos rackPos = iterator.next();
                if (!level.isLoaded(rackPos)) {
                    continue;
                }
                BlockEntity targetBE = level.getBlockEntity(rackPos);
                if (!(targetBE instanceof DiscRackBlockEntity)) {
                    iterator.remove();
                    this.setChanged();
                }
            }
        }
        
        BlockPos jukeboxPos = JukeboxHelper.findJukebox(this);
        int newStatus = JukeboxHelper.CheckJukeStatus(this, jukeboxPos);
        boolean isManualSlot = this.currentlyPlayingEntry != null && this.currentlyPlayingEntry.rackPos().equals(BlockPos.ZERO) && this.currentlyPlayingEntry.slotIndex() == -1;
        // Manual Jukebox Check
        if (newStatus == 2 && this.currentlyPlayingEntry == null && jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox)
        {
            ItemStack manualDisc = jukebox.getTheItem();
            if (!manualDisc.isEmpty())
            {
                this.currentlyPlayingEntry = new PlaylistEntry(BlockPos.ZERO, -1, manualDisc.copy());
                Optional<Holder<JukeboxSong>> songHolderOpt = JukeboxSong.fromStack(level.registryAccess(), manualDisc);
                if (songHolderOpt.isPresent()) {
                    this.songDurationTicks = songHolderOpt.get().value().lengthInTicks();
                    if (this.songDurationTicks <= 0) this.songDurationTicks = 6000;
                } else if (!BuiltInRegistries.ITEM.getKey(manualDisc.getItem()).getNamespace().equals("minecraft")) {
                    this.songDurationTicks = 6000;
                } else {
                    this.songDurationTicks = 0;
                }
                this.songStartTick = level.getGameTime();
                this.setChanged();
            }
        }
        else if (newStatus != 2 && isManualSlot)
        {
            this.currentlyPlayingEntry = null;
            this.songStartTick = -1;
            this.songDurationTicks = 0;
            this.setChanged();
        }
        if (this.cachedStatus != newStatus) {
            if (newStatus != 2)
            {
                LinkHelper.broadcastToSpeakers(this,false, null);
            }
            this.data.set(2,newStatus);
            this.setChanged();
            if (level instanceof ServerLevel) {
                level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    //#endregion
    //#region NBT Data Storage
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) 
    {
        super.saveAdditional(tag, registries);
        ListTag queueTag = new ListTag();
        ListTag exclusionTag = new ListTag();
        long[] positions = linkedRackPositions.stream().mapToLong(BlockPos::asLong).toArray();
        long[] Speakerpositions = linkedSpeakers.stream().mapToLong(BlockPos::asLong).toArray();
        // Queue Order
        for (DiscIdentity item : customQueueOrder)
        {
            CompoundTag disc = new CompoundTag();

            disc.putString("item",item.itemId().toString());
            if(item.variant()!=null)
            {
                disc.putString("variant",item.variant());
            }
            if(item.InstanceId()!=null)
            {
                disc.putString("InstanceId",item.variant());
            }
            queueTag.add(disc);
        }
        //Save Exclusions
        for (DiscIdentity item : excludedTracks)
        {
            CompoundTag disc = new CompoundTag();
            disc.putString("item",item.itemId().toString());
            if(item.variant()!=null)
            {
                disc.putString("variant",item.variant());
            }
            if(item.InstanceId()!=null)
            {
                disc.putString("InstanceId",item.variant());
            }
            exclusionTag.add(disc);
        }
        //save currentlyplaying
        if(currentlyPlayingEntry != null && currentlyPlayingEntry.stack() != null && !currentlyPlayingEntry.stack().isEmpty())
        {
            CompoundTag playing = new CompoundTag();
            playing.putLong("rack",currentlyPlayingEntry.rackPos().asLong());
            playing.putInt("slot",currentlyPlayingEntry.slotIndex());
            CompoundTag stackTag = new CompoundTag();
            currentlyPlayingEntry.stack().save(registries, stackTag);
            playing.put("stack", stackTag);
            tag.put("currentlyPlaying",playing);
        }
        tag.putLongArray("LinkedRacks", positions);
        tag.putLongArray("linkedSpeakers", Speakerpositions);
        tag.putLong("SongStart", songStartTick);
        tag.putInt("SongDuration", songDurationTicks);
        tag.putInt("CurrentPlayingSlot", currentPlaylistIndex);
        tag.putBoolean("autoplayEnabled", this.autoplayEnabled);
        tag.put("excludedTracks", exclusionTag);
        tag.put("queueOrder", queueTag);
        tag.putInt("CachedStatus", this.cachedStatus);
        tag.putUUID("networkId", this.networkId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) 
    {
        super.loadAdditional(tag, registries);
        linkedRackPositions.clear();
        long[] _positions = tag.getLongArray("LinkedRacks");
        for (long posLong : _positions) {
            linkedRackPositions.add(BlockPos.of(posLong));
        }
        linkedSpeakers.clear();
        long[] _linkedSpeakers = tag.getLongArray("linkedSpeakers");
        for (long posLong : _linkedSpeakers) {
            linkedSpeakers.add(BlockPos.of(posLong));
        }
        this.songStartTick = tag.getLong("SongStart");
        this.songDurationTicks = tag.getInt("SongDuration");
        this.currentPlaylistIndex = tag.getInt("CurrentPlayingSlot");
        this.cachedStatus = tag.getInt("CachedStatus");
        this.autoplayEnabled = tag.getBoolean("autoplayEnabled");
        this.networkId = tag.getUUID("networkId");
        if(tag.contains("currentlyPlaying"))
        {
            CompoundTag playing = tag.getCompound("currentlyPlaying");
            ItemStack stack =ItemStack.parseOptional(registries,playing.getCompound("stack"));
            currentlyPlayingEntry = new PlaylistEntry(BlockPos.of(playing.getLong("rack")),playing.getInt("slot"),stack);
        }
        else
        {
            currentlyPlayingEntry = null;
        }
        customQueueOrder.clear();
        ListTag queueTag = tag.getList("queueOrder", 10); // 8 is the ID for StringTag
        for(int i=0;i<queueTag.size();i++)
        {
            CompoundTag disc = queueTag.getCompound(i);
            customQueueOrder.add(new DiscIdentity(ResourceLocation.parse(disc.getString("item")),disc.contains("variant")? disc.getString("variant"): null,disc.contains("InstanceId")? disc.getString("InstanceId"): null));
        }
        excludedTracks.clear();
        ListTag exclusionTag = tag.getList("excludedTracks", 10);
        for(int i=0;i<exclusionTag.size();i++)
        {
            CompoundTag disc = exclusionTag.getCompound(i);
            excludedTracks.add( new DiscIdentity(ResourceLocation.parse(disc.getString("item")),disc.contains("variant")? disc.getString("variant"): null,disc.contains("InstanceId")? disc.getString("InstanceId"): null));
        }
    }
    ////#region
    public static UUID getNetworkId(AutoplayControllerBlockEntity Ctrl)
    {
        if (Ctrl.networkId == null) 
        {
            Ctrl.networkId = UUID.randomUUID();
            GlobalPos pos = GlobalPos.of(Ctrl.getLevel().dimension(),Ctrl.getBlockPos());
            ControllerRegistry.get((ServerLevel)Ctrl.getLevel()).register(Ctrl.networkId, pos);
        }
        Ctrl.setChanged();
        return Ctrl.networkId;
    }
    public BlockPos getworldposition ()
    {
        return this.worldPosition;
    }
    //#endregion

    // used for moving playlist entry in queue, mainly for autoplay purposes
    public void moveInQueue(int playlistIndex, int direction) {
        DiscIdentity id = CtrlHelper.resolveIdentity(this,playlistIndex, true);
        if (id == null) return;
        int index = customQueueOrder.indexOf(id);
        if (index == -1) { customQueueOrder.add(id); index = customQueueOrder.size() - 1; }
        int newIndex = index + direction;
        if (newIndex < 0 || newIndex >= customQueueOrder.size()) return;
        Collections.swap(customQueueOrder, index, newIndex);
        this.setChanged();
        if (level != null && !level.isClientSide) 
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            PlaylistHelper.broadcastPlaylistUpdate(this);
        }
    }
    //#endregion
    //#region Main Packet
    public void tryPlayDisc(int playlistIndex) {
        if (level == null || level.isClientSide) {
            return;
        }
        List<PlaylistEntry> playlist = PlaylistHelper.buildPlaylist(this);
        if (playlistIndex < 0 || playlistIndex >= playlist.size()) {
            return;
        }
        PlaylistEntry entry = playlist.get(playlistIndex);
        DiscRackBlockEntity rack = LinkHelper.getRack(this,entry.rackPos());
        if (rack == null) {
            return;
        }
        ItemStack discInRack = rack.getItem(entry.slotIndex());
        if (discInRack.isEmpty()) {
            return;
        }
        BlockPos jukeboxPos = JukeboxHelper.findJukebox(this);
        if (jukeboxPos == null) {
            return;
        }
        if (!(level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }
        JukeboxHelper.clearJukebox(this,jukebox, jukeboxPos);
        ItemStack refreshedDisc = rack.getItem(entry.slotIndex());
        if (refreshedDisc.isEmpty()) {
            return;
        }
        ItemStack discCopy = refreshedDisc.split(1);
        jukebox.setTheItem(discCopy);
        // Get Song Details
        Optional<Holder<JukeboxSong>> songHolderOpt = JukeboxSong.fromStack(level.registryAccess(), discCopy);
        if (songHolderOpt.isPresent()) {
            Holder<JukeboxSong> songHolder = songHolderOpt.get();
            this.songDurationTicks = songHolder.value().lengthInTicks();
            if (this.songDurationTicks <= 0) 
            {
                // Fallback to a standard 6-minute track length
                this.songDurationTicks = 6000; 
            }
        } 
        else if (BuiltInRegistries.ITEM.getKey(discCopy.getItem()).getNamespace() != "minecraft")
        {
            this.songDurationTicks = 6000; 
        }
        else {
            this.songDurationTicks = 0;
        }
        this.songStartTick = level.getGameTime();
        //
        currentPlaylistIndex = playlistIndex;
        currentlyPlayingEntry = new PlaylistEntry(entry.rackPos(),entry.slotIndex(),discCopy.copy());
        BlockState state = level.getBlockState(jukeboxPos);
        BlockState oldState = state;
        BlockState newState = state;
        if (state.hasProperty(JukeboxBlock.HAS_RECORD)) {
            newState = state.setValue(JukeboxBlock.HAS_RECORD, true);
            level.setBlock(jukeboxPos, newState, 3);
        }
        LinkHelper.broadcastToSpeakers(this,true, discCopy.copy());
        TerminalControlHandler.broadcastToTerminalOnline((ServerLevel) level, networkId, true,discCopy.copy());
        rack.setChanged();
        level.sendBlockUpdated(rack.getBlockPos(),rack.getBlockState(),rack.getBlockState(),3);
        level.levelEvent(null, 1010, jukeboxPos, Item.getId(discCopy.getItem()));
        jukebox.setChanged();
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        level.sendBlockUpdated(jukeboxPos,oldState,newState,3);
        PlaylistHelper.broadcastPlaylistUpdate(this);
    }
    public void stopAndReturnDisc() 
    {
        if (level == null || level.isClientSide) return;
        BlockPos jukeboxPos = JukeboxHelper.findJukebox(this);
        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
            JukeboxHelper.clearJukebox(this,jukebox, jukeboxPos);
            this.songStartTick = -1;
            this.songDurationTicks = 0;
            jukebox.setChanged();
            this.setChanged();
            level.sendBlockUpdated(jukeboxPos, level.getBlockState(jukeboxPos), level.getBlockState(jukeboxPos), 3);
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            LinkHelper.broadcastToSpeakers(this,false,null);
            TerminalControlHandler.broadcastToTerminalOnline((ServerLevel) level, networkId, false, null);
            PlaylistHelper.broadcastPlaylistUpdate(this);
        }      
    }
    public void playNextInQueue() 
    {
        List<PlaylistEntry> fullPlaylist = PlaylistHelper.buildPlaylist(this);
        if (fullPlaylist.isEmpty()) return;
        int startIndex = this.currentPlaylistIndex;
        int nextIndex = this.currentPlaylistIndex;
        int attempts = 0;
        while (attempts < fullPlaylist.size()) {
            nextIndex = (nextIndex + 1) % fullPlaylist.size();
            attempts++;
            if (!CtrlHelper.isExcluded(this,nextIndex) && CtrlHelper.getCustomOrder(this,nextIndex) != startIndex)
            {
                tryPlayDisc(nextIndex);
                return;
            }
        }
        this.stopAndReturnDisc();
    }
    //#endregion
}