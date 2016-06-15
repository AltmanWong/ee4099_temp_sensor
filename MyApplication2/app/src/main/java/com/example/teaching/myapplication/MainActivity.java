package com.example.teaching.myapplication;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {
    Context context;

    //For database use
    public static String jsonResult;
    private String url = "http://192.168.1.47/retrieve.php";

    //For layout handling
    ListView listview;

    //For graph plotting
    LineChart linechart;
    ArrayList<Entry> data = new ArrayList<Entry>();         //Data for x-axis
    ArrayList<Entry> alertdata = new ArrayList<Entry>();    //Data for alert temperature (x-axis)
    ArrayList<String> yasix = new ArrayList<String>();      //Data for y-axis

    //For calculation
    ArrayList<Integer> overTemp = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up the variable
        listview = (ListView) findViewById(R.id.listview);
        linechart = (LineChart) findViewById(R.id.linechart);

        accessWebService();
        setlinechart();
    }

    protected void onResume(){
        super.onResume();
        setContentView(R.layout.activity_main);
        listview = (ListView) findViewById(R.id.listview);
        linechart = (LineChart) findViewById(R.id.linechart);

        final Handler handler = new Handler();
        Runnable refresh = new Runnable(){
            @Override
            public void run() {
                accessWebService();
                setlinechart();
                handler.postDelayed(this, 30000);
            }
        };
        handler.postDelayed(refresh, 30000);
        //accessWebService();
    }

    protected void onDestory(){
        super.onDestroy();
        jsonResult = null;
    }

    public void cleararraylist(){
        data.clear();
        alertdata.clear();
        overTemp.clear();
        yasix.clear();
    }

    public void setlinechart(){
        XAxis xaxis = linechart.getXAxis();
        xaxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftaxis = linechart.getAxisLeft();
        YAxis rightaxis = linechart.getAxisRight();

        leftaxis.setAxisMaxValue(40);
        rightaxis.setEnabled(false);

        LineDataSet currenttempline = new LineDataSet(data, "Current Temperature");
        currenttempline.setColor(getResources().getColor(R.color.blue));
        LineDataSet alerttempline = new LineDataSet(alertdata, "Alert Tempearture");
        alerttempline.setColor(getResources().getColor(R.color.red));
        currenttempline.setAxisDependency(YAxis.AxisDependency.RIGHT);

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(currenttempline);
        dataSets.add(alerttempline);

        LineData data = new LineData(yasix, dataSets);
        linechart.setData(data);
        linechart.invalidate();
    }

    public void notification(){
        int bool = overTemp.get(overTemp.size() - 1);
        if(bool == 1){
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

            int icon = R.drawable.ic_launcher;
            CharSequence alert = "ALERT! OVER TEMP!";
            long when = System.currentTimeMillis();

            Context context = getApplicationContext();
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            Notification notification = new Notification(icon, alert, when);

            mNotificationManager.notify(0, notification);
        }

    }

    private class JsonReadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(params[0]);
            try{
                HttpResponse response = httpclient.execute(httppost);
                Log.i("connection established","");
                jsonResult = inputStreamToString(response.getEntity().getContent()).toString();
                Log.i("result", jsonResult);

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.i("Client Protocol Exception", "");
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("IOExecption", "");
            }
            return null;
        }

        private StringBuilder inputStreamToString(InputStream s) {
            String rLine = "";
            StringBuilder answer = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(s));

            try{
                while((rLine = rd.readLine()) != null){
                    answer.append(rLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return answer;
        }

        @Override
        protected void onPostExecute(String result){
            ListDrawer();
        }
    }

    public void accessWebService(){
        JsonReadTask task = new JsonReadTask();
        task.execute(new String[] {url});
    }

    public void ListDrawer(){
        List<Map<String, String>> temp = new ArrayList<Map<String, String>>();

        try{
            JSONArray jsonResponse = new JSONArray(jsonResult);
            cleararraylist();
            for(int i = 0; i < jsonResponse.length(); i++){
                JSONObject jsonChildNode = jsonResponse.getJSONObject(i);

                //Retrieve data from JSON object
                String time = jsonChildNode.optString("time");
                String temper = jsonChildNode.optString("temp");
                String alert_temper = jsonChildNode.optString("alert_temp");
                String over_temped = jsonChildNode.optString("over_temp");

                //Create hashmap
                Log.i("Record Number", String.valueOf(i));
                HashMap map = new HashMap();
                map.put("time", time);
                Log.i("time", time);
                map.put("temp", temper);
                Log.i("temp", temper);
                map.put("alert_temp", alert_temper);
                Log.i("alert_temp", alert_temper);
                map.put("over_temped", over_temped);
                Log.i("over_temped", over_temped);

                Entry entry = new Entry(Float.parseFloat(temper), i);
                data.add(entry);
                Entry alertentry = new Entry(Float.parseFloat(alert_temper), i);
                alertdata.add(alertentry);
                yasix.add(time);
                overTemp.add(Integer.parseInt(over_temped));
                temp.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SimpleAdapter mytemp = new SimpleAdapter(this, temp, R.layout.adapter_listview,
                new String[] {"time", "temp", "alert_temp", "over_temped"}, new int[] {R.id.textview_time,
        R.id.textview_currenttemp, R.id.textview_alerttemp, R.id.textview_overtemp});
        listview.setAdapter(mytemp);
        Log.i("Adapter set", "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
