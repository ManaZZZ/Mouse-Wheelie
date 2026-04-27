package dev.mousewheelie.client;

import dev.mousewheelie.MouseWheelie;
import dev.mousewheelie.client.click.ClickQueue;
import dev.mousewheelie.client.craft.QuickCraftHandler;
import dev.mousewheelie.client.craft.QuickCraftSession;
import dev.mousewheelie.client.drag.ShiftDragHandler;
import dev.mousewheelie.client.modifier.ModifierClickHandler;
import dev.mousewheelie.client.refill.StackRefillHandler;
import dev.mousewheelie.client.scroll.ScrollTransferHandler;
import dev.mousewheelie.client.sort.SortHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class MouseWheelieClient {
    private MouseWheelieClient() {}

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        var bus = NeoForge.EVENT_BUS;
        bus.register(SortHandler.class);
        bus.register(ModifierClickHandler.class);
        bus.register(ScrollTransferHandler.class);
        bus.register(ShiftDragHandler.class);
        bus.register(StackRefillHandler.class);
        bus.register(QuickCraftHandler.class);
        bus.register(ClickQueueDriver.class);

        MouseWheelie.LOGGER.info("Mouse Wheelie NeoForge client handlers registered.");
    }

    /** Drives tick-based subsystems (click queue, quick-craft session) once per client tick. */
    public static final class ClickQueueDriver {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            ClickQueue.tick();
            QuickCraftSession.tick();
        }
    }
}
