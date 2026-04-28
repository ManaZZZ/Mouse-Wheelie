package dev.mousemixer.client.modifier;

import dev.mousemixer.Config;
import dev.mousemixer.MouseMixer;
import dev.mousemixer.client.click.ClickQueue;
import dev.mousemixer.client.click.ContainerClicker;
import dev.mousemixer.client.click.ScreenHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Intercepts left-clicks and left-button drags in container screens when modifier keys are held:
 * <ul>
 *   <li>Ctrl+click / Ctrl+drag: quick-move every stack of each hovered item type to the other section</li>
 *   <li>Alt+click / Alt+drag: transfer exactly one item from each hovered slot to the other section</li>
 * </ul>
 */
public final class ModifierClickHandler {
    private static boolean dragging;
    private static boolean ctrlMode;
    private static int lastSlotIdx = -1;

    private ModifierClickHandler() {}

    @SubscribeEvent
    public static void onMouseButton(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!Config.ENABLE_MODIFIER_CLICKS.get()) return;
        if (event.getButton() != 0) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof CreativeModeInventoryScreen) return;

        boolean ctrl = Screen.hasControlDown();
        boolean alt = Screen.hasAltDown();
        if (!ctrl && !alt) return;
        if (Screen.hasShiftDown()) return;

        AbstractContainerMenu menu = screen.getMenu();
        Slot hovered = ScreenHelper.findSlotUnder(screen, event.getMouseX(), event.getMouseY());

        // Always start drag tracking when modifier+LMB is pressed inside a container screen, even
        // on dead space — that way the user can press first and sweep over slots afterwards.
        dragging = true;
        ctrlMode = ctrl;
        lastSlotIdx = -1;
        ClickQueue.beginBatch();

        if (hovered != null && hovered.hasItem()) {
            int hoveredIdx = menu.slots.indexOf(hovered);
            if (hoveredIdx >= 0) {
                lastSlotIdx = hoveredIdx;
                ItemStack template = hovered.getItem().copy();
                Container section = hovered.container;
                if (ctrl) {
                    ClickQueue.enqueue(() -> moveAllMatching(menu, template, section));
                } else {
                    Container other = findOtherSection(menu, section);
                    if (other != null) ClickQueue.enqueue(() -> transferOneItem(menu, hoveredIdx, other));
                }
            }
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!dragging) return;
        if (event.getMouseButton() != 0) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        if (!Screen.hasControlDown() && !Screen.hasAltDown()) {
            dragging = false;
            lastSlotIdx = -1;
            return;
        }

        Slot hovered = ScreenHelper.findSlotUnder(screen, event.getMouseX(), event.getMouseY());
        if (hovered == null) {
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
            ItemStack template = hovered.getItem().copy();
            Container section = hovered.container;
            if (ctrlMode) {
                ClickQueue.enqueue(() -> moveAllMatching(menu, template, section));
            } else {
                Container other = findOtherSection(menu, section);
                if (other != null) ClickQueue.enqueue(() -> transferOneItem(menu, idx, other));
            }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 0) return;
        dragging = false;
        lastSlotIdx = -1;
    }

    /** Quick-move every stack matching {@code template} that lives in {@code section}. */
    private static void moveAllMatching(AbstractContainerMenu menu, ItemStack template, Container section) {
        // Snapshot indices first; quick-moving mutates slot contents but indices stay valid.
        int size = menu.slots.size();
        int moved = 0;
        for (int i = 0; i < size; i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container != section) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(stack, template)) continue;
            ContainerClicker.quickMove(i);
            moved++;
        }
        MouseMixer.LOGGER.debug("Ctrl+LMB moved {} stack(s) of {}", moved, template.getItem());
    }

    /**
     * Transfer exactly one item from {@code sourceIdx} into {@code destSection}.
     * If the source has only one item, vanilla quick-move is used directly.
     * Otherwise: pick up stack, right-click destination to drop one, left-click source to return rest.
     */
    private static void transferOneItem(AbstractContainerMenu menu, int sourceIdx, Container destSection) {
        ItemStack at = menu.slots.get(sourceIdx).getItem();
        if (at.isEmpty()) return;

        if (at.getCount() <= 1) {
            ContainerClicker.quickMove(sourceIdx);
            return;
        }

        int destIdx = findInsertionSlot(menu, at, destSection);
        if (destIdx < 0) {
            ContainerClicker.quickMove(sourceIdx);
            return;
        }

        ContainerClicker.leftClick(sourceIdx);
        ContainerClicker.rightClick(destIdx);
        ContainerClicker.leftClick(sourceIdx);
    }

    /**
     * Find a slot in {@code destSection} that can accept {@code template}: prefer a partial stack
     * with room, else the first empty slot.
     */
    private static int findInsertionSlot(AbstractContainerMenu menu, ItemStack template, Container destSection) {
        int firstEmpty = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container != destSection) continue;
            if (!slot.mayPlace(template)) continue;
            ItemStack at = slot.getItem();
            if (at.isEmpty()) {
                if (firstEmpty < 0) firstEmpty = i;
            } else if (ItemStack.isSameItemSameComponents(at, template)) {
                int cap = Math.min(template.getMaxStackSize(), slot.getMaxStackSize(at));
                if (at.getCount() < cap) return i;
            }
        }
        return firstEmpty;
    }

    private static Container findOtherSection(AbstractContainerMenu menu, Container source) {
        for (Slot slot : menu.slots) {
            if (slot.container != source) return slot.container;
        }
        return null;
    }
}
