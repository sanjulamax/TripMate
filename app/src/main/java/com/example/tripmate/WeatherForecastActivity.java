package com.example.tripmate;



import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

// For real-world use, move network to ViewModel/AsyncTask/Coroutine!
public class WeatherForecastActivity extends AppCompatActivity {

    private RecyclerView recyclerWeather;
    private WeatherForecastAdapter adapter;
    private List<WeatherForecast> weatherList = new ArrayList<>();
    private String tripDate;
    private List<String> destinations;
    // Replace with your OpenWeatherMap API key
    private final String WEATHER_API_KEY = "3017e3e6929cc8391ff616c7cccf4fd4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forcast);

        recyclerWeather = findViewById(R.id.recyclerWeather);
        recyclerWeather.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeatherForecastAdapter(weatherList);
        recyclerWeather.setAdapter(adapter);

        // Get trip info from Intent
        tripDate = getIntent().getStringExtra("date");
        destinations = getIntent().getStringArrayListExtra("destinations");
        if (tripDate == null || destinations == null || destinations.isEmpty()) {
            Toast.makeText(this, "No destinations or date provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // For each destination, get forecast for the trip date
        for (String city : destinations) {
            fetchWeatherForDestination(city, tripDate);
        }
    }

    // Fetch weather in a background thread (for demo only, use proper async in prod!)
    private void fetchWeatherForDestination(String city, String date) {
        new Thread(() -> {
            try {
                // OpenWeatherMap 5-day/3-hour forecast API
                String urlString = "https://api.openweathermap.org/data/2.5/forecast?q=" +
                        URLEncoder.encode(city, "UTF-8") +
                        "&appid=" + WEATHER_API_KEY + "&units=metric";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                InputStream in = conn.getInputStream();
                String response = new Scanner(in).useDelimiter("\\A").next();
                JSONObject json = new JSONObject(response);
                JSONArray list = json.getJSONArray("list");

                // Find the forecast closest to the requested date
                WeatherForecast bestForecast = null;
                long minDiff = Long.MAX_VALUE;
                SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    String dt_txt = item.getString("dt_txt");
                    String forecastDay = dt_txt.substring(0, 10);
                    if (forecastDay.equals(date)) {
                        // Optionally, pick the time closest to noon for better accuracy
                        Date forecastDate = apiFormat.parse(dt_txt);
                        Date tripDay = dayFormat.parse(date);
                        long diff = Math.abs(forecastDate.getTime() - tripDay.getTime());
                        if (diff < minDiff) {
                            minDiff = diff;
                            String desc = item.getJSONArray("weather").getJSONObject(0).getString("description");
                            double temp = item.getJSONObject("main").getDouble("temp");
                            bestForecast = new WeatherForecast(city, date, desc, temp);
                        }
                    }
                }
                if (bestForecast != null) {
                    WeatherForecast finalBestForecast = bestForecast;
                    runOnUiThread(() -> {
                        weatherList.add(finalBestForecast);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() -> {
                        weatherList.add(new WeatherForecast(city, date, "No forecast available", 0));
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    weatherList.add(new WeatherForecast(city, date, "Error: " + e.getMessage(), 0));
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }
}