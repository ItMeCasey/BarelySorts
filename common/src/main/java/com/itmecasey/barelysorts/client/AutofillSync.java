package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;

/**
 * Tells the server the player's autofill settings: the swap-at-1-durability choice and the locked player slots. Sent
 * when either changes and again on every join, so the server always has the saved values. Rides on the sort request
 * with the autofill group name.
 */
public final class AutofillSync {
    private AutofillSync() {
    }

    /** Sends the saved "player" group settings to the server. Called on join. */
    public static void sendSaved() {
        send(ClientSettings.get(Sorter.GROUP_PLAYER));
    }

    public static void send(GroupSettings playerSettings) {
        String locked = String.join(",", playerSettings.lockedSlots().stream().map(String::valueOf).toList());
        Platform.sendSort(0, Sorter.AUTOFILL, String.valueOf(playerSettings.replaceAtOne()), "", locked);
    }
}
