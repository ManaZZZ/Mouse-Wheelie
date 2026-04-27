package dev.mousewheelie.client.scroll;

import dev.mousewheelie.Config;
import dev.mousewheelie.client.click.ClickQueue;
import dev.mousewheelie.client.click.ContainerClicker;
import dev.mousewheelie.client.click.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Scroll on a slot to move items between sections of an open container. The base direction is
 * controlled by {@link Config#SCROLL_DIRECTIONAL}:
 * <ul>
 *   <li>Hover-relative (default): scroll up SENDS from the hovered section, scroll down RECEIVES into it.</li>
 *   <li>Directional: scroll up moves items toward the upper container (e.g. chest), scroll down toward the lower container (e.g. player inventory), regardless of where the cursor is.</li>
 * </ul>
 * Modifiers in either mode:
 * <ul>
 *   <li>+ Shift = full stack instead of one item</li>
 *   <li>+ Ctrl = act on every matching stack in the source section</li>
 *   <li>+ Alt = drop instead of transfer (direction-agnostic)</li>
 * </ul>
 */
public final class ScrollTransferHandler {
    private ScrollTransferHandler() {}

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!Config.ENABLE_SCROLL_TRANSFER.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof CreativeModeInventoryScreen) return; // creative tabs use scroll

        double dy = event.getScrollDeltaY();
        if (dy == 0.0) return;

        Slot hovered = ScreenHelper.findSlotUnder(screen, event.getMouseX(), event.getMouseY());
        if (hovered == null || !hovered.hasItem()) return;

        AbstractContainerMenu menu = screen.getMenu();
        int hoveredIdx = menu.slots.indexOf(hovered);
        if (hoveredIdx < 0) return;

        ItemStack template = hovered.getItem().copy();
        boolean shift = Screen.hasShiftDown();
        boolean ctrl = Screen.hasControlDown();
        boolean alt = Screen.hasAltDown();
        boolean send = dy > 0;

        Container hoveredSection = hovered.container;
        Container otherSection = findOtherSection(menu, hoveredSection);

        ClickQueue.beginBatch();

        if (alt) {
            // Drop action: scroll direction is irrelevant.
            List<Integer> targets = ctrl
                    ? findAllMatching(menu, template, null)
                    : List.of(hoveredIdx);
            for (int slotIdx : targets) {
                if (shift) {
                    ClickQueue.enqueue(() -> ContainerClicker.dropStack(slotIdx));
                } else {
                    ClickQueue.enqueue(() -> ContainerClicker.dropOne(slotIdx));
                }
            }
            if (!ClickQueue.isEmpty()) event.setCanceled(true);
            return;
        }

        // Transfer action.
        Container sourceSection;
        Container destSection;
        if (Config.SCROLL_DIRECTIONAL.get() && otherSection != null) {
            Container upper = upperSection(menu, hoveredSection, otherSection);
            Container lower = upper == hoveredSection ? otherSection : hoveredSection;
            destSection = send ? upper : lower;
            sourceSection = send ? lower : upper;
        } else {
            sourceSection = send ? hoveredSection : otherSection;
            destSection = send ? otherSection : hoveredSection;
        }
        if (sourceSection == null) return;

        List<Integer> sourceSlots;
        if (ctrl) {
            sourceSlots = findAllMatching(menu, template, sourceSection);
        } else if (hoveredSection == sourceSection) {
            sourceSlots = List.of(hoveredIdx);
        } else {
            int first = findFirstMatching(menu, template, sourceSection);
            sourceSlots = first < 0 ? List.of() : List.of(first);
        }

        for (int sourceIdx : sourceSlots) {
            if (shift) {
                ClickQueue.enqueue(() -> ContainerClicker.quickMove(sourceIdx));
            } else {
                ClickQueue.enqueue(() -> transferOneItem(sourceIdx, destSection));
            }
        }

        if (!ClickQueue.isEmpty()) event.setCanceled(true);
    }

    /** Pull one item from {@code sourceIdx} into the first available slot of {@code destSection}. */
    private static void transferOneItem(int sourceIdx, Container destSection) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null || sourceIdx >= menu.slots.size()) return;

        ItemStack at = menu.slots.get(sourceIdx).getItem();
        if (at.isEmpty()) return;

        if (at.getCount() <= 1) {
            ContainerClicker.quickMove(sourceIdx);
            return;
        }

        int destIdx = findInsertionSlot(menu, at, destSection);
        if (destIdx < 0) {
            // No room to insert single - fall back to full quick-move so at least something happens.
            ContainerClicker.quickMove(sourceIdx);
            return;
        }

        // Three clicks back-to-back: pickup whole, right-click drops 1, leftClick puts rest back.
        ContainerClicker.leftClick(sourceIdx);
        ContainerClicker.rightClick(destIdx);
        ContainerClicker.leftClick(sourceIdx);
    }

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

    private static List<Integer> findAllMatching(AbstractContainerMenu menu, ItemStack template, Container restrictTo) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (restrictTo != null && slot.container != restrictTo) continue;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                out.add(i);
            }
        }
        return out;
    }

    private static int findFirstMatching(AbstractContainerMenu menu, ItemStack template, Container restrictTo) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (restrictTo != null && slot.container != restrictTo) continue;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                return i;
            }
        }
        return -1;
    }

    /** Pick the first {@link Container} in {@code menu} that isn't {@code from}. */
    private static Container findOtherSection(AbstractContainerMenu menu, Container from) {
        for (Slot slot : menu.slots) {
            if (slot.container != from) return slot.container;
        }
        return null;
    }

    /** Of two containers, return the one whose topmost slot is higher on screen (smaller Y). */
    private static Container upperSection(AbstractContainerMenu menu, Container a, Container b) {
        return minSlotY(menu, a) <= minSlotY(menu, b) ? a : b;
    }

    private static int minSlotY(AbstractContainerMenu menu, Container container) {
        int min = Integer.MAX_VALUE;
        for (Slot slot : menu.slots) {
            if (slot.container == container && slot.y < min) min = slot.y;
        }
        return min;
    }
}
