package com.rtsbuilding.rtsbuilding.compat.sophisticatedstorage;

import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class RtsSophisticatedStorageCompat {
    private static final String MOD_ID = "sophisticatedstorage";
    private static final String MENU_CLASS_PREFIX = "net.p3pp3rf1y.sophisticatedstorage.common.gui.";

    private RtsSophisticatedStorageCompat() {
    }

    public static AbstractContainerMenu wrapRemoteMenu(AbstractContainerMenu menu) {
        if (!isSupportedRemoteMenu(menu)) {
            return menu;
        }
        return menu instanceof StillValidBypassMenu ? menu : new StillValidBypassMenu(menu);
    }

    public static boolean isSupportedRemoteMenu(AbstractContainerMenu menu) {
        if (menu == null || !ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        AbstractContainerMenu unwrapped = unwrap(menu);
        return unwrapped.getClass().getName().startsWith(MENU_CLASS_PREFIX);
    }

    private static AbstractContainerMenu unwrap(AbstractContainerMenu menu) {
        AbstractContainerMenu current = menu;
        while (current instanceof StillValidBypassMenu wrapped) {
            current = wrapped.delegate;
        }
        return current;
    }

    private static final class StillValidBypassMenu extends AbstractContainerMenu {
        private final AbstractContainerMenu delegate;

        private StillValidBypassMenu(AbstractContainerMenu delegate) {
            super(null, delegate.containerId);
            this.delegate = delegate;
            syncVisibleState();
        }

        private void syncVisibleState() {
            this.slots.clear();
            this.slots.addAll(this.delegate.slots);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            ItemStack moved = this.delegate.quickMoveStack(player, index);
            syncVisibleState();
            return moved;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            this.delegate.clicked(slotId, button, clickType, player);
            syncVisibleState();
        }

        @Override
        public void removed(Player player) {
            this.delegate.removed(player);
            syncVisibleState();
        }

        @Override
        public void broadcastChanges() {
            this.delegate.broadcastChanges();
            syncVisibleState();
        }

        @Override
        public void sendAllDataToRemote() {
            this.delegate.sendAllDataToRemote();
            syncVisibleState();
        }

        @Override
        public void slotsChanged(Container container) {
            this.delegate.slotsChanged(container);
            syncVisibleState();
        }

        @Override
        public void setItem(int slotId, int stateId, ItemStack stack) {
            this.delegate.setItem(slotId, stateId, stack);
            syncVisibleState();
        }

        @Override
        public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
            this.delegate.initializeContents(stateId, items, carried);
            syncVisibleState();
        }

        @Override
        public void setCarried(ItemStack stack) {
            this.delegate.setCarried(stack);
        }

        @Override
        public ItemStack getCarried() {
            return this.delegate.getCarried();
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
            return this.delegate.canTakeItemForPickAll(stack, slot);
        }

        @Override
        public Slot getSlot(int slotId) {
            return this.delegate.getSlot(slotId);
        }
    }
}
