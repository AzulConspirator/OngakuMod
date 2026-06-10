package com.azulc.ongakumod.container;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.TerminalBlockEntity;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public class TerminalMenu extends AbstractContainerMenu {
    @SuppressWarnings("unused")
    private final TerminalBlockEntity terminal;
    private final UUID networkId;
    
    // REMOVED 'final': This must be hot-swapped dynamically when server syncs occur
    private ControllerSnapshot snapshot; 
    private final Optional<BlockPos> terminalBlockPos;
    private boolean isControllerLoaded = false;
    private boolean isBlockMode = false;

    // Client-side initialization constructor
    public TerminalMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(OngakuMod.TERMINAL_MENU.get(), id);
        boolean openedFromItem = buf.readBoolean();
        this.networkId = buf.readUUID();
        if (!openedFromItem) {
            BlockPos pos = buf.readBlockPos();
            this.terminalBlockPos = Optional.of(pos);
            this.terminal = (TerminalBlockEntity) inv.player.level().getBlockEntity(pos);
        } else {
            this.terminalBlockPos = Optional.empty();
            this.terminal = null;
        }
        boolean hasSnapshot = buf.readBoolean();
        if (hasSnapshot) {
            this.snapshot = ControllerSnapshot.read(buf);
            this.isControllerLoaded = buf.readBoolean();
        } else {
            this.snapshot = null;
        }
    }

    // Server-side Item variant constructor
    public TerminalMenu(int id, Inventory inv, UUID controllerId, boolean isBlockMode) {
        super(OngakuMod.TERMINAL_MENU.get(), id); 
        this.terminal = null;
        this.networkId = controllerId;
        this.terminalBlockPos = Optional.empty();
        this.isBlockMode = isBlockMode;
        if (inv.player.level() instanceof ServerLevel serverLevel) { 
            this.snapshot = ControllerRegistry.get(serverLevel).getSnapshot(controllerId); 
            this.isControllerLoaded = this.snapshot != null && serverLevel.isLoaded(this.snapshot.pos());
        } else {
            this.snapshot = null; 
        }
    }

    // Server-side Block variant constructor
    public TerminalMenu(int id, Inventory inv, TerminalBlockEntity terminal, boolean isBlockMode) {
        super(OngakuMod.TERMINAL_MENU.get(), id); 
        this.terminal = terminal;
        this.networkId = terminal.getNetworkId(); 
        this.terminalBlockPos = Optional.of(terminal.getBlockPos());
        this.isBlockMode = isBlockMode;
        if (inv.player.level() instanceof ServerLevel serverLevel && this.networkId != null) { 
            this.snapshot = ControllerRegistry.get(serverLevel).getSnapshot(this.networkId); 
            this.isControllerLoaded = this.snapshot != null && serverLevel.isLoaded(this.snapshot.pos());
        } else {
            this.snapshot = null;
        }
    }

    /**
     * Network Sync Entrypoint: Called by S2C packet handler to instantly refresh client visuals
     */
    public void clientUpdateSnapshot(ControllerSnapshot newSnapshot, boolean isControllerLoaded) {
        this.snapshot = newSnapshot;
        this.isControllerLoaded = isControllerLoaded;
    }

    public Optional<BlockPos> getTerminalBlockPos() { return this.terminalBlockPos; }
    public Boolean IsControllerLoaded() { return this.isControllerLoaded; }
    public Boolean IsblockMode() { return this.isBlockMode; }
    public ControllerSnapshot getSnapshot() { return snapshot; }
    public UUID getNetworkId() { return networkId; }
    public Boolean IsPlaying(ControllerSnapshot snap) { return snap.currentDisc() !=null; }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}