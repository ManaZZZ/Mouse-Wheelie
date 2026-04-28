package dev.mousemixer.client.refill;

import dev.mousemixer.Config;
import dev.mousemixer.client.click.ClickQueue;
import dev.mousemixer.client.click.ContainerClicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Detects when the main hand or off hand has just emptied during normal gameplay (no screen open)
 * and swaps a matching stack from the main inventory into that slot via a vanilla SWAP click.
 */
public final class StackRefillHandler {
    private static ItemStack lastMainHand = ItemStack.EMPTY;
    private static ItemStack lastOffHand = ItemStack.EMPTY;
    private static int lastSelected = -1;

    private StackRefillHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.ENABLE_REFILL.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        // Only when no GUI is showing (the player is using items in the world).
        if (mc.screen != null) {
            capture(player);
            return;
        }

        // Don't fight a click batch that's still processing.
        if (!ClickQueue.isEmpty()) {
            capture(player);
            return;
        }

        // Player switched hotbar slot - no refill, just refresh state.
        int selected = player.getInventory().selected;
        if (selected != lastSelected) {
            capture(player);
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.isEmpty() && !lastMainHand.isEmpty()) {
            tryRefillMainHand(player, lastMainHand);
        }
        if (offHand.isEmpty() && !lastOffHand.isEmpty()) {
            tryRefillOffHand(player, lastOffHand);
        }

        capture(player);
    }

    private static void capture(LocalPlayer player) {
        lastMainHand = player.getMainHandItem().copy();
        lastOffHand = player.getOffhandItem().copy();
        lastSelected = player.getInventory().selected;
    }

    private static void tryRefillMainHand(LocalPlayer player, ItemStack template) {
        int sourceSlot = findRefillSource(player.inventoryMenu, template);
        if (sourceSlot < 0) return;
        ContainerClicker.swapWithHotbar(sourceSlot, player.getInventory().selected);
    }

    private static void tryRefillOffHand(LocalPlayer player, ItemStack template) {
        int sourceSlot = findRefillSource(player.inventoryMenu, template);
        if (sourceSlot < 0) return;
        ContainerClicker.swapWithOffhand(sourceSlot);
    }

    /**
     * Look in the player's main inventory (slots 9..35 of {@code inventoryMenu}) for a stack
     * suitable to refill {@code template}. Strict component match is preferred so we don't pull
     * an unenchanted backup over an enchanted original; we fall back to a loose item match so
     * tools that took damage can be replaced by a fresh copy.
     */
    private static int findRefillSource(AbstractContainerMenu menu, ItemStack template) {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, template)) return i;
        }
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (stack.getItem() == template.getItem()) return i;
        }
        return -1;
    }
}
