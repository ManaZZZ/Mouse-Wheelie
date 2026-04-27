package dev.mousewheelie.client.craft;

import dev.mousewheelie.Config;
import dev.mousewheelie.MouseWheelie;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Intercepts right-clicks on recipe book entries inside crafting screens (player 2x2 + crafting
 * table 3x3) and starts a {@link QuickCraftSession}:
 * <ul>
 *   <li>RMB: craft one batch</li>
 *   <li>Shift+RMB: craft up to a full stack of the result</li>
 *   <li>Ctrl+Shift+RMB: keep crafting until ingredients run out</li>
 * </ul>
 * Vanilla's variant-overlay-on-RMB is bypassed; recipe variants still cycle automatically in the
 * button preview.
 */
public final class QuickCraftHandler {
    private static Field recipeBookPageField;
    private static Field buttonsField;

    private QuickCraftHandler() {}

    @SubscribeEvent
    public static void onMouseButton(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!Config.ENABLE_QUICK_CRAFT.get()) return;
        if (event.getButton() != 1) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!(screen instanceof RecipeUpdateListener listener)) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (!(menu instanceof CraftingMenu) && !(menu instanceof InventoryMenu)) return;
        if (!(menu instanceof RecipeBookMenu<?, ?> recipeMenu)) return;

        RecipeBookComponent component = listener.getRecipeBookComponent();
        if (component == null || !component.isVisible()) return;

        RecipeButton hit = findHitButton(component, event.getMouseX(), event.getMouseY());
        if (hit == null) return;

        RecipeHolder<?> recipe = hit.getRecipe();
        if (recipe == null) return;

        boolean shift = Screen.hasShiftDown();
        boolean ctrl = Screen.hasControlDown();
        boolean placeShift = shift;
        boolean unlimited = ctrl && shift;

        QuickCraftSession.start(recipe, placeShift,
                recipeMenu.getResultSlotIndex(), menu.containerId, unlimited);
        event.setCanceled(true);
    }

    private static RecipeButton findHitButton(RecipeBookComponent component, double mouseX, double mouseY) {
        try {
            if (recipeBookPageField == null) {
                recipeBookPageField = RecipeBookComponent.class.getDeclaredField("recipeBookPage");
                recipeBookPageField.setAccessible(true);
            }
            RecipeBookPage page = (RecipeBookPage) recipeBookPageField.get(component);
            if (page == null) return null;

            if (buttonsField == null) {
                buttonsField = RecipeBookPage.class.getDeclaredField("buttons");
                buttonsField.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            List<RecipeButton> buttons = (List<RecipeButton>) buttonsField.get(page);

            for (RecipeButton b : buttons) {
                if (b.visible && b.isMouseOver(mouseX, mouseY)) return b;
            }
        } catch (ReflectiveOperationException e) {
            MouseWheelie.LOGGER.error("Quick-craft: failed to access recipe book internals", e);
        }
        return null;
    }
}
