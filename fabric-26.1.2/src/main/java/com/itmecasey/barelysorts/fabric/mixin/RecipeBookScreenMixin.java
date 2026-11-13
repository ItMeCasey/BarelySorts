package com.itmecasey.barelysorts.fabric.mixin;

import com.itmecasey.barelysorts.client.ScreenActions;
import com.itmecasey.barelysorts.compat.DrawImpl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The same hook for screens with a recipe book (the player inventory, crafting table, furnaces), which override the
 * render method instead of calling the base class. NeoForge fires its Foreground event at this same spot.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenMixin {
    @Inject(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
                    shift = At.Shift.AFTER))
    private void barelysorts$overlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ScreenActions.render((AbstractContainerScreen<?>) (Object) this, new DrawImpl(graphics), mouseX, mouseY);
    }
}
