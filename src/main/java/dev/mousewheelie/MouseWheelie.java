package dev.mousewheelie;

import com.mojang.logging.LogUtils;
import dev.mousewheelie.client.MouseWheelieClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MouseWheelie.MODID)
public class MouseWheelie {
    public static final String MODID = "mousewheelie";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MouseWheelie(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            MouseWheelieClient.init(modContainer);
        }
    }
}
