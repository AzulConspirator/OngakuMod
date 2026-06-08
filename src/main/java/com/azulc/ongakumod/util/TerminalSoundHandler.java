package com.azulc.ongakumod.util;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = OngakuMod.MODID, value = Dist.CLIENT)
public class TerminalSoundHandler 
{
    private static final int SOUND_STOP_CHECK_INTERVAL = 10;
    private static final Map<UUID, SoundInstance> activeTerminalSounds = new ConcurrentHashMap<>();
    private static long lastPlaybackChecked = 0;

    private TerminalSoundHandler() {}

    public static void playSound(UUID controllerId, SoundInstance sound) {
        stopSound(controllerId);
        activeTerminalSounds.put(controllerId, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void stopSound(UUID controllerId) {
        if (activeTerminalSounds.containsKey(controllerId)) {
            Minecraft.getInstance().getSoundManager().stop(activeTerminalSounds.remove(controllerId));
        }
    }

    // --- MODE 1: BLOCK MODE (Static Position Attenuation) ---
    public static void playBlockModeSound(UUID controllerId, SoundEvent soundEvent,ItemStack Disc, BlockPos pos) {
        // SimpleSoundInstance.forJukeboxSong scales volume and creates standard jukebox attenuation dropoffs
        playSound(controllerId, SimpleSoundInstance.forJukeboxSong(soundEvent, Vec3.atCenterOf(pos)));
    }

    // --- MODE 2: ITEM MODE / MP3 MODE (Entity-Bound Attenuation) ---
    public static void playItemModeSound(UUID controllerId, SoundEvent soundEvent,ItemStack Disc, int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            stopSound(controllerId);
            return;
        }

        playSound(controllerId, new EntityBoundSoundInstance(soundEvent, SoundSource.RECORDS, 1.0F, 1.0F, entity, level.random.nextLong()) 
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
            lastPlaybackChecked = 0;
        }
    }
}