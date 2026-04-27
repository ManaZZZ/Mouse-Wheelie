package dev.mousewheelie.client.craft;

import dev.mousewheelie.Config;
import dev.mousewheelie.client.click.ContainerClicker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Drives a single quick-craft operation as a tick-based state machine. Only one session is active
 * at a time. Each batch: send a vanilla PlaceRecipe packet, wait for the result slot to populate,
 * pick up the result per {@link Config#QUICK_CRAFT_PICKUP}, then either stop or loop.
 */
public final class QuickCraftSession {
    private static final int RESULT_TIMEOUT_TICKS = 20;
    private static final int SETTLE_TICKS = 1;

    private static QuickCraftSession active;

    private final RecipeHolder<?> recipe;
    private final boolean placeShift;
    private final int resultSlot;
    private final int containerId;
    private final boolean unlimited;

    private Phase phase = Phase.PLACE;
    private int waitTicks;

    private enum Phase { PLACE, WAIT_RESULT, PICKUP_CURSOR, WAIT_SETTLE }

    private QuickCraftSession(RecipeHolder<?> recipe, boolean placeShift,
                              int resultSlot, int containerId, boolean unlimited) {
        this.recipe = recipe;
        this.placeShift = placeShift;
        this.resultSlot = resultSlot;
        this.containerId = containerId;
        this.unlimited = unlimited;
    }

    public static void start(RecipeHolder<?> recipe, boolean placeShift,
                             int resultSlot, int containerId, boolean unlimited) {
        active = new QuickCraftSession(recipe, placeShift, resultSlot, containerId, unlimited);
    }

    public static boolean isActive() {
        return active != null;
    }

    public static void abort() {
        active = null;
    }

    public static void tick() {
        if (active != null && active.tickInternal()) {
            active = null;
        }
    }

    /** Returns true when the session should terminate. */
    private boolean tickInternal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null || menu.containerId != containerId) return true;
        if (resultSlot >= menu.slots.size()) return true;
        Slot slot = menu.slots.get(resultSlot);

        switch (phase) {
            case PLACE -> {
                mc.gameMode.handlePlaceRecipe(containerId, recipe, placeShift);
                phase = Phase.WAIT_RESULT;
                waitTicks = 0;
                return false;
            }
            case WAIT_RESULT -> {
                if (slot.hasItem()) {
                    if (Config.QUICK_CRAFT_PICKUP.get() == QuickCraftPickup.TO_INVENTORY) {
                        ContainerClicker.quickMove(resultSlot);
                        phase = Phase.WAIT_SETTLE;
                    } else {
                        ContainerClicker.leftClick(resultSlot);
                        phase = Phase.PICKUP_CURSOR;
                    }
                    waitTicks = 0;
                    return false;
                }
                if (++waitTicks > RESULT_TIMEOUT_TICKS) return true;
                return false;
            }
            case PICKUP_CURSOR -> {
                // Drain the grid by repeatedly left-clicking; each click takes one result onto
                // the cursor and re-computes the result slot from remaining ingredients.
                if (slot.hasItem()) {
                    ItemStack cursor = menu.getCarried();
                    if (!cursor.isEmpty() && cursor.getCount() >= cursor.getMaxStackSize()) {
                        return handleOverflow(slot);
                    }
                    ContainerClicker.leftClick(resultSlot);
                    return false;
                }
                return finishBatch();
            }
            case WAIT_SETTLE -> {
                if (++waitTicks < SETTLE_TICKS) return false;
                if (slot.hasItem()) return handleOverflow(slot);
                return finishBatch();
            }
        }
        return true;
    }

    private boolean finishBatch() {
        if (!unlimited) return true;
        phase = Phase.PLACE;
        waitTicks = 0;
        return false;
    }

    private boolean handleOverflow(Slot slot) {
        // Cursor is full while collecting — redirect this result to inventory and keep batching.
        // If inventory is also full we'll re-enter overflow from WAIT_SETTLE and apply STOP/DROP.
        if (phase == Phase.PICKUP_CURSOR && unlimited) {
            ContainerClicker.quickMove(resultSlot);
            phase = Phase.WAIT_SETTLE;
            waitTicks = 0;
            return false;
        }
        if (Config.QUICK_CRAFT_OVERFLOW.get() == QuickCraftOverflow.STOP) return true;
        ContainerClicker.dropStack(resultSlot);
        return finishBatch();
    }
}
