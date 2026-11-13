package com.itmecasey.barelysorts.devtest;

import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.client.Group;
import com.itmecasey.barelysorts.client.KeyBindings;
import com.itmecasey.barelysorts.client.ScreenActions;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Development-only client tour, compiled in only with {@code -Ptour} and run only when the environment variable
 * {@code BARELYSORTS_TOUR} names the layer. It loads the world given with {@code --quickPlaySingleplayer}, then drives
 * the mod through the game's own mouse handler (so each loader's event hooks run), saves screenshots into
 * {@code run/screenshots} and quits. It never touches the real cursor or keyboard. Progress goes to the log as
 * {@code BS-TOUR} lines. Version-specific calls are found by reflection so one file serves every Minecraft version.
 */
public final class Tour {
    private static final String LAYER = System.getenv("BARELYSORTS_TOUR");
    /** The game's own value: 26.3 renumbered the mouse buttons. */
    private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
    private static volatile boolean forceMark;
    private static volatile boolean forceShift;
    private static Minecraft mc;

    private Tour() {
    }

    public static void start() {
        Thread thread = new Thread(Tour::run, "barelysorts-tour");
        thread.setDaemon(true);
        thread.start();
    }

    private static void log(String text) {
        System.out.println("BS-TOUR " + text);
    }

    private static void run() {
        try {
            while ((mc = Minecraft.getInstance()) == null) {
                Thread.sleep(200);
            }
            installKeyOverride();
            Thread.sleep(2000);
            openWorld();
            while (!(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && currentScreen() == null)) {
                Thread.sleep(500);
            }
            log("in the world");
            Thread.sleep(4000);
            tour();
            log("DONE");
        } catch (Throwable t) {
            log("FAILED " + t);
            t.printStackTrace();
        } finally {
            if (mc != null) {
                mc.execute(() -> mc.stop());
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
                // Going down anyway.
            }
            Runtime.getRuntime().halt(0);
        }
    }

    /** Waits for the title screen, then creates the test world. */
    private static void openWorld() throws Exception {
        while (mc.level != null || currentScreen() == null) {
            Thread.sleep(500);
        }
        Thread.sleep(6000);
        log("creating a fresh flat world");
        onClient(() -> FreshWorld.create("barelysorts_tour"));
    }

    // ---- The tour ----

    private static void tour() throws Exception {
        MinecraftServer server = mc.getSingleplayerServer();
        int[] pos = new int[3];
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            BlockPos b = p.blockPosition();
            pos[0] = b.getX() + 2;
            pos[1] = b.getY();
            pos[2] = b.getZ();
        });
        String at = pos[0] + " " + pos[1] + " " + pos[2];
        if ("extras".equals(System.getenv("BS_TOUR_PART"))) {
            extras(server, pos);
            return;
        }
        command("setblock " + at + " minecraft:chest");
        String[][] chest = {{"0", "cobblestone 10"}, {"1", "diamond 3"}, {"2", "cobblestone 20"}, {"3", "apple 5"},
                {"4", "iron_ingot 7"}, {"5", "oak_log 5"}, {"6", "iron_ingot 30"}, {"7", "stone 64"}, {"8", "coal 12"}};
        for (String[] s : chest) {
            command("item replace block " + at + " container." + s[0] + " with minecraft:" + s[1]);
        }
        String[][] pack = {{"9", "cobblestone 32"}, {"10", "bread 4"}, {"12", "iron_ingot 9"}, {"14", "diamond 2"},
                {"16", "oak_log 20"}, {"18", "apple 3"}, {"20", "coal 40"}, {"25", "stone 17"}, {"30", "diamond 1"}};
        for (String[] s : pack) {
            command("item replace entity @p container." + s[0] + " with minecraft:" + s[1]);
        }
        command("item replace entity @p container.0 with minecraft:cobblestone 1");
        Thread.sleep(1500);

        // Player inventory: before, sort with the sort button, settings popup, lock selection.
        log("opening the inventory");
        onClient(() -> setScreen(new InventoryScreen(mc.player)));
        Thread.sleep(2500);
        shot("inventory_before");
        dump(server, "inventory before");
        click(group("player"), true);
        Thread.sleep(1500);
        shot("inventory_sorted");
        dump(server, "inventory after sort button");

        clickGui(group("player").menuButtonX() + 4, group("player").menuButtonY() + 4, LEFT);
        Thread.sleep(800);
        shot("inventory_popup");
        Group player = group("player");
        int rowLeft = call("popupLeft", player) + 6;
        int rowTop = call("popupTop", player);
        int rowHeight = field("ROW_HEIGHT");
        clickGui(rowLeft, rowTop + 2 * rowHeight + rowHeight / 2, LEFT);
        Thread.sleep(800);
        AbstractContainerScreen<?> screen = screen();
        var slots = group("player").slots();
        clickGui(group("player").left() + slots.get(0).x - slots.get(0).x + 8, group("player").top() + 8, LEFT);
        clickGui(group("player").left() + 8 + 18, group("player").top() + 8, LEFT);
        Thread.sleep(800);
        shot("inventory_locking");
        clickGui(group("player").menuButtonX() + 4, group("player").menuButtonY() + 4, LEFT);
        Thread.sleep(800);
        shot("inventory_locked");
        clickGui(group("player").menuButtonX() + 4, group("player").menuButtonY() + 4, LEFT);
        Thread.sleep(500);
        // Slots 9 and 10 are locked: put odd items in them and sort; they must stay put while the rest sorts.
        command("item replace entity @p container.9 with minecraft:dirt 1");
        command("item replace entity @p container.10 with minecraft:dirt 2");
        command("item replace entity @p container.20 with minecraft:sand 5");
        Thread.sleep(500);
        click(group("player"), true);
        Thread.sleep(1500);
        dump(server, "inventory after sort with slots 9 and 10 locked (9=dirtx1 and 10=dirtx2 must be unchanged)");
        // Middle-click sort.
        clickGui(group("player").left() + 40, group("player").top() + 20, InputConstants.MOUSE_BUTTON_MIDDLE);
        Thread.sleep(1200);
        shot("inventory_middle_click");
        onClient(() -> setScreen(null));
        Thread.sleep(500);

        // Chest.
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            var be = ((ServerLevel) p.level()).getBlockEntity(new BlockPos(pos[0], pos[1], pos[2]));
            p.openMenu((MenuProvider) be);
        });
        Thread.sleep(2500);
        shot("chest_before");
        click(group("container"), true);
        Thread.sleep(1500);
        shot("chest_sorted");
        dumpChest(server, pos);

        clickGui(group("container").menuButtonX() + 4, group("container").menuButtonY() + 4, LEFT);
        Thread.sleep(800);
        shot("chest_popup");
        clickGui(group("container").menuButtonX() + 4, group("container").menuButtonY() + 4, LEFT);
        Thread.sleep(500);

        // Mass delete: hold X, click two chest slots, release X, read the dialog, confirm.
        forceMark = true;
        Thread.sleep(700);
        var cslots = group("container").slots();
        clickGui(group("container").left() + 8, group("container").top() + 8, LEFT);
        clickGui(group("container").left() + 8 + 18, group("container").top() + 8, LEFT);
        Thread.sleep(700);
        shot("chest_marked");
        forceMark = false;
        Thread.sleep(900);
        shot("chest_delete_dialog");
        screen = screen();
        int left = call("confirmLeft", screen);
        int buttonY = call("confirmButtonY", screen);
        clickGui(left + 12 + 28, buttonY + 7, LEFT);
        Thread.sleep(1500);
        shot("chest_after_delete");
        dumpChest(server, pos);
        onClient(() -> setScreen(null));
        Thread.sleep(500);

        // Autofill. First with the only cobblestone in locked slots 9 and 10: slot 0 must stay empty.
        command("item replace entity @p container.9 with minecraft:cobblestone 32");
        command("item replace entity @p container.10 with minecraft:cobblestone 32");
        command("item replace entity @p container.0 with minecraft:cobblestone 1");
        Thread.sleep(800);
        onServer(server, () -> server.getPlayerList().getPlayers().get(0).getInventory().setItem(0, ItemStack.EMPTY));
        Thread.sleep(1200);
        dump(server, "autofill with cobblestone only in locked slots (slot 0 must stay empty)");
        // Then with cobblestone in an unlocked slot: slot 0 must refill.
        command("item replace entity @p container.0 with minecraft:cobblestone 1");
        command("item replace entity @p container.22 with minecraft:cobblestone 16");
        Thread.sleep(800);
        onServer(server, () -> server.getPlayerList().getPlayers().get(0).getInventory().setItem(0, ItemStack.EMPTY));
        Thread.sleep(1200);
        shot("autofill_hotbar");
        dump(server, "autofill with cobblestone in unlocked slot 22 (slot 0 must hold cobblestone)");
    }

    /** Tooltip draw order and Shift-click on crafting results. Run alone with BS_TOUR_PART=extras. */
    private static void extras(MinecraftServer server, int[] pos) throws Exception {
        String[][] pack = {{"9", "cobblestone 32"}, {"10", "bread 4"}, {"12", "iron_ingot 9"}, {"14", "diamond 2"},
                {"16", "oak_log 20"}, {"18", "apple 3"}, {"20", "coal 40"}, {"25", "stone 17"}, {"30", "diamond 1"}};
        for (String[] s : pack) {
            command("item replace entity @p container." + s[0] + " with minecraft:" + s[1]);
        }
        for (int i = 9; i < 36; i++) {
            if (i != 9 && i != 10) {
                command("item replace entity @p container." + i + " with minecraft:dirt 5");
            }
        }
        Thread.sleep(800);

        // 1. An item tooltip that overlaps the open settings popup must be drawn on top of it.
        onClient(() -> setScreen(new InventoryScreen(mc.player)));
        Thread.sleep(2000);
        Group player = group("player");
        clickGui(player.menuButtonX() + 4, player.menuButtonY() + 4, LEFT);
        Thread.sleep(800);
        player = group("player");
        int[] box = new int[4];
        Group popupGroup = player;
        // Text measuring touches the font textures, so it has to run on the render thread.
        onClient(() -> {
            box[0] = call("popupLeft", popupGroup);
            box[1] = call("popupTop", popupGroup);
            box[2] = call("popupWidth", popupGroup);
            box[3] = call("popupHeight", popupGroup);
        });
        int pl = box[0];
        int pt = box[1];
        int pw = box[2];
        int ph = box[3];
        net.minecraft.world.inventory.Slot target = null;
        double best = Double.MAX_VALUE;
        for (net.minecraft.world.inventory.Slot slot : screen().getMenu().slots) {
            if (!slot.hasItem()) {
                continue;
            }
            int sx = Platform.guiOrigin().left(screen()) + slot.x + 8;
            int sy = Platform.guiOrigin().top(screen()) + slot.y + 8;
            double d = Math.hypot(sx - (pl + pw / 2.0), sy - (pt + ph / 2.0));
            if (d < best) {
                best = d;
                target = slot;
            }
        }
        log("popup " + pl + "," + pt + " " + pw + "x" + ph + " tooltip slot " + (target == null ? "NONE" : target.index));
        if (target != null) {
            int mx = Platform.guiOrigin().left(screen()) + target.x + 8;
            int my = Platform.guiOrigin().top(screen()) + target.y + 8;
            onClient(() -> invokeMouse("onMove", Compat.windowHandle(),
                    mx * (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(),
                    my * (double) mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight()));
            Thread.sleep(1200);
            try {
                var hf = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
                hf.setAccessible(true);
                var hs = (net.minecraft.world.inventory.Slot) hf.get(screen());
                log("hovered slot " + (hs == null ? "null" : hs.index) + " mouse " + mx + "," + my);
            } catch (ReflectiveOperationException e) {
                log("hovered slot unknown " + e);
            }
            shot("tooltip_over_popup");
        }
        onClient(() -> setScreen(null));
        Thread.sleep(500);

        // 1b. A chest screen (no recipe book): the sort buttons must still be drawn.
        command("setblock " + pos[0] + " " + pos[1] + " " + pos[2] + " minecraft:chest");
        command("item replace block " + pos[0] + " " + pos[1] + " " + pos[2] + " container.13 with minecraft:apple 3");
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            p.openMenu((MenuProvider) ((ServerLevel) p.level()).getBlockEntity(new BlockPos(pos[0], pos[1], pos[2])));
        });
        Thread.sleep(2000);
        shot("chest_buttons");
        onClient(() -> setScreen(null));
        Thread.sleep(500);

        // 2. Shift-click on the 2x2 result slot of the inventory.
        forceShift = true;
        craft(server, "inventory 2x2", () -> setScreen(new InventoryScreen(mc.player)), null);

        // 3. The same on a vanilla crafting table.
        String table = pos[0] + " " + pos[1] + " " + (pos[2] + 3);
        command("setblock " + table + " minecraft:crafting_table");
        craft(server, "crafting table 3x3", null, new BlockPos(pos[0], pos[1], pos[2] + 3));
        forceShift = false;
    }

    private static void craft(MinecraftServer server, String label, Runnable openClient, BlockPos tablePos)
            throws Exception {
        command("clear @p");
        Thread.sleep(300);
        int inputs = tablePos == null ? 4 : 9;
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            if (tablePos != null) {
                var level = (ServerLevel) p.level();
                p.openMenu(level.getBlockState(tablePos).getMenuProvider(level, tablePos));
            }
        });
        if (openClient != null) {
            onClient(openClient);
        }
        Thread.sleep(2000);
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            var menu = p.containerMenu;
            for (int i = 1; i <= inputs; i++) {
                if (tablePos != null && i == 5) {
                    continue;
                }
                menu.getSlot(i).set(new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 16));
            }
            menu.slotsChanged(menu.getSlot(1).container);
            menu.broadcastChanges();
        });
        Thread.sleep(1000);
        shot("craft_before_" + (tablePos == null ? "2x2" : "3x3"));
        net.minecraft.world.inventory.Slot result = screen().getMenu().getSlot(0);
        int cx = Platform.guiOrigin().left(screen()) + result.x + 8;
        int cy = Platform.guiOrigin().top(screen()) + result.y + 8;
        log("craft " + label + ": result slot shows " + result.getItem());
        // Press Shift and left-click the result through the mod's own handler. true means the mod ate the click.
        var window = mc.getWindow();
        double mx = cx * (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double my = cy * (double) window.getScreenHeight() / window.getGuiScaledHeight();
        boolean[] ate = new boolean[1];
        onClient(() -> {
            invokeMouse("onMove", Compat.windowHandle(), mx, my);
            ate[0] = ScreenActions.mouseButton(screen(), cx, cy, LEFT);
        });
        log("craft " + label + ": mod handler consumed the shift-click = " + ate[0]);
        Thread.sleep(500);
        if (!ate[0]) {
            // The mod passed the click on, so the game handles it: this is the quick move the game then sends.
            onClient(Tour::quickMoveResult);
        }
        Thread.sleep(1200);
        dump(server, label + " after shift-click on the result");
        shot("craft_after_" + (tablePos == null ? "2x2" : "3x3"));
        onClient(() -> setScreen(null));
        Thread.sleep(500);
    }

    /** The game's own container click (QUICK_MOVE on slot 0); the method and its enum are renamed in 26.x. */
    private static void quickMoveResult() {
        try {
            for (Method m : mc.gameMode.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 5 && p[0] == int.class && p[1] == int.class && p[2] == int.class && p[3].isEnum()
                        && (m.getName().equals("handleInventoryMouseClick") || m.getName().equals("handleContainerInput"))) {
                    for (Object c : p[3].getEnumConstants()) {
                        if (c.toString().equals("QUICK_MOVE")) {
                            m.invoke(mc.gameMode, screen().getMenu().containerId, 0, 0, c, mc.player);
                            return;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("no container click method");
    }

    // ---- Helpers ----

    /** The open screen: a field on the client in older versions, behind the gui object in newer ones. */
    private static Object currentScreen() {
        try {
            try {
                return Minecraft.class.getField("screen").get(mc);
            } catch (NoSuchFieldException e) {
                Object gui = Minecraft.class.getField("gui").get(mc);
                return gui == null ? null : gui.getClass().getMethod("screen").invoke(gui);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setScreen(net.minecraft.client.gui.screens.Screen screen) {
        try {
            try {
                Minecraft.class.getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(mc, screen);
            } catch (NoSuchMethodException e) {
                Object gui = Minecraft.class.getField("gui").get(mc);
                gui.getClass().getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(gui, screen);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AbstractContainerScreen<?> screen() {
        if (currentScreen() instanceof AbstractContainerScreen<?> s) {
            return s;
        }
        throw new IllegalStateException("no container screen: " + currentScreen());
    }

    private static Group group(String name) {
        for (Group g : Group.of(screen())) {
            if (g.name().equals(name) || g.settingsKey().equals(name)) {
                return g;
            }
        }
        List<String> names = new ArrayList<>();
        Group.of(screen()).forEach(g -> names.add(g.name() + "/" + g.settingsKey()));
        throw new IllegalStateException("no group " + name + " among " + names);
    }

    private static void click(Group g, boolean sortButton) throws Exception {
        int x = sortButton ? g.sortButtonX() + 4 : g.menuButtonX() + 4;
        int y = sortButton ? g.sortButtonY() + 4 : g.menuButtonY() + 4;
        clickGui(x, y, LEFT);
    }

    /** A press and release at a position in GUI coordinates, through the game's own mouse handler. */
    private static void clickGui(int guiX, int guiY, int button) throws Exception {
        var window = mc.getWindow();
        double rawX = guiX * (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double rawY = guiY * (double) window.getScreenHeight() / window.getGuiScaledHeight();
        long handle = Compat.windowHandle();
        onClient(() -> {
            invokeMouse("onMove", handle, rawX, rawY);
            invokeMouse("onPress", handle, button, 1);
        });
        Thread.sleep(120);
        onClient(() -> invokeMouse("onPress", handle, button, 0));
        Thread.sleep(250);
    }

    private static void invokeMouse(String kind, long handle, Object a, Object b) {
        try {
            Object handler = mc.mouseHandler;
            for (Method m : handler.getClass().getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (kind.equals("onMove") && (m.getName().equals("onMove") || m.getName().equals("onCursorPos"))
                        && p.length >= 3 && p[0] == long.class && p[1] == double.class) {
                    m.setAccessible(true);
                    Object[] args = new Object[p.length];
                    args[0] = handle;
                    args[1] = a;
                    args[2] = b;
                    for (int i = 3; i < p.length; i++) {
                        args[i] = 0.0;
                    }
                    m.invoke(handler, args);
                    return;
                }
                if (kind.equals("onPress") && (m.getName().equals("onPress") || m.getName().equals("onButton"))
                        && p.length >= 3 && p[0] == long.class) {
                    m.setAccessible(true);
                    int button = (Integer) a;
                    int action = (Integer) b;
                    Object[] args = new Object[p.length];
                    args[0] = handle;
                    for (int i = 1; i < p.length; i++) {
                        if (p[i] == int.class) {
                            args[i] = i == 1 && p.length == 4 ? button : i == 2 && p.length == 4 ? action : action;
                        } else {
                            Constructor<?> c = p[i].getDeclaredConstructors()[0];
                            Object[] cargs = new Object[c.getParameterCount()];
                            cargs[0] = button;
                            for (int k = 1; k < cargs.length; k++) {
                                cargs[k] = 0;
                            }
                            args[i] = c.newInstance(cargs);
                        }
                    }
                    if (p.length == 3) {
                        // (window, buttonInfo, action)
                        args[2] = action;
                    }
                    m.invoke(handler, args);
                    return;
                }
            }
            throw new IllegalStateException("no mouse method for " + kind);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void shot(String name) throws Exception {
        log("shot " + name);
        CompletableFuture<Void> done = new CompletableFuture<>();
        String file = "barelysorts_" + LAYER + "_" + name + ".png";
        onClient(() -> {
            try {
                Object target = renderTarget();
                for (Method m : Class.forName("net.minecraft.client.Screenshot").getMethods()) {
                    if (!m.getName().equals("grab") || !Modifier.isStatic(m.getModifiers())) {
                        continue;
                    }
                    Class<?>[] p = m.getParameterTypes();
                    Consumer<Object> after = msg -> done.complete(null);
                    if (p.length == 4) {
                        m.invoke(null, mc.gameDirectory, file, target, after);
                    } else if (p.length == 5) {
                        m.invoke(null, mc.gameDirectory, file, target, 1, after);
                    } else {
                        continue;
                    }
                    return;
                }
                throw new IllegalStateException("no Screenshot.grab");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        done.get(15, TimeUnit.SECONDS);
        log("saved " + file);
    }

    private static Object renderTarget() throws ReflectiveOperationException {
        try {
            return Minecraft.class.getMethod("getMainRenderTarget").invoke(mc);
        } catch (NoSuchMethodException e) {
            Object renderer = Minecraft.class.getField("gameRenderer").get(mc);
            return renderer.getClass().getMethod("mainRenderTarget").invoke(renderer);
        }
    }

    private static void command(String text) throws Exception {
        MinecraftServer server = mc.getSingleplayerServer();
        onServer(server, () -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), text));
        Thread.sleep(60);
    }

    private static void dump(MinecraftServer server, String label) throws Exception {
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            StringBuilder sb = new StringBuilder("inventory ").append(label).append(':');
            for (int i = 0; i < 36; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.isEmpty()) {
                    sb.append(' ').append(i).append('=').append(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath())
                            .append('x').append(s.getCount());
                }
            }
            log(sb.toString());
        });
    }

    private static void dumpChest(MinecraftServer server, int[] pos) throws Exception {
        onServer(server, () -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            var c = (net.minecraft.world.Container) ((ServerLevel) p.level()).getBlockEntity(new BlockPos(pos[0], pos[1], pos[2]));
            StringBuilder sb = new StringBuilder("chest:");
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (!s.isEmpty()) {
                    sb.append(' ').append(i).append('=').append(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath())
                            .append('x').append(s.getCount());
                }
            }
            log(sb.toString());
        });
    }

    private static void onClient(Runnable r) throws InterruptedException {
        CompletableFuture<Void> done = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                r.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            done.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void onServer(MinecraftServer server, Runnable r) throws InterruptedException {
        CompletableFuture<Void> done = new CompletableFuture<>();
        server.execute(() -> {
            try {
                r.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            done.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T callRaw(String name, Object... args) {
        try {
            for (Method m : ScreenActions.class.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    return (T) m.invoke(null, args);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("no ScreenActions." + name);
    }

    private static int call(String name, Object... args) {
        return callRaw(name, args);
    }

    private static int field(String name) {
        try {
            var f = ScreenActions.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Wraps the version layer so the mark key reads as held while {@code forceMark} is set. */
    private static void installKeyOverride() throws Exception {
        var f = Compat.class.getDeclaredField("api");
        f.setAccessible(true);
        while (f.get(null) == null) {
            Thread.sleep(200);
        }
        Compat.Api real = (Compat.Api) f.get(null);
        Compat.Api wrapper = (Compat.Api) Proxy.newProxyInstance(Compat.Api.class.getClassLoader(),
                new Class<?>[]{Compat.Api.class}, (proxy, method, args) -> {
                    if (method.getName().equals("keyDown") && forceShift
                            && ((Integer) args[0] == InputConstants.KEY_LSHIFT)) {
                        return true;
                    }
                    if (method.getName().equals("keyDown") && forceMark
                            && (Integer) args[0] == Platform.keyCode(KeyBindings.MARK)) {
                        return true;
                    }
                    return method.invoke(real, args);
                });
        Compat.init(wrapper);
    }
}
