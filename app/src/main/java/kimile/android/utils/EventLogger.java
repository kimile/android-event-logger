package kimile.android.utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Simple Accelerometer and magnetic field event logger that will take 10 sec. samples of
 * data.
 * It will give 10 sec. countdown before start logging events and then will log for another 10 sec. and stop.
 */
public class EventLogger extends ActionBarActivity implements SensorEventListener {

    private static final float ALPHA = 0.8f;
    private static final double THRESHHOLD = .06;
    private static final int POSITIONS = 3;
    private float[] gravity;
    private Double acceleration;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magneticField;

    private TextView uiAccelerationTv;
    private TextView textTimer;
    private Button uiBtnStartStop;
    private boolean reading = false;
    private long start = 0;
    private double last = -1000;
    private int total = 0;
    private float[] latest;
    private List<String> accelerationEvents;
    private List<String> magneticFieldEvents;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_logger);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gravity = new float[3];
        latest = new float[POSITIONS];


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        uiAccelerationTv = (TextView) findViewById(R.id.acceleration);
        textTimer = (TextView) findViewById(R.id.stop_watch);
        uiBtnStartStop = (Button) findViewById(R.id.btn_start_stop);

        // Start/stop event handler
        uiBtnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reading) {
                    stopReading();
                } else {
                    startReading();
                }
            }
        });


    }


    /**
     * Start monitoring and recording events
     */
    public void startReading() {
        accelerationEvents = new ArrayList<String>(500);
        magneticFieldEvents = new ArrayList<String>(500);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);
        startTime = SystemClock.uptimeMillis();
        reading = true;
        stop = false;
        total = 0;
        last = -1000;


        start = System.currentTimeMillis();
        startChrono();
    }

    /**
     * Stop monitoring and recording events
     */
    public void stopReading() {
        mSensorManager.unregisterListener(this);
        reading = false;
        recording = false;

        Log.w("counter", "" + accelerationEvents.size());
        String content = "";
        long milis = System.currentTimeMillis();
        for (String line : accelerationEvents) {
            content += line + "\n";
        }
        writeToSDFile(content, "acceleration-log-" + milis + ".csv");

        content = "";
        for (String line : magneticFieldEvents) {
            content += line + "\n";
        }
        writeToSDFile(content, "magnetic-log-" + milis + ".csv");
    }

    /**
     * Start countdown
     */
    public void startChrono() {
        myHandler.postDelayed(updateTimerMethod, 0);
    }

    /**
     * Stop chronometer
     */
    public void stopChrono() {
        myHandler.removeCallbacks(updateTimerMethod);
    }

    /**
     * Record acceleration data and add it to List in csv format
     *
     * @param values
     */
    public void recordAcceleration(float[] values) {
        values = highPass(values[0], values[1], values[2]);
        float x = values[0], y = values[1], z = values[2];

        // calculate total acceleration, no matter direction
        double sumOfSquares = (x * x) + (y * y) + (z * z);

        acceleration = Math.sqrt(sumOfSquares);

        if (acceleration < THRESHHOLD) return;


        if (last == -1000) {
            last = acceleration;
        } else if (last < acceleration && x > 0 && Math.abs((last - acceleration)) > .5) {
            total += 1;
        }

        last = acceleration;
        uiAccelerationTv.setText((Math.round(acceleration * 100.0) / 100.0) + "");
        accelerationEvents.add(System.currentTimeMillis() + "," + x + "," + y + "," + z);
    }

    /**
     * Record magnetic field sensor data and add it to List in csv format
     * @param values
     */
    public void recordMagneticField(float[] values) {
        float x = values[0], y = values[1], z = values[2];
        magneticFieldEvents.add(System.currentTimeMillis() + "," + x + "," + y + "," + z);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logger, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * This method derived from the Android documentation and is available under
     * the Apache 2.0 license.
     *
     * @see //developer.android.com/reference/android/hardware/SensorEvent.html
     */
    private float[] highPass(float x, float y, float z) {
        float[] filteredValues = new float[3];

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

        filteredValues[0] = x - gravity[0];
        filteredValues[1] = y - gravity[1];
        filteredValues[2] = z - gravity[2];

        return filteredValues;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (!recording) {
            return;
        }

        float[] values = sensorEvent.values;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            recordAcceleration(values);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            recordMagneticField(values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // don't care
    }

    private void writeToSDFile(String out, String filename) {

        if (false) {

            // Find the root of the external storage.
            // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

            File root = android.os.Environment.getExternalStorageDirectory();

            // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

            File dir = new File(root.getAbsolutePath() + "/download");
            dir.mkdirs();
            File file = new File(dir, filename);

            try {
                FileOutputStream log = new FileOutputStream(file, true);
                PrintWriter logWriter = new PrintWriter(log);
                logWriter.println(out);
                logWriter.flush();
                logWriter.close();
                log.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.w("Writting", "Writting file");
        }
    }


    /**
     * All the chrono related stuff bellow
     * ===================================
     */

    private long startTime = 0L;
    private Handler myHandler = new Handler();
    long timeInMillies = 0L;
    long timeSwap = 0L;
    long finalTime = 0L;
    int seconds = 0;
    long countdown = 10000;
    long captureFor = 10000;
    boolean recording = false;
    boolean stop = false;

    private Runnable updateTimerMethod = new Runnable() {

        public void run() {

            if (stop) {
                stopReading();
                stopChrono();
                return;
            }

            timeInMillies = SystemClock.uptimeMillis() - startTime;
            finalTime = timeSwap + timeInMillies;

            // count downwards until zero and then from zero onwards
            if ((countdown - finalTime) > 0) {
                finalTime = countdown - finalTime;
            } else {
                finalTime -= countdown;
                if (!recording) {
                    recording = true;
                }
            }

            seconds = (int) (finalTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            int milliseconds = (int) (finalTime % 1000);
            textTimer.setText("" + minutes + ":"
                    + String.format("%02d", seconds) + ":"
                    + String.format("%03d", milliseconds));
            myHandler.postDelayed(this, 0);


            if (recording && finalTime > captureFor) {
                recording = false;
                // when calling stopReading here, screen wouldn't update crono until files are
                // written... allow to update and then write to fs
                stop = true;
            }

        }

    };
}