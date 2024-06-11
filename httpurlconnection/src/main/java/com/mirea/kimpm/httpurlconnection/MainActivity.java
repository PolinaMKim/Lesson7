package com.mirea.kimpm.httpurlconnection;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mirea.kimpm.httpurlconnection.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnFetchData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonClicked(v);
            }
        });
    }

    public void ButtonClicked(View v) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            Toast.makeText(this, "Request network module...", Toast.LENGTH_LONG).show();
            return;
        }

        NetworkInfo networkinfo = connectivityManager.getActiveNetworkInfo();
        if (networkinfo == null || !networkinfo.isConnected()) {
            Toast.makeText(this, "Request network connection...", Toast.LENGTH_LONG).show();
            return;
        }

        new RequestIpInfo().execute();
    }

    private class BaseHttpRequestTask extends AsyncTask<Void, Void, String> {
        private final String address;
        private final String method;

        public BaseHttpRequestTask(String address, String method) {
            this.address = address;
            this.method = method;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return MakeRequest();
            } catch (IOException | RuntimeException e) {
                return null;
            }
        }

        private String MakeRequest() throws IOException, RuntimeException {
            final URL url = new URL(address);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new RuntimeException("Invalid return code");

            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (int read; (read = inputStream.read()) != -1; ) {
                bos.write(read);
            }
            final String result = bos.toString();
            connection.disconnect();
            bos.close();
            return result;
        }
    }

    private class RequestIpInfo extends BaseHttpRequestTask {
        public RequestIpInfo() {
            super("https://ipinfo.io/json", "GET");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                Toast.makeText(MainActivity.this, "Failed to fetch IP info", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject responseJson = new JSONObject(result);
                binding.tvIp.setText(responseJson.getString("ip"));
                binding.tvCity.setText(responseJson.getString("city"));
                binding.tvCountry.setText(responseJson.getString("country"));

                String region = responseJson.optString("region", "N/A");
                binding.tvRegion.setText(region);

                final String[] location = responseJson.getString("loc").split(",");
                new RequestWeatherInfo(Float.parseFloat(location[0]), Float.parseFloat(location[1])).execute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class RequestWeatherInfo extends BaseHttpRequestTask {
        private static final String apiKey = "ae53760321911b952e3646887ead8c6d";

        public RequestWeatherInfo(float latitude, float longitude) {
            super(String.format("https://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f&appid=%s",
                    latitude, longitude, apiKey), "GET");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                Toast.makeText(MainActivity.this, "Failed to fetch weather info", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject responseJson = new JSONObject(result);

                binding.tvTemperature.setText(String.format("%.2f°C", responseJson.getJSONObject("main").getDouble("temp") - 273.15));
                binding.tvWindSpeed.setText(String.format("%.2f m/s", responseJson.getJSONObject("wind").getDouble("speed")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}