package com.azulc.ongakumod.container;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.TerminalBlockEntity;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public class TerminalMenu extends AbstractContainerMenu {
    private final TerminalBlockEntity terminal;
    private final UUID networkId;
    private final ControllerRegistry.ControllerSnapshot snapshot; 
    private final Optional<BlockPos> terminalBlockPos = null;

    // Dynamic Packet/Buffer Decoder constructor (RUNS ON CLIENT ONLY)
    public TerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(OngakuMod.TERMINAL_MENU.get(), id);
        boolean openedFromItem = buf.readBoolean();
        
        if (openedFromItem) {
            this.terminal = null;
            this.networkId = buf.readUUID();
        } else {
            this.terminal = (TerminalBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos());
            this.networkId = this.terminal != null ? this.terminal.getNetworkId() : null;
        }
        boolean hasSnapshot = buf.readBoolean();
        if (hasSnapshot) {
            this.snapshot = ControllerSnapshot.read(buf); 
        } else {
            this.snapshot = null; 
        }
    }

    // Direct server-side initialization constructor (Item variant)
    public TerminalMenu(int id, Inventory inv, UUID controllerId) {
        super(OngakuMod.TERMINAL_MENU.get(), id); 
        this.terminal = null;
        this.networkId = controllerId;
        if (inv.player.level() instanceof ServerLevel serverLevel) { 
            this.snapshot = ControllerRegistry.get(serverLevel).getSnapshot(controllerId); 
        } else {
            this.snapshot = null; 
        }
    }

    // Direct server-side initialization constructor (Block variant)
    public TerminalMenu(int id, Inventory inv, TerminalBlockEntity terminal) {
        super(OngakuMod.TERMINAL_MENU.get(), id); 
        this.terminal = terminal;
        this.networkId = terminal.getNetworkId(); 
        if (inv.player.level() instanceof ServerLevel serverLevel && this.networkId != null) { 
            this.snapshot = ControllerRegistry.get(serverLevel).getSnapshot(this.networkId); 
        } else {
            this.snapshot = null;
        }
    }
    public Optional<BlockPos> getTerminalBlockPos() {
    return this.terminalBlockPos;
}
    public ControllerSnapshot getSnapshot() {
        return snapshot; 
    }

    public UUID getNetworkId() {
        return networkId;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; 
    }
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Server-side heartbeat emission routine tracking down active sessions
        if (this._player.level() instanceof ServerPlayer serverPlayer) {
            UUID terminalAudioSessionId = UUID.nameUUIDFromBytes((serverPlayer.getUUID().toString() + "_" + this.getNetworkId().toString()).getBytes());
            
            if (this.getTerminalBlockPos().isPresent()) {
                ServerStorageSoundHandler.updateKeepAlive(
                    terminalAudioSessionId, 
                    serverPlayer.level(), 
                    Vec3.atCenterOf(this.getTerminalBlockPos().get()), 
                    () -> {}
                );
            } else {
                ServerStorageSoundHandler.updateKeepAlive(
                    terminalAudioSessionId, 
                    serverPlayer.level(), 
                    serverPlayer.position(), 
                    () -> {}
                );
            }
        }
    }
}