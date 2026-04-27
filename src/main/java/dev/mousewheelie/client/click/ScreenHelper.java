package dev.mousewheelie.client.click;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/** Stateless helpers for working with container screens. */
public final class ScreenHelper {
    private ScreenHelper() {}

    /** Find the slot under the given mouse coordinates, or null. */
    public static Slot findSlotUnder(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        AbstractContainerMenu menu = screen.getMenu();
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (localX >= slot.x && localX < slot.x + 16
                    && localY >= slot.y && localY < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Returns all slot indices belonging to the same {@link Container} as the given slot index.
     * For chest screens this gives the chest slots; for the player section this gives the player
     * inventory + hotbar.
     */
    public static List<Integer> slotsInSameSection(AbstractContainerMenu menu, int slotIndex) {
        Container target = menu.slots.get(slotIndex).container;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).container == target) out.add(i);
        }
        return out;
    }

    /** True if the given slot index belongs to the player inventory side of the menu. */
    public static boolean isPlayerSection(AbstractContainerMenu menu, int slotIndex) {
        return menu.slots.get(slotIndex).container instanceof Inventory;
    }

    /**
     * Return slots belonging to the OPPOSITE section of the given slot index. Used by
     * scroll-to-transfer to figure out where to move things.
     */
    public static List<Integer> slotsInOtherSection(AbstractContainerMenu menu, int slotIndex) {
        Container source = menu.slots.get(slotIndex).container;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).container != source) out.add(i);
        }
        return out;
    }
}
