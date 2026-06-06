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
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public class TerminalMenu extends AbstractContainerMenu {
    private final TerminalBlockEntity terminal;
    private final UUID networkId;
    private final ControllerRegistry.ControllerSnapshot snapshot; 
    // FIX: Declare clean without hardcoding null references
    private final Optional<BlockPos> terminalBlockPos;

    // Dynamic Packet/Buffer Decoder constructor (RUNS ON CLIENT ONLY)
    public TerminalMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(OngakuMod.TERMINAL_MENU.get(), id);
        
        // 1. Read the boolean flag
        boolean openedFromItem = buf.readBoolean();
        
        // 2. Read UUID
        this.networkId = buf.readUUID();
        
        // 3. Read BlockPos only if it's NOT an item
        if (!openedFromItem) {
            BlockPos pos = buf.readBlockPos();
            this.terminalBlockPos = Optional.of(pos);
            this.terminal = (TerminalBlockEntity) inv.player.level().getBlockEntity(pos);
        } else {
            this.terminalBlockPos = Optional.empty();
            this.terminal = null;
        }
        
        // 4. Read Snapshot flag
        boolean hasSnapshot = buf.readBoolean();
        
        // 5. Read snapshot
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
        this.terminalBlockPos = Optional.empty();
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
        this.terminalBlockPos = Optional.of(terminal.getBlockPos());
        if (inv.player.level() instanceof ServerLevel serverLevel && this.networkId != null) { 
            this.snapshot = ControllerRegistry.get(serverLevel).getSnapshot(this.networkId); 
        } else {
            this.snapshot = null;
        }
    }

    public Optional<BlockPos> getTerminalBlockPos() {
        return this.terminalBlockPos;
    }

    public ControllerSnapshot getSnapshot() { return snapshot; }
    public UUID getNetworkId() { return networkId; }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}