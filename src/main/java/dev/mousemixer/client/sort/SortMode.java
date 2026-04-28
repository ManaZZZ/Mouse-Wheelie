package dev.mousemixer.client.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Sorting strategies ported from the Fabric Mouse Wheelie mod
 * (de.siphalor.mousewheelie.client.inventory.sort.SortMode). Each mode produces a permutation
 * {@code sortIds[i] = origin slot index whose contents should end up at logical position i}.
 */
public enum SortMode implements TranslatableEnum {
    /** Leave the order untouched. Useful as an explicit "off" value. */
    NONE {
        @Override
        public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
            return sortIds;
        }
    },
    /** Sort alphabetically by the displayed item name. Empty slots fall to the end. */
    ALPHABET {
        @Override
        public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
            String[] names = new String[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                names[i] = stacks[i].isEmpty() ? "" : stacks[i].getHoverName().getString();
            }
            sortBy(sortIds, (a, b) -> {
                if (names[a].isEmpty()) return names[b].isEmpty() ? 0 : 1;
                if (names[b].isEmpty()) return -1;
                int c = names[a].compareToIgnoreCase(names[b]);
                return c != 0 ? c : compareEqualItems(stacks[a], stacks[b]);
            });
            return sortIds;
        }
    },
    /**
     * Group by item type, ordered by the total count across the section (largest groups first).
     * Within a group, full stacks come before partials.
     */
    QUANTITY {
        @Override
        public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
            Map<Item, Integer> totals = new HashMap<>();
            for (ItemStack s : stacks) {
                if (s.isEmpty()) continue;
                totals.merge(s.getItem(), s.getCount(), Integer::sum);
            }
            sortBy(sortIds, (a, b) -> {
                ItemStack sa = stacks[a];
                ItemStack sb = stacks[b];
                if (sa.isEmpty()) return sb.isEmpty() ? 0 : 1;
                if (sb.isEmpty()) return -1;
                int c = Integer.compare(totals.get(sb.getItem()), totals.get(sa.getItem()));
                return c != 0 ? c : compareEqualItems(sa, sb);
            });
            return sortIds;
        }
    },
    /** Sort by registry id of the item. Stable across sessions; fastest mode. */
    RAW_ID {
        @Override
        public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
            int[] ids = new int[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                ids[i] = stacks[i].isEmpty()
                        ? Integer.MAX_VALUE
                        : BuiltInRegistries.ITEM.getId(stacks[i].getItem());
            }
            sortBy(sortIds, (a, b) -> {
                int c = Integer.compare(ids[a], ids[b]);
                return c != 0 ? c : compareEqualItems(stacks[a], stacks[b]);
            });
            return sortIds;
        }
    };

    public abstract int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context);

    @Override
    public Component getTranslatedName() {
        return Component.translatable("mousemixer.configuration.sortMode." + name());
    }

    @FunctionalInterface
    private interface IntCmp {
        int compare(int a, int b);
    }

    private static void sortBy(int[] sortIds, IntCmp cmp) {
        Integer[] boxed = Arrays.stream(sortIds).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, (a, b) -> cmp.compare(a, b));
        for (int i = 0; i < boxed.length; i++) sortIds[i] = boxed[i];
    }

    /** Tie-breaker for stacks the primary sort treats as equal. Item id, then count desc. */
    private static int compareEqualItems(ItemStack a, ItemStack b) {
        int c = Integer.compare(
                BuiltInRegistries.ITEM.getId(a.getItem()),
                BuiltInRegistries.ITEM.getId(b.getItem()));
        if (c != 0) return c;
        return Integer.compare(b.getCount(), a.getCount());
    }
}
