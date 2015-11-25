package com.example.b1013_000.demoactivity;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    //レイアウトのための変数
    private TextView nowTime;
    private TextView dis;
    private TextView time;
    private Button startBt;
    private Button stopBt;

    //センサーのための変数
    private double acX = 0, acY = 0, acZ = 0;
    private SensorManager mSensorManager;

    //タイマーのための変数
    private Timer mTimer1 = null;
    private Timer mTimer2 = null;
    private Timer mTimer3 = null;
    private TimerTask mTimerTask1; //情報収取のためのタイマー
    private TimerTask mTimerTask2; //サーバ送信のためのタイマー
    private TimerTask mTimerTask3; //距離測定のためのタイマー
    private Handler mHandler1 = new Handler();
    private Handler mHandler2 = new Handler();
    private Handler mHandler3 = new Handler();

    //位置情報のための変数
    private LocationManager mLocationManager;
    private double st_lat = 0, st_lng = 0;
    private double en_lat = 0, en_lng = 0;


    //カウンターのための変数
    private int noDump = 0;
    private int highDump = 0;

    //距離測定のための変数
    private float[] results = new float[3];
    private float distance = 0;

    //リスナー
    SensorEventListener SListner = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                acX = event.values[0];
                acY = event.values[1];
                acZ = event.values[2];
                aX.setText(String.valueOf(event.values[0]));
                aY.setText(String.valueOf(event.values[1]));
                aZ.setText(String.valueOf(event.values[2]));
                sum.setText(String.valueOf(event.values[0] + event.values[1] + event.values[2]));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    LocationListener LListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            en_lat = location.getLatitude();
            en_lng = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
    View.OnClickListener CListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.startbt:
                    mTimer1 = new Timer(true);
                    mTimer2 = new Timer(true);
                    mTimer3 = new Timer(true);
                    mTimerTask1 = new MTimerTask1();
                    mTimerTask2 = new MTimerTask2();
                    mTimerTask3 = new MTimerTask3();
                    mTimer1.schedule(mTimerTask1, 1000, 50);
                    mTimer2.schedule(mTimerTask2, 1000, 5000);
                    mTimer3.schedule(mTimerTask3, 1000, 5000);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    break;
                case R.id.stopbt:
                    if(mTimer2 != null){
                        mTimer1.cancel();
                        mTimer2.cancel();
                        mTimer3.cancel();
                        mTimer1 = null;
                        mTimer2 = null;
                        mTimer3 = null;
                        mChronometer.stop();
                        highDump = 0;
                        noDump = 0;
                        distance = 0;
                    }
                    break;
                default:
            }
        }
    };

    //デバッグのための変数
    private TextView aX, aY, aZ, sum, highdump, nodump;
    private Chronometer mChronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startBt = (Button) findViewById(R.id.startbt);
        stopBt = (Button) findViewById(R.id.stopbt);
        aX = (TextView) findViewById(R.id.aX);
        aY = (TextView) findViewById(R.id.aY);
        aZ = (TextView) findViewById(R.id.aZ);
        sum = (TextView) findViewById(R.id.sum);
        highdump = (TextView) findViewById(R.id.highdump);
        nodump = (TextView) findViewById(R.id.nodump);
        dis = (TextView) findViewById(R.id.distance);


        startBt.setOnClickListener(CListener);
        stopBt.setOnClickListener(CListener);


        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = mLocationManager.getBestProvider(criteria, true);
        mLocationManager.requestLocationUpdates(provider, 0, 0, LListener);

        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mChronometer.stop();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(SListner,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

    }

    //情報収集のためのタイマータスク
    private class MTimerTask1 extends TimerTask {
        @Override
        public void run() {
            mHandler1.post(new Runnable() {
                @Override
                public void run() {
                    //閾値判定を入れる
                    if (acZ > 10.9 || acZ< 8.9) {
                        highDump++;
                        highdump.setText("" + highDump);

                    } else {
                        noDump++;
                        nodump.setText("" + noDump);
                    }
//                    System.out.println("highDump = " + highDump);
//                    System.out.println("noDump = " + noDump);

                }
            });
        }
    }

    //サーバ送信のためのタイマータスク
    private class MTimerTask2 extends TimerTask {
        @Override
        public void run() {
            mHandler2.post(new Runnable() {
                @Override
                public void run() {
                    volleyPost(st_lat, st_lng, en_lat, en_lng, noDump, highDump);
                    highDump = 0;
                    noDump = 0;
                    st_lat = en_lat;
                    st_lng = en_lng;
                }
            });
        }
    }

    private class MTimerTask3 extends TimerTask{
        @Override
        public void run() {
            mHandler3.post(new Runnable() {
                @Override
                public void run() {
                    if(st_lat != 0 && st_lng !=0){
                        System.out.println(st_lat);
                        System.out.println(st_lng);
                        System.out.println(en_lat);
                        System.out.println(en_lng);
                        Location.distanceBetween(st_lat, st_lng, en_lat, en_lng, results);
                        distance += results[0];
                        System.out.println("距離:" + distance);
                        dis.setText(distance + "m");
                    }
                }
            });
        }
    }

    private void volleyPost(final double st_lat, final double st_lng, final double en_lat, final double en_lng, final int noDump, final int highDump){
        RequestQueue mQueue = Volley.newRequestQueue(getApplicationContext());
        String url = "http://mirai2015kuru.ddns.net/json_in.php";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("レスポンス", response);
                        Log.d("レスポンス", "1");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("エラーレスポンス", "error");
                    }
                }) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("st_lat", String.valueOf(st_lat));
                params.put("st_lng", String.valueOf(st_lng));
                params.put("en_lat", String.valueOf(en_lat));
                params.put("en_lng", String.valueOf(en_lng));
                params.put("no_dump", String.valueOf(noDump));
                params.put("high_dump", String.valueOf(highDump));
                Log.d("レスポンス", "2");
                return params;
            }
        };
        mQueue.add(postRequest);
    }

}
