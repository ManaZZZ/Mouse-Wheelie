package dev.mousewheelie.client.sort;

import dev.mousewheelie.Config;
import dev.mousewheelie.client.click.ScreenHelper;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

public final class SortHandler {
    private SortHandler() {}

    @SubscribeEvent
    public static void onMouseButton(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!Config.ENABLE_SORT.get()) return;
        if (event.getButton() != 2) return; // middle mouse only
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof CreativeModeInventoryScreen) return;

        Slot hovered = ScreenHelper.findSlotUnder(screen, event.getMouseX(), event.getMouseY());
        if (hovered == null) return;

        AbstractContainerMenu menu = screen.getMenu();
        int slotIndex = menu.slots.indexOf(hovered);
        if (slotIndex < 0) return;

        List<Integer> section = ScreenHelper.slotsInSameSection(menu, slotIndex);
        // Exclude hotbar (Inventory slots 0-8) so it is never rearranged.
        section.removeIf(idx -> {
            Slot s = menu.slots.get(idx);
            return s.container instanceof Inventory && Inventory.isHotbarSlot(s.getContainerSlot());
        });
        if (section.size() < 2) return;

        InventorySorter.sortSection(menu, section, Config.SORT_MODE.get());
        event.setCanceled(true);
    }
}
