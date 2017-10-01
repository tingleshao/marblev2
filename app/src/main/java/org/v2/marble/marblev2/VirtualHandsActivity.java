package org.v2.marble.marblev2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ImageTarget;
import com.vuforia.ObjectTracker;
import com.vuforia.Rectangle;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.VirtualButton;
import com.vuforia.Vuforia;

import android.hardware.SensorManager;


import java.util.Vector;

/**
 * Created by chongshao on 9/21/17.
 */
public class VirtualHandsActivity extends Activity implements SampleApplicationControl, SensorEventListener {

    // Enumeration for masking button indices into single integer:
    private static final int BUTTON_1 = 1;
    private static final int BUTTON_2 = 2;
    private static final int BUTTON_3 = 4;
    private static final int BUTTON_4 = 8;

    private byte buttonMask = 0;
    static final int NUM_BUTTONS = 4;
    public String virtualButtonColors[] = { "red", "blue", "yellow", "green" };

    private static final String LOGTAG = "DDL";

    private DataSet dataSet = null;

    private VirtualHandsRenderer mRenderer;

    private SampleApplicationGLView mGlView;

    private RelativeLayout mUILayout;

    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    SampleApplicationSession vuforiaAppSession;

    private Vector<Texture> mTextures;

    private AlertDialog mErrorDialog;

    private GestureDetector mGestureDetector;

    private boolean updateBtns = false;

    String texturename = "fabric.jpg";

    //IMU
    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor, mAccelerationSensor, mGravitySensor, mMagSensor;
    // new
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    float[] prevR = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new SampleApplicationSession(this);

        startLoadingAnimation();

        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();

        mGestureDetector = new GestureDetector(this, new GestureListener());

     //   mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
     //           "droid");

        // IMU
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR);
        mAccelerationSensor = mSensorManager.getDefaultSensor(
                Sensor.TYPE_LINEAR_ACCELERATION);

        mGravitySensor = mSensorManager.getDefaultSensor(
                Sensor.TYPE_GRAVITY);
        mMagSensor = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
        mSensorManager.registerListener(this, mAccelerationSensor, 5000);

        mSensorManager.registerListener(this, mGravitySensor, 10000);
        mSensorManager.registerListener(this, mMagSensor, 10000);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // rotation
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && (gravityValues != null) && (magneticValues != null)) {
            long startTime = System.currentTimeMillis();

            float[] R = new float[16], I = new float[16];
            float[] angleChange = null;
            SensorManager.getRotationMatrixFromVector(R, event.values);
            if (prevR == null) {
                prevR = R;
            } else {
                angleChange = new float[3];
                SensorManager.getAngleChange(angleChange, R, prevR);
            }
       //     if (angleChange != null) {
        //        VirtualHandsActivity.this.mRenderer.rotate3(angleChange[0]  / (-100.0f * (float) Math.PI));
        //        VirtualHandsActivity.this.mRenderer.rotate2(angleChange[1] / (-100.0f * (float) Math.PI));
         //       VirtualHandsActivity.this.mRenderer.rotate1(angleChange[2]  / (-100.0f * (float) Math.PI));
        //    }
            prevR = R;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();

        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);

                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);

            return true;
        }
    }

    // We want to load specific textures from the APK, which we will later use
    // for rendering.
    private void loadTextures() {
        mTextures.add(Texture.loadTextureFromApk(texturename,
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk(texturename,
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk(texturename,
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk(
                texturename, getAssets()));
        mTextures.add(Texture.loadTextureFromApk(
                texturename, getAssets()));
    }

    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        // This is needed for some Droid devices to force portrait
     //   if (mIsDroidDevice)
    //    {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      //  }

        try {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Resume the GL view:
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        vuforiaAppSession.onConfigurationChanged();
    }

    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }

    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Process the Gestures
      //  if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
     //       return true;

        return mGestureDetector.onTouchEvent(event);
    }


    private void startLoadingAnimation() {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
                null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }


    // Create/destroy a Virtual Button at runtime
    //
    // Note: This will NOT work if the tracker is active!
    boolean toggleVirtualButton(ImageTarget imageTarget, String name,
                                float left, float top, float right, float bottom)
    {
        Log.d(LOGTAG, "toggleVirtualButton");

        boolean buttonToggleSuccess = false;

        VirtualButton virtualButton = imageTarget.getVirtualButton(name);
        if (virtualButton != null)
        {
            Log.d(LOGTAG, "Destroying Virtual Button> " + name);
            buttonToggleSuccess = imageTarget
                    .destroyVirtualButton(virtualButton);
        } else
        {
            Log.d(LOGTAG, "Creating Virtual Button> " + name);
            Rectangle vbRectangle = new Rectangle(left, top, right, bottom);
            VirtualButton virtualButton2 = imageTarget.createVirtualButton(
                    name, vbRectangle);

            if (virtualButton2 != null)
            {
                // This is just a showcase. The values used here a set by
                // default on Virtual Button creation
                virtualButton2.setEnabled(true);
                virtualButton2.setSensitivity(VirtualButton.SENSITIVITY.MEDIUM);
                buttonToggleSuccess = true;
            }
        }

        return buttonToggleSuccess;
    }
    private void addButtonToToggle(int virtualButtonIdx)
    {
        Log.d(LOGTAG, "addButtonToToggle");

        assert (virtualButtonIdx >= 0 && virtualButtonIdx < NUM_BUTTONS);

        switch (virtualButtonIdx)
        {
            case 0:
                buttonMask |= BUTTON_1;
                break;

            case 1:
                buttonMask |= BUTTON_2;
                break;

            case 2:
                buttonMask |= BUTTON_3;
                break;

            case 3:
                buttonMask |= BUTTON_4;
                break;
        }
        updateBtns = true;
    }

//    // This method sets the menu's settings
//    private void setSampleAppMenuSettings()
//    {
//        SampleAppMenuGroup group;
//
//        group = mSampleAppMenu.addGroup("", false);
//        group.addTextItem(getString(R.string.menu_back), -1);
//
//        group = mSampleAppMenu.addGroup("", true);
//        group.addSelectionItem(getString(R.string.menu_button_red),
//                CMD_BUTTON_RED, true);
//        group.addSelectionItem(getString(R.string.menu_button_blue),
//                CMD_BUTTON_BLUE, true);
//        group.addSelectionItem(getString(R.string.menu_button_yellow),
//                CMD_BUTTON_YELLOW, true);
//        group.addSelectionItem(getString(R.string.menu_button_green),
//                CMD_BUTTON_GREEN, true);
//
//        mSampleAppMenu.attachMenu();
//    }

//
//    @Override
//    public boolean menuProcess(int command)
//    {
//        boolean result = true;
//
//        switch (command)
//        {
//            case CMD_BACK:
//                finish();
//                break;
//
//            case CMD_BUTTON_RED:
//                addButtonToToggle(0);
//                break;
//
//            case CMD_BUTTON_BLUE:
//                addButtonToToggle(1);
//                break;
//
//            case CMD_BUTTON_YELLOW:
//                addButtonToToggle(2);
//                break;
//
//            case CMD_BUTTON_GREEN:
//                addButtonToToggle(3);
//                break;
//
//        }
//
//        return result;
//    }


    @Override
    public void onVuforiaUpdate(State state) {
      //  Log.d("DDL", "on Vuforia update called");
    }

    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }


    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            Log.d(
                    LOGTAG,
                    "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        if (dataSet != null)
        {
            if (!objectTracker.deactivateDataSet(dataSet))
            {
                Log.d(
                        LOGTAG,
                        "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            } else if (!objectTracker.destroyDataSet(dataSet))
            {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.");
                result = false;
            }

            if (result)
                Log.d(LOGTAG, "Successfully destroyed the data set.");

            dataSet = null;
        }

        return result;
    }


    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }


    @Override
    public void onInitARDone(SampleApplicationException exception)
    {

        if (exception == null)
        {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            // Hides the Loading Dialog
            loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");

        //    mSampleAppMenu = new SampleAppMenu(this, this, "Virtual Buttons",
        //            mGlView, mUILayout, null);
        //    setSampleAppMenuSettings();

        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }


    @Override
    public boolean doLoadTrackersData()
    {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager
                .getTracker(ObjectTracker.getClassType()));
        if (objectTracker == null)
        {
            Log.d(
                    LOGTAG,
                    "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        // Create the data set:
        dataSet = objectTracker.createDataSet();
        if (dataSet == null)
        {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        // Load the data set:
        if (!dataSet.load("Wood.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
        {
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }

        // Activate the data set:
        if (!objectTracker.activateDataSet(dataSet))
        {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }

    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }


    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }


    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new VirtualHandsRenderer(this, vuforiaAppSession);

        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        mGlView.setRenderer2(mRenderer);

    }

    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        VirtualHandsActivity.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int id)
                                    {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
        //        mErrorDialog.show();
            }
        });
    }

}



