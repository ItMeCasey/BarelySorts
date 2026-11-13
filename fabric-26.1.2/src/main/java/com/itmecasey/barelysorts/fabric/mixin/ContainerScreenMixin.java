package com.itmecasey.barelysorts.fabric.mixin;

import com.itmecasey.barelysorts.client.ScreenActions;
import com.itmecasey.barelysorts.compat.DrawImpl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the Barely Sorts buttons and popups right after the screen's contents, the same spot as the Foreground event
 * on NeoForge and Forge and Fabric API's afterForeground event (which Fabric API for 26.1.2 does not have yet).
 * Fabric's after-extract event comes after the game has queued its tooltip, which would put our overlay above item
 * tooltips.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Inject(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
                    shift = At.Shift.AFTER))
    private void barelysorts$overlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ScreenActions.render((AbstractContainerScreen<?>) (Object) this, new DrawImpl(graphics), mouseX, mouseY);
    }
}
