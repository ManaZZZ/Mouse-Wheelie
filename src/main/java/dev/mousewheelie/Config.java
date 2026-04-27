package dev.mousewheelie;

import dev.mousewheelie.client.craft.QuickCraftOverflow;
import dev.mousewheelie.client.craft.QuickCraftPickup;
import dev.mousewheelie.client.sort.SortMode;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_SORT = BUILDER
            .comment("Enable middle-click to sort the container under the cursor.")
            .define("enableSort", true);

    public static final ModConfigSpec.EnumValue<SortMode> SORT_MODE = BUILDER
            .comment("Algorithm used when sorting. RAW_ID groups by item registry id, ALPHABET by display name, QUANTITY by total count of each item type, NONE disables sorting.")
            .defineEnum("sortMode", SortMode.QUANTITY);

    public static final ModConfigSpec.BooleanValue ENABLE_REFILL = BUILDER
            .comment("Enable automatic refill of the main hand and off-hand from the inventory when a stack runs out.")
            .define("enableRefill", true);

    public static final ModConfigSpec.BooleanValue ENABLE_SCROLL_TRANSFER = BUILDER
            .comment("Enable scrolling on a slot to move the stack to the other section.")
            .define("enableScrollTransfer", true);

    public static final ModConfigSpec.BooleanValue SCROLL_DIRECTIONAL = BUILDER
            .comment("When true, scroll up always moves items toward the upper container (e.g. chest) and scroll down toward the lower container (e.g. player inventory), regardless of which section is hovered. When false (default), scroll up sends items from the hovered section to the other side and scroll down pulls items into the hovered section.")
            .define("scrollDirectional", false);

    public static final ModConfigSpec.BooleanValue ENABLE_MODIFIER_CLICKS = BUILDER
            .comment("Enable Shift / Ctrl / Alt click modifiers in container screens.")
            .define("enableModifierClicks", true);

    public static final ModConfigSpec.BooleanValue ENABLE_SHIFT_DRAG = BUILDER
            .comment("Enable holding Shift + left mouse and dragging across slots to quick-move each one to the other section.")
            .define("enableShiftDrag", true);

    public static final ModConfigSpec.BooleanValue ENABLE_QUICK_CRAFT = BUILDER
            .comment("Enable quick-craft on the recipe book: RMB places one craft, Shift+RMB up to a full stack, Ctrl+Shift+RMB as much as possible.")
            .define("enableQuickCraft", true);

    public static final ModConfigSpec.EnumValue<QuickCraftPickup> QUICK_CRAFT_PICKUP = BUILDER
            .comment("Where crafted items go. TO_INVENTORY shift-clicks the result into the inventory. TO_CURSOR left-clicks each result onto the cursor (limited by stack size).")
            .defineEnum("quickCraftPickup", QuickCraftPickup.TO_INVENTORY);

    public static final ModConfigSpec.EnumValue<QuickCraftOverflow> QUICK_CRAFT_OVERFLOW = BUILDER
            .comment("Behavior when crafted results cannot be picked up (inventory or cursor full). STOP halts the operation; DROP throws the leftover stack on the ground and continues.")
            .defineEnum("quickCraftOverflow", QuickCraftOverflow.STOP);

    public static final ModConfigSpec.IntValue CLICK_DELAY_TICKS = BUILDER
            .comment("Delay (in ticks) between consecutive simulated container clicks (e.g. while sorting). 0 = no delay.")
            .defineInRange("clickDelayTicks", 0, 0, 20);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
