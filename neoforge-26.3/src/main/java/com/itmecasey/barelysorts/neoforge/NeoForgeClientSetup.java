package com.itmecasey.barelysorts.neoforge;

import com.itmecasey.barelysorts.Platform;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Client-only wiring. It lives in its own class so a dedicated server never loads the client types it names.
 */
final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {
    }

    static void install() {
        Platform.setKeyCodeReader(mapping -> mapping.getKey().getValue());
        Platform.setGuiOrigin(new Platform.GuiOrigin() {
            @Override
            public int left(AbstractContainerScreen<?> screen) {
                return com.itmecasey.barelysorts.compat.GuiPosition.left(screen);
            }

            @Override
            public int top(AbstractContainerScreen<?> screen) {
                return com.itmecasey.barelysorts.compat.GuiPosition.top(screen);
            }
        });
    }
}
