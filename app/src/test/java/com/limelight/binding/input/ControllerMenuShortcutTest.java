package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;
import org.junit.Test;
import static org.junit.Assert.*;

public class ControllerMenuShortcutTest {
    private static final int L = ControllerPacket.LS_CLK_FLAG;
    private static final int R = ControllerPacket.RS_CLK_FLAG;

    @Test public void eitherPressOrderOpensOnceUntilBothReleased() {
        for (int first : new int[]{L, R}) {
            ControllerMenuShortcut shortcut = new ControllerMenuShortcut();
            assertFalse(shortcut.update(first));
            assertTrue(shortcut.update(L | R));
            assertFalse(shortcut.update(L | R));
            assertFalse(shortcut.update(first));
            assertFalse(shortcut.update(L | R));
            assertFalse(shortcut.update(0));
            assertTrue(shortcut.update(L | R));
        }
    }

    @Test public void normalSingleStickClicksPassThrough() {
        ControllerMenuShortcut shortcut = new ControllerMenuShortcut();
        assertFalse(shortcut.update(L));
        assertEquals(L, shortcut.filter(L));
        assertFalse(shortcut.update(0));
        assertFalse(shortcut.update(R));
        assertEquals(R, shortcut.filter(R));
    }

    @Test public void consumedComboDoesNotReachGameAndDoesNotEatOtherButtons() {
        ControllerMenuShortcut shortcut = new ControllerMenuShortcut();
        assertTrue(shortcut.update(L | R | ControllerPacket.A_FLAG));
        assertEquals(ControllerPacket.A_FLAG, shortcut.filter(L | R | ControllerPacket.A_FLAG));
        shortcut.update(L);
        assertEquals(0, shortcut.filter(L));
    }

    @Test public void separateControllersCannotCombineTheirStickClicks() {
        ControllerMenuShortcut first = new ControllerMenuShortcut();
        ControllerMenuShortcut second = new ControllerMenuShortcut();
        assertFalse(first.update(L));
        assertFalse(second.update(R));
    }
}
