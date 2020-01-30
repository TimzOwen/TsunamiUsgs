package com.owen.tsunamiusgs;

import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    //tag for log messages;
    public static final String LOG_TAG = MainActivity.class.getName();
    //create a query URL to request USGS Datasets;
    public static final String USG_REQUEST_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //kick off asycTask to perfom an http request
        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();
    }
    //UI to update the information
    private void updateUI(Events earthquake)
    {
        //display Title
        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(earthquake.title);

        // Display the earthquake date in the UI
        TextView dateTextView = findViewById(R.id.date);
        dateTextView.setText(getDateString(earthquake.time));

        // Display whether or not there was a tsunami alert in the UI
        TextView tsunamiTextView = findViewById(R.id.tsunami_alert);
        tsunamiTextView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    //return formatted date and time
    private String getDateString(long timeInMilisecods)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE ,d MMM YYY 'at' HH:mm:ss z");
        return formatter.format(timeInMilisecods);
    }
    //Return displayString whether or not an earthquake occured
    private String getTsunamiAlertString(int tsunamiAlert)
    {
        switch (tsunamiAlert)
        {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.not_available);
        }
    }
    //perform a network request on background thread and then update UI with first element
    private class TsunamiAsyncTask extends AsyncTask<URL,Void, Events>{

        @Override
        protected Events doInBackground(URL... urls) {
            //Create URL object
            URL url = createURL(USG_REQUEST_URL);
            //perform HTTP Request ot URL and get Json Response back
            String jsonRespons="";
            try
            {
                jsonRespons = makeHttpRequest(url);
            }
            catch (IOException e)
            {
                //handle exception
            }
            //extract relevant fields from JSON Response and create "Events" object
            Events earthquakes = extrackFeatureFromJson(jsonRespons);
            //return the result events object as result of the asycTask
            return earthquakes;
        }
    }
    //update the screen with the recent earthquake from the AsycTask
    protected void onPostExecute(Events earthquakes)
    {
        if (earthquakes==null)
        {
            return;
        }
        updateUI(earthquakes);
    }
    //Return new URL Object from the given String
    private URL createURL(String stringUrl)
    {
        URL url = null;
        try
        {
            url = new URL(stringUrl);
        }
        catch (MalformedURLException exception)
        {
            Log.e(LOG_TAG,"Error creating url",exception);
            return null;
        }
        return url;
    }
    //Make HTTP request to the URL and return String as the response
    private String makeHttpRequest(URL url) throws IOException
    {
        String jsonResponse = "";
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try
        {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(1000);
            urlConnection.setConnectTimeout(1500);
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            jsonResponse = readFromStream(inputStream);
        }
        catch (IOException e)
        {
            //handle IOException
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
            if (inputStream !=  null)
            {
                //fn to handle IOException
                inputStream.close();
            }
        }
        return jsonResponse;
    }
    //fn to convert the inputStream into String containing data from the USGS json server response
    private String readFromStream(InputStream inputStream) throws IOException{
        StringBuilder output = new StringBuilder();
        if (inputStream != null)
        {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null)
            {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }
    //fn to return "Event" by passing in the response from json
    private Events extrackFeatureFromJson(String earthquakeJSON){
        try
        {
            JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
            JSONArray featureArray = baseJsonResponse.getJSONArray("features");
            //if the array has feature;
            if (featureArray.length()>0)
            {
                JSONObject firstFeature = featureArray.getJSONObject(0);
                JSONObject propertiesKey = firstFeature.getJSONObject("properties");
                //extract the title, date and tsunami occurance
                String title = propertiesKey.getString("title");
                long time = propertiesKey.getLong("time");
                int tsunamiAlert = propertiesKey.getInt("tsunami");

                //create a new "Events" object
                return new Events(title,time,tsunamiAlert);
            }
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG,"problem fetching JSON Response",e);
        }
        return null;
    }
}