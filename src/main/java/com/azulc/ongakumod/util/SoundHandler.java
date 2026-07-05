package com.azulc.ongakumod.util;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.compat.EtchedBridge;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = OngakuMod.MODID, value = Dist.CLIENT)
public class SoundHandler
{
    private static final int SOUND_STOP_CHECK_INTERVAL = 10;
    private static final long MIN_REPLAY_INTERVAL_MS = 150L;
    private record SoundKey(UUID controllerId, Optional<BlockPos> pos) {}
    private record PlaybackRecord(ResourceLocation soundLocation, long startedAtMs) {}

    private static final Map<SoundKey, SoundInstance> activeTerminalSounds = new ConcurrentHashMap<>();
    private static final Map<SoundKey, PlaybackRecord> lastPlayback = new ConcurrentHashMap<>();
    private static long lastPlaybackChecked = 0;

    private SoundHandler() {}

    private static void playSound(SoundKey key, SoundInstance sound) {
        long now = Util.getMillis();
        PlaybackRecord last = lastPlayback.get(key);

        // #4: if this exact key+track was just (re)started a moment ago, treat this
        // as a duplicate burst (e.g. multiple speakers updating in the same tick,
        // or a resend) rather than a genuine new play request.
        if (last != null && last.soundLocation().equals(sound.getLocation()) && now - last.startedAtMs() < MIN_REPLAY_INTERVAL_MS) { return; }
        stopSound(key);
        activeTerminalSounds.put(key, sound);
        lastPlayback.put(key, new PlaybackRecord(sound.getLocation(), now));
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void stopSound(SoundKey key) {
        SoundInstance existing = activeTerminalSounds.remove(key);
        if (existing != null) {
            Minecraft.getInstance().getSoundManager().stop(existing);
        }
    }

    // Public stop entry point used from ClientPayloadHandler.
    public static void stopSound(UUID controllerId, Optional<BlockPos> pos) {
        stopSound(new SoundKey(controllerId, pos));
    }

    // --- MODE 1: BLOCK MODE (Static Position Attenuation) ---
    public static void playBlockModeSound(UUID controllerId, Optional<ItemStack> Disc, BlockPos pos) {
        ItemStack disc = Disc.orElse(ItemStack.EMPTY);
        if (disc == null || disc.isEmpty()) return;
        SoundEvent soundEvent = null;
        if (!disc.isEmpty()) {
            soundEvent = getSoundFromDiscId(Minecraft.getInstance().level, disc);
        }
        SoundKey key = new SoundKey(controllerId, Optional.of(pos));
        if (OngakuMod.IS_ETCHED_LOADED && LinkHelper.hasComponentByString(disc, "etched:music")) {
            ItemStack stack = Disc.get();
            if (EtchedBridge.hasEtchedMusic(stack)) {
                EtchedBridge.createEtchedBlockSound(pos, stack, Minecraft.getInstance().level).ifPresent(sound -> playSound(key, sound));
                return;
            }
        }
        playSound(key, SimpleSoundInstance.forJukeboxSong(soundEvent, Vec3.atCenterOf(pos)));
    }

    // --- MODE 2: ITEM MODE / MP3 MODE (Entity-Bound Attenuation) ---
    public static void playItemModeSound(UUID controllerId, Optional<ItemStack> Disc, int entityId) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) return;

        Entity entity = level.getEntity(entityId);
        SoundKey key = new SoundKey(controllerId, Optional.empty());
        if (entity == null) {
            stopSound(key);
            return;
        }
        ItemStack disc = Disc.orElse(ItemStack.EMPTY);
        if (disc == null || disc.isEmpty()) return;
        SoundEvent soundEvent = null;
        if (!disc.isEmpty()) {
            soundEvent = getSoundFromDiscId(Minecraft.getInstance().level, disc);
        }
        if (OngakuMod.IS_ETCHED_LOADED && LinkHelper.hasComponentByString(disc, "etched:music")) {
            ItemStack stack = Disc.get();
            if (EtchedBridge.hasEtchedMusic(stack)) {
                EtchedBridge.createEtchedEntitySound(entity, stack).ifPresent(sound -> playSound(key, sound));
                return;
            }
        }
        playSound(key, new EntityBoundSoundInstance(soundEvent, SoundSource.RECORDS, 1.0F, 1.0F, entity, level.random.nextLong())
        {
            @Override
            public void tick()
            {
                super.tick();
                if (entity instanceof Player player) {
                    Vec3 lookAngle = player.getLookAngle();
                    this.x = player.getX() + lookAngle.x * 0.3;
                    this.y = player.getEyeY() + lookAngle.y * 0.3;
                    this.z = player.getZ() + lookAngle.z * 0.3;
                }
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        if (!activeTerminalSounds.isEmpty() && lastPlaybackChecked < level.getGameTime() - SOUND_STOP_CHECK_INTERVAL) {
            lastPlaybackChecked = level.getGameTime();
            activeTerminalSounds.entrySet().removeIf(entry -> !Minecraft.getInstance().getSoundManager().isActive(entry.getValue()));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            activeTerminalSounds.clear();
            lastPlayback.clear();
            lastPlaybackChecked = 0;
        }
    }

    public static SoundEvent getSoundFromDiscId(Level level, ItemStack discId)
    {
        if (discId == null || discId.isEmpty()) {return null;}
        return JukeboxSong.fromStack(level.registryAccess(), discId).map(holder -> holder.value().soundEvent().value()).orElse(null);
    }
}