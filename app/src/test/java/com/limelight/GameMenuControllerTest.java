package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.KeyEvent;
import com.limelight.nvstream.input.ControllerPacket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import android.os.Looper;
import org.robolectric.shadows.ShadowAlertDialog;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameMenuControllerTest {
    private Game game;
    private GameMenu menu;

    @Before public void setup() {
        Activity screen = Robolectric.buildActivity(Activity.class).setup().get();
        game = mock(Game.class);
        when(game.getResources()).thenReturn(screen.getResources());
        when(game.getApplicationInfo()).thenReturn(screen.getApplicationInfo());
        menu = new GameMenu(game, screen);
        menu.showMenu(null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test public void firstActionResumesAndRestoresHostInput() {
        assertTrue(menu.isMenuOpen());
        verify(game).setMenuInputSuppressed(true);
        menu.handleControllerInput(ControllerPacket.A_FLAG);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(menu.isMenuOpen());
        verify(game).setMenuInputSuppressed(false);
        verify(game, never()).disconnect();
    }

    @Test public void controllerCanNavigateToBackgroundAction() {
        menu.handleControllerInput(ControllerPacket.DOWN_FLAG);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        menu.handleControllerInput(0);
        menu.handleControllerInput(ControllerPacket.A_FLAG);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        verify(game).keepStreamInBackground();
    }

    @Test public void physicalControllerACanResumeAndBCanCancel() {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A));
        assertFalse(menu.isMenuOpen());
        menu.showMenu(null);
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_B));
        assertFalse(menu.isMenuOpen());
    }
}
