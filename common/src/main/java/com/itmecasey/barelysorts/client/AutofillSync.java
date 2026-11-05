package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;

/**
 * Tells the server the player's autofill setting. Sent when the setting changes and again on every join, so the
 * server always has the saved value. Rides on the sort request with the autofill group name.
 */
public final class AutofillSync {
    private AutofillSync() {
    }

    /** Sends the saved "player" group setting to the server. Called on join. */
    public static void sendSaved() {
        send(ClientSettings.get(Sorter.GROUP_PLAYER).replaceAtOne());
    }

    public static void send(boolean replaceAtOne) {
        Platform.sendSort(0, Sorter.AUTOFILL, String.valueOf(replaceAtOne), "", "");
    }
}
