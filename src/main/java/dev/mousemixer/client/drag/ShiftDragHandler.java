package dev.mousemixer.client.drag;

import dev.mousemixer.Config;
import dev.mousemixer.client.click.ClickQueue;
import dev.mousemixer.client.click.ContainerClicker;
import dev.mousemixer.client.click.ScreenHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Holding shift and dragging the cursor with the left mouse button quick-moves every slot the
 * cursor passes over to the opposite section. Re-entering a slot fires again, allowing continuous
 * back-and-forth movement between two containers.
 */
public final class ShiftDragHandler {
    private static boolean dragging;
    /** Index of the last slot the cursor was inside; -1 when not dragging or between slots. */
    private static int lastSlotIdx = -1;

    private ShiftDragHandler() {}

    @SubscribeEvent
    public static void onMouseButton(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!Config.ENABLE_SHIFT_DRAG.get()) return;
        if (event.getButton() != 0) return;
        if (!Screen.hasShiftDown()) return;
        if (Screen.hasControlDown() || Screen.hasAltDown()) return; // Ctrl/Alt combos go to ModifierClickHandler
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof CreativeModeInventoryScreen) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (!hasTwoSections(menu)) return;

        Slot hovered = ScreenHelper.findSlotUnder(screen, event.getMouseX(), event.getMouseY());
        // Recipe book buttons and other non-slot UI areas return null — let vanilla handle those
        // (shift-clicking a recipe button to learn/craft it must not be intercepted).
        // Empty inventory slots DO return a non-null Slot and should still start a drag so the
        // user can begin the gesture on an empty cell and sweep into filled ones.
        if (hovered == null) return;

        int idx = menu.slots.indexOf(hovered);
        if (idx < 0) return;

        dragging = true;
        lastSlotIdx = -1;
        ClickQueue.beginBatch();

        if (hovered.hasItem()) {
            lastSlotIdx = idx;
            ClickQueue.enqueue(() -> ContainerClicker.quickMove(idx));
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!dragging) return;
        if (event.getMouseButton() != 0) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        if (!Screen.hasShiftDown()) {
            dragging = false;
            lastSlotIdx = -1;
            return;
        }

        Slot hovered = ScreenHelper.findSlotUnder(screen, event.getMouseX(), event.getMouseY());
        if (hovered == null) {
            // Cursor left all slots — reset so re-entering a slot fires again.
            lastSlotIdx = -1;
            event.setCanceled(true);
            return;
        }

        AbstractContainerMenu menu = screen.getMenu();
        int idx = menu.slots.indexOf(hovered);
        if (idx < 0 || idx == lastSlotIdx) {
            event.setCanceled(true);
            return;
        }

        lastSlotIdx = idx;
        if (hovered.hasItem()) {
            ClickQueue.enqueue(() -> ContainerClicker.quickMove(idx));
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 0) return;
        dragging = false;
        lastSlotIdx = -1;
    }

    private static boolean hasTwoSections(AbstractContainerMenu menu) {
        if (menu.slots.isEmpty()) return false;
        var first = menu.slots.get(0).container;
        for (Slot slot : menu.slots) {
            if (slot.container != first) return true;
        }
        return false;
    }
}
