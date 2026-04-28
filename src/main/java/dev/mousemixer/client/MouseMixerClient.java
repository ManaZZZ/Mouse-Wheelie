package dev.mousemixer.client;

import dev.mousemixer.MouseMixer;
import dev.mousemixer.client.click.ClickQueue;
import dev.mousemixer.client.craft.QuickCraftHandler;
import dev.mousemixer.client.craft.QuickCraftSession;
import dev.mousemixer.client.drag.ShiftDragHandler;
import dev.mousemixer.client.modifier.ModifierClickHandler;
import dev.mousemixer.client.refill.StackRefillHandler;
import dev.mousemixer.client.scroll.ScrollTransferHandler;
import dev.mousemixer.client.sort.SortHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class MouseMixerClient {
    private MouseMixerClient() {}

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

        MouseMixer.LOGGER.info("Mouse Mixer client handlers registered.");
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
