package com.example.freeman.test;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by freeman on 3/11/2017.
 */
public class TimerService extends Service {

    private static final String TAG = "TimerService";
    private final int INTERVAL = 2 * 1000;
    private Timer timer = new Timer();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Toast.makeText(this, "TimerService is created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        // Display the Toast Message
        Toast.makeText(this, "Start Service", Toast.LENGTH_SHORT).show();

        // Execute an action after period time
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                // Print a log
                Log.d(TAG, "Start to do an action");
            }
        }, 0, INTERVAL);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        // Display the Toast Message
        Toast.makeText(this, "Stop TimerService", Toast.LENGTH_SHORT).show();
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }

}
