package dev.mousemixer.client.click;

import dev.mousemixer.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Paces simulated clicks across ticks so we don't spam the server in a single frame and so the
 * local menu state has time to settle. Bound to a specific menu when the operation begins; the
 * queue is flushed if the player closes that menu.
 */
public final class ClickQueue {
    private static final Deque<Runnable> QUEUE = new ArrayDeque<>();
    private static int boundContainerId = -1;
    private static int waitTicks;

    private ClickQueue() {}

    /** Begin a batch bound to the player's current menu. Clears any prior queue. */
    public static void beginBatch() {
        QUEUE.clear();
        Minecraft mc = Minecraft.getInstance();
        AbstractContainerMenu menu = mc.player == null ? null : mc.player.containerMenu;
        boundContainerId = menu == null ? -1 : menu.containerId;
        waitTicks = 0;
    }

    public static void enqueue(Runnable click) {
        QUEUE.addLast(click);
    }

    public static void clear() {
        QUEUE.clear();
        boundContainerId = -1;
        waitTicks = 0;
    }

    public static boolean isEmpty() {
        return QUEUE.isEmpty();
    }

    /** Drive the queue once per client tick. */
    public static void tick() {
        if (QUEUE.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            clear();
            return;
        }

        // If the menu changed (e.g. player closed the screen), abort.
        if (boundContainerId != -1 && mc.player.containerMenu.containerId != boundContainerId) {
            clear();
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        int delay = Math.max(0, Config.CLICK_DELAY_TICKS.get());
        if (delay == 0) {
            // Drain the queue this tick. Local menu state updates synchronously between
            // clicks (handleInventoryMouseClick), so the planned cursor sequence is preserved.
            while (!QUEUE.isEmpty()) {
                if (mc.player.containerMenu.containerId != boundContainerId) {
                    clear();
                    return;
                }
                Runnable next = QUEUE.pollFirst();
                if (next != null) next.run();
            }
            return;
        }

        Runnable next = QUEUE.pollFirst();
        if (next != null) next.run();
        waitTicks = delay;
    }
}
