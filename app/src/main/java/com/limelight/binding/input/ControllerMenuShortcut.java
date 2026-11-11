package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

/** Per-device latch: one menu opening until BOTH stick buttons have been released. */
final class ControllerMenuShortcut {
    private static final int STICKS = ControllerPacket.LS_CLK_FLAG | ControllerPacket.RS_CLK_FLAG;
    private boolean consumed;

    boolean update(int buttons) {
        if ((buttons & STICKS) == 0) {
            consumed = false;
        } else if (!consumed && (buttons & STICKS) == STICKS) {
            consumed = true;
            return true;
        }
        return false;
    }

    int filter(int buttons) {
        return consumed ? buttons & ~STICKS : buttons;
    }
}
