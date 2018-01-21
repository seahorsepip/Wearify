package com.seapip.thomas.wearify.tests;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;

import com.seapip.thomas.wearify.Callback;
import com.seapip.thomas.wearify.spotify.controller.Controller;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.wearify.Manager;
import com.seapip.thomas.wearify.wearify.Token;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class PlaybackTest {
    private static final String accessToken = "<snip>";
    private static final String deviceId = "<snip>";

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();
    private final Token token = new Token();
    private Service service;

    @Before
    public void setUp() throws Exception {
        token.access_token = accessToken;
        token.refresh_token = "<no-need-to-refresh-during-tests>";
        token.expires_in = 60;
        Manager.setToken(InstrumentationRegistry.getContext(), token);
        service = ((Service.ControllerBinder) mServiceRule.bindService(
                new Intent(InstrumentationRegistry.getTargetContext(), Service.class))
        ).getServiceInstance();
    }

    @Test
    public void testNativePlayback() throws TimeoutException, InterruptedException {
        Controller controller = setController(Service.Companion.getNATIVE_CONTROLLER(), null);

        controller.play(null, "spotify:album:74EKsgjD5GJOJpthJ59dhQ",
                2, false, "off", 0);
        Assert.assertTrue("Playback didn't start", getPlayback(controller).is_playing);
        Assert.assertEquals("Playback didn't start correct song", getPlayback(controller).item.name, "M.O.N.E.Y.");

        controller.pause();
        Assert.assertFalse("Playback didn't pause", getPlayback(controller).is_playing);

        controller.resume();
        Assert.assertTrue("Playback didn't resume", getPlayback(controller).is_playing);

        controller.next();
        Assert.assertEquals("Playback didn't skip to next track", getPlayback(controller).item.name, "Chocolate");

        controller.seek(60000);
        Assert.assertTrue("Playback didn't seek one minute forward", getPlayback(controller).progress_ms > 60000);

        int volume = 20 + (int) Math.floor(Math.random() * 80);
        controller.volume(volume);
        Assert.assertEquals("Playback didn't change volume", getPlayback(controller).device.volume_percent, volume);

        controller.previous();
        Assert.assertEquals("Playback didn't skip to previous track", getPlayback(controller).item.name, "M.O.N.E.Y.");
    }

    @Test
    public void testConnectedPlayback() throws TimeoutException, InterruptedException {
        Controller controller = setController(Service.Companion.getCONNECT_CONTROLLER(), deviceId);

        controller.play(null, "spotify:album:74EKsgjD5GJOJpthJ59dhQ",
                2, false, "off", 0);

        Assert.assertTrue("Playback didn't start", getPlayback(controller).is_playing);
        Assert.assertEquals("Playback didn't start correct song", getPlayback(controller).item.name, "M.O.N.E.Y.");

        controller.pause();
        Assert.assertFalse("Playback didn't pause", getPlayback(controller).is_playing);

        controller.resume();
        Assert.assertTrue("Playback didn't resume", getPlayback(controller).is_playing);

        controller.next();
        Assert.assertEquals("Playback didn't skip to next track", getPlayback(controller).item.name, "Chocolate");

        controller.seek(60000);
        Assert.assertTrue("Playback didn't seek one minute forward", getPlayback(controller).progress_ms > 60000);

        int volume = 20 + (int) Math.floor(Math.random() * 80);
        controller.volume(volume);
        Assert.assertEquals("Playback didn't change volume", getPlayback(controller).device.volume_percent, volume);

        controller.previous();
        Assert.assertEquals("Playback didn't skip to previous track", getPlayback(controller).item.name, "M.O.N.E.Y.");
    }

    @Test
    public void testNativeToConnectedPlaybackTransfer() throws TimeoutException, InterruptedException {
        Controller controller = setController(Service.Companion.getNATIVE_CONTROLLER(), null);

        controller.play(null, "spotify:album:3smvpv7CdrhVcGYaNDLOqn",
                -1, true, "track", 30000);
        Thread.sleep(2000);

        controller = setController(Service.Companion.getCONNECT_CONTROLLER(), deviceId);
        Assert.assertEquals("Playback didn't transfer between devices", getPlayback(controller).device.id, deviceId);
        Assert.assertEquals("Track state didn't transfer between devices", getPlayback(controller).item.name, "Despacito - Remix");
        Assert.assertEquals("Shuffle state didn't transfer between devices", getPlayback(controller).shuffle_state, true);
        Assert.assertEquals("Repeat state didn't transfer between devices", getPlayback(controller).repeat_state, "track");
        Assert.assertTrue("Progress didn't transfer between devices", getPlayback(controller).progress_ms > 30000);
    }

    @Test
    public void testConnectedToNativePlaybackTransfer() throws TimeoutException, InterruptedException {
        Controller controller = setController(Service.Companion.getCONNECT_CONTROLLER(), deviceId);
        controller.play(null, "spotify:album:3smvpv7CdrhVcGYaNDLOqn",
                -1, true, "track", 30000);
        Thread.sleep(2000);

        controller = setController(Service.Companion.getNATIVE_CONTROLLER(), deviceId);
        Assert.assertEquals("Playback didn't transfer between devices", getPlayback(controller).device.id, deviceId);
        Assert.assertEquals("Track state didn't transfer between devices", getPlayback(controller).item.name, "Despacito - Remix");
        Assert.assertEquals("Shuffle state didn't transfer between devices", getPlayback(controller).shuffle_state, true);
        Assert.assertEquals("Repeat state didn't transfer between devices", getPlayback(controller).repeat_state, "track");
        Assert.assertTrue("Progress didn't transfer between devices", getPlayback(controller).progress_ms > 30000);
    }

    private Controller setController(int controllerId, String deviceId) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Controller[] controller = new Controller[1];
        service.setController(controllerId, deviceId, new Callback<Controller>() {
            @Override
            public void onSuccess(Controller c) {
                controller[0] = c;
                latch.countDown();
            }

            @Override
            public void onError() {
                Assert.fail("Unable to get controller");
                latch.countDown();
            }
        });
        try {
            Thread.sleep(2000);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return controller[0];
    }

    private CurrentlyPlaying getPlayback(Controller controller) {
        final CountDownLatch latch = new CountDownLatch(1);
        final CurrentlyPlaying[] currentlyPlaying = new CurrentlyPlaying[1];
        controller.getPlayback(new Callback<CurrentlyPlaying>() {
            @Override
            public void onSuccess(CurrentlyPlaying c) {
                currentlyPlaying[0] = c;
                latch.countDown();
            }

            @Override
            public void onError() {
                Assert.fail("Unable to get playback");
                latch.countDown();
            }
        });
        try {
            Thread.sleep(3000);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return currentlyPlaying[0];
    }
}
