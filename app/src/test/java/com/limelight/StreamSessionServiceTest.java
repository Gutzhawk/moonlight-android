package com.limelight;

import android.content.Intent;
import android.os.PowerManager;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class StreamSessionServiceTest {
    @After public void clearGame() { Game.instance = null; }

    @Test public void activeStreamHasOngoingNotificationAndReleasesWakeLockOnStop() {
        Game.instance = mock(Game.class);
        StreamSessionService service = Robolectric.buildService(StreamSessionService.class).create().get();
        service.onStartCommand(new Intent(), 0, 1);
        android.app.Notification notification = Shadows.shadowOf(service).getLastForegroundNotification();
        assertNotNull(notification);
        assertEquals(2, notification.actions.length);
        assertNotNull(notification.contentIntent);
        PowerManager.WakeLock lock = ReflectionHelpers.getField(service, "wakeLock");
        assertTrue(lock.isHeld());
        service.onDestroy();
        assertFalse(lock.isHeld());
    }

    @Test public void stopActionDisconnectsStream() {
        Game.instance = mock(Game.class);
        StreamSessionService service = Robolectric.buildService(StreamSessionService.class).create().get();
        service.onStartCommand(new Intent().setAction(StreamSessionService.ACTION_STOP), 0, 1);
        verify(Game.instance).disconnect();
        assertTrue(Shadows.shadowOf(service).isStoppedBySelf());
    }

    @Test public void serviceDoesNotInventSessionAfterProcessDeath() {
        Game.instance = null;
        StreamSessionService service = Robolectric.buildService(StreamSessionService.class).create().get();
        assertEquals(android.app.Service.START_NOT_STICKY, service.onStartCommand(null, 0, 1));
        assertTrue(Shadows.shadowOf(service).isStoppedBySelf());
    }
}
