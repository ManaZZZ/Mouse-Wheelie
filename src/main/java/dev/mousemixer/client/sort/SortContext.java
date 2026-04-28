package dev.mousemixer.client.sort;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Context passed to a {@link SortMode}. Mirrors the upstream Mouse Wheelie API. */
public record SortContext(AbstractContainerMenu menu, List<Slot> relevantSlots) {
}
