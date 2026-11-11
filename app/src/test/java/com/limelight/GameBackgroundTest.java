package com.limelight;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.Surface;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.video.BackgroundSurface;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.nvstream.NvConnection;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameBackgroundTest {
    // Exercise Game's real visibility/surface callbacks without starting a network connection.
    public static class TestGame extends Game {
        @Override protected void onCreate(Bundle state) { }
    }
    private TestGame game;
    private MediaCodecDecoderRenderer decoder;
    private ControllerHandler controller;
    private BackgroundSurface background;

    @Before public void setup() {
        game = Robolectric.buildActivity(TestGame.class).create().get();
        game.conn = mock(NvConnection.class);
        game.connected = true;
        decoder = mock(MediaCodecDecoderRenderer.class);
        controller = mock(ControllerHandler.class);
        background = mock(BackgroundSurface.class);
        ReflectionHelpers.setField(game, "decoderRenderer", decoder);
        ReflectionHelpers.setField(game, "controllerHandler", controller);
        ReflectionHelpers.setField(game, "backgroundSurface", background);
        ReflectionHelpers.setField(game, "attemptedConnection", true);
        ReflectionHelpers.setField(game, "surfaceCreated", true);
        ReflectionHelpers.setField(game, "prefConfig", new PreferenceConfiguration());
        ReflectionHelpers.setField(game, "tombstonePrefs", mock(android.content.SharedPreferences.class));
    }

    @Test public void homeKeepsSameConnectionAliveAndReturnResumesInput() {
        game.onStreamWindowStarted();
        game.onStreamWindowStopped();
        assertFalse(game.isFinishing());
        assertTrue(game.connected);
        verify(game.conn, never()).stop();
        verify(controller).setLocalInputSuppressed(true);
        game.onStreamWindowStarted();
        verify(controller, times(2)).setLocalInputSuppressed(false);
        verify(decoder, times(2)).notifyVideoForeground();
    }

    @Test public void losingWindowRedirectsDecoderWithoutStoppingStream() {
        Surface output = mock(Surface.class);
        when(background.getSurface()).thenReturn(output);
        when(decoder.replaceRenderTarget(output, true)).thenReturn(true);
        game.surfaceDestroyed(mock(SurfaceHolder.class));
        verify(decoder).replaceRenderTarget(output, true);
        verify(decoder, never()).prepareForStop();
        verify(game.conn, never()).stop();
        assertFalse((boolean) ReflectionHelpers.getField(game, "surfaceCreated"));
    }

    @Test public void backgroundingWhileConnectingDoesNotFinishActivity() {
        game.connected = false;
        ReflectionHelpers.setField(game, "connecting", true);
        game.onStreamWindowStopped();
        assertFalse(game.isFinishing());
        verify(game.conn, never()).stop();
    }

    @Test public void notificationDisconnectExplicitlyFinishesActivity() {
        game.disconnect();
        assertTrue(game.isFinishing());
    }

    @Test public void explicitDisconnectStopsConnectionAndReleasesBackgroundResources() {
        game.disconnect();
        game.onStreamWindowStopped();
        verify(game.conn, timeout(1000)).stop();
        verify(background, timeout(1000)).release();
        verify(controller).stop();
        verify(decoder).prepareForStop();
    }
}
