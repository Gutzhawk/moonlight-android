package com.limelight.binding.video;

import android.media.MediaCodec;
import android.view.Surface;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DecoderSurfaceSwitchTest {
    private MediaCodecDecoderRenderer renderer;
    private MediaCodec codec;
    private Surface oldSurface;
    private Surface newSurface;

    @Before public void setup() {
        // Codec discovery is device-specific. Exercise the real switching code with a codec double.
        renderer = mock(MediaCodecDecoderRenderer.class, CALLS_REAL_METHODS);
        codec = mock(MediaCodec.class);
        oldSurface = mock(Surface.class);
        newSurface = mock(Surface.class);
        when(newSurface.isValid()).thenReturn(true);
        ReflectionHelpers.setField(renderer, "codecRecoveryMonitor", new Object());
        ReflectionHelpers.setField(renderer, "videoDecoder", codec);
        ReflectionHelpers.setField(renderer, "renderTarget", oldSurface);
        ReflectionHelpers.setField(renderer, "decoderStarted", true);
    }

    @Test public void backgroundAndReturnOnlyChangeOutputSurface() {
        assertTrue(renderer.replaceRenderTarget(newSurface, true));
        when(oldSurface.isValid()).thenReturn(true);
        assertTrue(renderer.replaceRenderTarget(oldSurface));
        verify(codec).setOutputSurface(newSurface);
        verify(codec).setOutputSurface(oldSurface);
        verifyNoMoreInteractions(codec);
        assertFalse((boolean) ReflectionHelpers.getField(renderer, "backgroundOutput"));
    }

    @Test public void unsupportedSwitchKeepsPreviousTarget() {
        doThrow(new IllegalStateException("unsupported codec")).when(codec).setOutputSurface(newSurface);
        assertFalse(renderer.replaceRenderTarget(newSurface, true));
        assertSame(oldSurface, ReflectionHelpers.getField(renderer, "renderTarget"));
    }

    @Test public void stoppingDecoderNeverUsesNewSurface() {
        ReflectionHelpers.setField(renderer, "stopping", true);
        assertFalse(renderer.replaceRenderTarget(newSurface, true));
        verifyNoInteractions(codec);
    }
}
