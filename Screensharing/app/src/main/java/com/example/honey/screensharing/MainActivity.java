package com.example.honey.screensharing;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
                          implements EasyPermissions.PermissionCallbacks,
                                     Session.SessionListener,
                                     Publisher.PublisherListener,
                                     Subscriber.VideoListener {

    private static final String TAG = "hello-world " + MainActivity.class.getSimpleName();

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    boolean IS_CAMERA_APP_OPEN = false;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    WebView mWebViewContainer;
    private RelativeLayout mPublisherViewContainer;
    private LinearLayout mSubscriberViewContainer;

    private Handler handler;
    int timeInMilliseconds = 100;
    int cameraRetries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button swapCamera = (Button)findViewById(R.id.button2);
        swapCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlexibleCameraCapturer)(mPublisher.getCapturer())).cycleCamera();
            }
        });

        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (LinearLayout) findViewById(R.id.subscriberview);

        handler = new Handler();

        this.getApplicationContext();

        requestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (mSession == null) {
            return;
        }
        mSession.onResume();

        //re-obtain the camera for the Publisher after returning to the activity
//        if(IS_CAMERA_APP_OPEN) {
//            IS_CAMERA_APP_OPEN = false;
//            ((FlexibleCameraCapturer) (mPublisher.getCapturer())).init();
//            ((FlexibleCameraCapturer)(mPublisher.getCapturer())).swapCamera(((FlexibleCameraCapturer)(mPublisher.getCapturer())).getCameraIndex());
//        }

        if(IS_CAMERA_APP_OPEN){
            IS_CAMERA_APP_OPEN = false;
            runnable.run();
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                cameraRetries++;
                ((FlexibleCameraCapturer) (mPublisher.getCapturer())).init();
                ((FlexibleCameraCapturer) (mPublisher.getCapturer())).startCapture();
            } catch (Exception e) {
                if(timeInMilliseconds < 1000){
                    handler.postDelayed(runnable, timeInMilliseconds);
                    timeInMilliseconds = timeInMilliseconds * 2;
                }
                else{
                    Log.e(TAG, "error re-obtaining camera");
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        // relinquish the camera without destroying the publisher
        // ((FlexibleCameraCapturer)(mPublisher.getCapturer())).releaseCamera();
        ((FlexibleCameraCapturer)(mPublisher.getCapturer())).destroy();
        IS_CAMERA_APP_OPEN = true;

        // call Session.onPause()
        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        if (EasyPermissions.hasPermissions(this, perms)) {
            mSession = new Session(MainActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }
    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());

        // use the custom capturer instead of the default one
        // mPublisher = new Publisher(MainActivity.this, "publisher");
        mPublisher = new Publisher(MainActivity.this, "publisher", new FlexibleCameraCapturer(MainActivity.this));

        mPublisher.setPublisherListener(this);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        mPublisherViewContainer.addView(mPublisher.getView());

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());
        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber != null) {
            return;
        }

        subscribeToStream(stream);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        if (OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber == null) {
            return;
        }

        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");
        System.out.println("on stream created video status is " + mPublisher.getPublishVideo());

        System.out.println("on stream created audio status is " + mPublisher.getPublishAudio());

        if (!OpenTokConfig.SUBSCRIBE_TO_SELF) {
            return;
        }

        subscribeToStream(stream);
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mSubscriberViewContainer.addView(mSubscriber.getView());
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }

    private void subscribeToStream(Stream stream) {
        mSubscriber = new Subscriber(MainActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSession.unsubscribe(mSubscriber);
            mSubscriber.destroy();
            mSubscriber = null;
        }

        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }
}
