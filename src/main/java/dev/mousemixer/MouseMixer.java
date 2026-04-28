package dev.mousemixer;

import com.mojang.logging.LogUtils;
import dev.mousemixer.client.MouseMixerClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MouseMixer.MODID)
public class MouseMixer {
    public static final String MODID = "mousemixer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MouseMixer(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            MouseMixerClient.init(modContainer);
        }
    }
}
