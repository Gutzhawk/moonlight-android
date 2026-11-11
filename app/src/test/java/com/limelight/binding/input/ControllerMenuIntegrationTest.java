package com.limelight.binding.input;

import android.app.Activity;
import android.os.Looper;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.GameGestures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Implementation;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, shadows = ControllerMenuIntegrationTest.ShadowMoonBridge.class)
public class ControllerMenuIntegrationTest {
    @Implements(MoonBridge.class)
    public static class ShadowMoonBridge {
        @Implementation protected static void __staticInitializer__() { }
    }
    private ControllerHandler handler;
    private NvConnection connection;
    private GameGestures gestures;

    @Before public void setup() {
        Activity screen = spy(Robolectric.buildActivity(Activity.class).setup().get());
        // The NVIDIA-only extension service does not exist in the JVM test environment.
        doReturn(false).when(screen).bindService(any(), any(), anyInt());
        doNothing().when(screen).unbindService(any());
        connection = mock(NvConnection.class);
        gestures = mock(GameGestures.class);
        PreferenceConfiguration prefs = new PreferenceConfiguration();
        prefs.onscreenController = true;
        handler = new ControllerHandler(screen, connection, gestures, prefs);
    }

    @After public void cleanup() { handler.destroy(); }

    private void report(int buttons) {
        handler.reportOscState(buttons, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0, (byte) 0);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test public void shortcutReleasesHostAndOpensOnlyOneMenu() {
        report(ControllerPacket.LS_CLK_FLAG);
        clearInvocations(connection);
        report(ControllerPacket.LS_CLK_FLAG | ControllerPacket.RS_CLK_FLAG);
        report(ControllerPacket.LS_CLK_FLAG | ControllerPacket.RS_CLK_FLAG);
        verify(gestures, times(1)).showGameMenu(any());
        verify(connection).sendControllerInput((short) 0, (short) 1, 0,
                (byte) 0, (byte) 0, (short) 0, (short) 0, (short) 0, (short) 0);
        verify(connection, never()).sendControllerInput(anyShort(), anyShort(),
                eq(ControllerPacket.LS_CLK_FLAG | ControllerPacket.RS_CLK_FLAG),
                anyByte(), anyByte(), anyShort(), anyShort(), anyShort(), anyShort());
    }

    @Test public void menuInputStaysLocalAndNormalInputWorksAfterResume() {
        handler.setLocalInputSuppressed(true);
        clearInvocations(connection);
        report(ControllerPacket.A_FLAG);
        verifyNoInteractions(connection);
        handler.setLocalInputSuppressed(false);
        report(ControllerPacket.A_FLAG);
        verify(connection).sendControllerInput((short) 0, (short) 1, ControllerPacket.A_FLAG,
                (byte) 0, (byte) 0, (short) 0, (short) 0, (short) 0, (short) 0);
    }
}
