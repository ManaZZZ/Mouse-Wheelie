package dev.mousemixer.client.sort;

import dev.mousemixer.client.click.ClickQueue;
import dev.mousemixer.client.click.ContainerClicker;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Port of the Fabric Mouse Wheelie sort
 * (<a href="https://github.com/Siphalor/mouse-wheelie/tree/cross-version/src/main/java/de/siphalor/mousewheelie/client/inventory/sort">upstream</a>).
 *
 * Two-phase plan, mutating an in-memory copy of the section:
 * <ol>
 *   <li>{@code combineStacks}: walk backward, pick up at {@code i}, distribute into earlier
 *       partial slots of the same item, then put any remainder back at {@code i}.</li>
 *   <li>{@link SortMode}: produces a permutation {@code sortedIds[i] = origin index} mapping
 *       each target position to the slot whose content belongs there.</li>
 *   <li>{@code reorderOnClient}: chain-follow with a BitSet — for each unfinished target,
 *       pick up at origin, then walk along the cycle ({@code id = origin2Target[id]}) placing
 *       the carried stack and picking up the displaced one. Empty target ends a chain.</li>
 * </ol>
 */
public final class InventorySorter {
    private InventorySorter() {}

    public static void sortSection(AbstractContainerMenu menu, List<Integer> slotIndices, SortMode mode) {
        if (mode == SortMode.NONE) return;
        if (!menu.getCarried().isEmpty()) return;

        // Drop restrictive slots (armor, crafting result, brewing fuel/result, etc.). If a chain
        // ends on one, vanilla silently rejects the placing click and the item is stranded in the
        // cursor — that's the "last item didn't get placed" symptom in the player inventory.
        ItemStack probe = new ItemStack(Items.STONE);
        List<Integer> sortable = new ArrayList<>(slotIndices.size());
        for (Integer idx : slotIndices) {
            if (menu.slots.get(idx).mayPlace(probe)) sortable.add(idx);
        }
        if (sortable.size() < 2) return;
        slotIndices = sortable;
        int n = slotIndices.size();

        Slot[] slots = new Slot[n];
        ItemStack[] stacks = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            slots[i] = menu.slots.get(slotIndices.get(i));
            stacks[i] = slots[i].getItem().copy();
        }

        List<Runnable> clicks = new ArrayList<>();
        combineStacks(clicks, slotIndices, slots, stacks);

        int[] sortedIds = new int[n];
        for (int i = 0; i < n; i++) sortedIds[i] = i;
        sortedIds = mode.sort(sortedIds, stacks, new SortContext(menu, Arrays.asList(slots)));

        reorderOnClient(clicks, slotIndices, slots, stacks, sortedIds);

        if (clicks.isEmpty()) return;
        ClickQueue.beginBatch();
        for (Runnable click : clicks) ClickQueue.enqueue(click);
    }

    private static void combineStacks(List<Runnable> clicks,
                                      List<Integer> slotIndices,
                                      Slot[] slots,
                                      ItemStack[] stacks) {
        for (int i = stacks.length - 1; i >= 0; i--) {
            ItemStack stack = stacks[i];
            if (stack.isEmpty()) continue;
            int capI = Math.min(stack.getMaxStackSize(), slots[i].getMaxStackSize(stack));
            int stackSize = stack.getCount();
            if (stackSize >= capI) continue;

            List<Runnable> tentative = new ArrayList<>();
            int realI = slotIndices.get(i);
            tentative.add(() -> ContainerClicker.leftClick(realI));

            for (int j = 0; j < i; j++) {
                ItemStack target = stacks[j];
                if (target.isEmpty()) continue;
                int capJ = Math.min(target.getMaxStackSize(), slots[j].getMaxStackSize(target));
                if (target.getCount() >= capJ) continue;
                if (!ItemStack.isSameItemSameComponents(stack, target)) continue;

                int delta = Math.min(capJ - target.getCount(), stackSize);
                stackSize -= delta;
                target.setCount(target.getCount() + delta);

                int realJ = slotIndices.get(j);
                tentative.add(() -> ContainerClicker.leftClick(realJ));
                if (stackSize <= 0) break;
            }

            if (tentative.size() <= 1) continue; // no merges happened — abandon the pickup
            clicks.addAll(tentative);

            if (stackSize > 0) {
                clicks.add(() -> ContainerClicker.leftClick(realI));
                stack.setCount(stackSize);
            } else {
                stacks[i] = ItemStack.EMPTY;
            }
        }
    }

    private static void reorderOnClient(List<Runnable> clicks,
                                        List<Integer> slotIndices,
                                        Slot[] slots,
                                        ItemStack[] stacks,
                                        int[] sortedIds) {
        final int n = stacks.length;

        // origin -> target: where each currently-occupied slot's content should end up.
        int[] origin2Target = new int[n];
        for (int i = 0; i < n; i++) origin2Target[sortedIds[i]] = i;

        // Combined bitset: [0..n) = "done", [n..2n) = "empty".
        BitSet doneOrEmpty = new BitSet(n * 2);
        for (int i = 0; i < n; i++) {
            if (i == sortedIds[i]) doneOrEmpty.set(i);
            if (stacks[i].isEmpty()) doneOrEmpty.set(n + i);
        }

        for (int i = 0; i < n; i++) {
            if (doneOrEmpty.get(i)) continue;
            int origin = sortedIds[i];
            if (doneOrEmpty.get(n + origin)) {
                // Origin is empty — nothing to carry; mark as done so we skip later.
                doneOrEmpty.set(origin);
                continue;
            }

            int originReal = slotIndices.get(origin);
            clicks.add(() -> ContainerClicker.leftClick(originReal));
            doneOrEmpty.set(n + origin); // origin is now empty (cursor holds its content)

            ItemStack currentStack = stacks[origin];
            final int workingSlotReal = originReal; // empty scratch slot for the duration of the chain

            int id = i;
            while (!doneOrEmpty.get(id)) {
                int targetReal = slotIndices.get(id);
                ItemStack atTarget = stacks[id];
                boolean targetEmpty = doneOrEmpty.get(n + id);

                if (!targetEmpty
                        && atTarget.getItem() == currentStack.getItem()
                        && ItemStack.isSameItemSameComponents(atTarget, currentStack)) {
                    if (atTarget.getCount() == currentStack.getCount()) {
                        // Identical — no click needed; continue along the chain.
                        doneOrEmpty.set(id);
                        id = origin2Target[id];
                        continue;
                    }
                    if (currentStack.getCount() < atTarget.getCount()) {
                        // Low onto high (e.g. bundle): direct PICKUP would just merge.
                        // Workaround: working, target, working, target, working.
                        final int ws = workingSlotReal;
                        final int ts = targetReal;
                        clicks.add(() -> ContainerClicker.leftClick(ws));
                        clicks.add(() -> ContainerClicker.leftClick(ts));
                        clicks.add(() -> ContainerClicker.leftClick(ws));
                        clicks.add(() -> ContainerClicker.leftClick(ts));
                        clicks.add(() -> ContainerClicker.leftClick(ws));

                        currentStack = atTarget;
                        doneOrEmpty.set(id);
                        id = origin2Target[id];
                        continue;
                    }
                    // currentStack > atTarget same item: fall through to left-click. Vanilla
                    // merges cursor into the slot up to max, leaving the remainder
                    // (= old target count) in the cursor. Effectively a swap-with-carry.
                }

                // Universal left-click: empty target -> place, different item -> vanilla swaps,
                // same-item high-onto-low -> merge-with-remainder (handled above).
                final int ts = targetReal;
                clicks.add(() -> ContainerClicker.leftClick(ts));
                currentStack = atTarget;
                doneOrEmpty.set(id);
                if (targetEmpty) break;
                id = origin2Target[id];
            }
        }
    }
}
