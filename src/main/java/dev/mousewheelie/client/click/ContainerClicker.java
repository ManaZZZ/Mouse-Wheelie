package dev.mousewheelie.client.click;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

/**
 * Sends container clicks via the vanilla packet path. Pure client-side: every helper here ends up
 * calling {@link net.minecraft.client.multiplayer.MultiPlayerGameMode#handleInventoryMouseClick}
 * which both updates the local menu state and dispatches a {@code ServerboundContainerClickPacket}.
 */
public final class ContainerClicker {
    private ContainerClicker() {}

    /** Send a click against the player's currently open menu. */
    public static void click(int slotId, int button, ClickType clickType) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;
        click(menu, slotId, button, clickType);
    }

    /** Send a click against a specific menu (by container id). */
    public static void click(AbstractContainerMenu menu, int slotId, int button, ClickType clickType) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slotId, button, clickType, player);
    }

    public static void leftClick(int slotId) {
        click(slotId, 0, ClickType.PICKUP);
    }

    public static void rightClick(int slotId) {
        click(slotId, 1, ClickType.PICKUP);
    }

    /** Vanilla shift-click: moves the stack to the other inventory section. */
    public static void quickMove(int slotId) {
        click(slotId, 0, ClickType.QUICK_MOVE);
    }

    /** Drop one item from the stack at slotId (Q-key equivalent). */
    public static void dropOne(int slotId) {
        click(slotId, 0, ClickType.THROW);
    }

    /** Drop the entire stack at slotId (Ctrl+Q equivalent). */
    public static void dropStack(int slotId) {
        click(slotId, 1, ClickType.THROW);
    }

    /** Swap the stack at slotId with a hotbar slot (0..8). */
    public static void swapWithHotbar(int slotId, int hotbarIndex) {
        click(slotId, hotbarIndex, ClickType.SWAP);
    }

    /** Swap the stack at slotId with the off-hand. */
    public static void swapWithOffhand(int slotId) {
        click(slotId, Inventory.SLOT_OFFHAND, ClickType.SWAP);
    }
}
