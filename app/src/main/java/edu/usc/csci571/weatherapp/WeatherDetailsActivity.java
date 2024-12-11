package edu.usc.csci571.weatherapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONException;

public class WeatherDetailsActivity extends AppCompatActivity {
    private static final String TAG = "WeatherDetailsActivity";

    private static final String BASE_URL = "http://10.0.2.2:3001";
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private String latitude;
    private String longitude;
    private String cityName;
    private int temperature;
    private WeatherPagerAdapter adapter;
    private JSONArray forecastData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_details);

        // Get data from intent
        Intent intent = getIntent();
        latitude = intent.getStringExtra("latitude");
        longitude = intent.getStringExtra("longitude");
        cityName = intent.getStringExtra("city_name");
        temperature = intent.getIntExtra("temperature", 0);

        Log.d(TAG, String.format("Received data in WeatherDetailsActivity {\n" +
                "  Latitude: %s\n" +
                "  Longitude: %s\n" +
                "  City: %s\n" +
                "  Temperature: %d\n" +
                "}", latitude, longitude, cityName, temperature));

        // Parse forecast data if available
        String forecastDataStr = intent.getStringExtra("forecast_data");
        if (forecastDataStr != null) {
            try {
                forecastData = new JSONArray(forecastDataStr);
                Log.d(TAG, "Successfully parsed forecast data with " + forecastData.length() + " days");
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing forecast data: " + e.getMessage());
                Toast.makeText(this, "Error loading forecast data", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "No forecast data received in intent");
        }

        // Set city name in title
        TextView cityTitle = findViewById(R.id.cityTitle);
        cityTitle.setText(cityName);

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Setup tweet button
        ImageButton tweetButton = findViewById(R.id.tweetButton);
        tweetButton.setOnClickListener(v -> shareOnTwitter());

        // Setup ViewPager and TabLayout
        setupViewPager();
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        adapter = new WeatherPagerAdapter(this, latitude, longitude);

        // Set forecast data if available
        if (forecastData != null) {
            Log.d(TAG, "Setting forecast data in adapter");
            adapter.setForecastData(forecastData);
        }

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("TODAY");
                    tab.setIcon(R.drawable.today);
                    break;
                case 1:
                    tab.setText("WEEKLY");
                    tab.setIcon(R.drawable.weekly_tab);
                    break;
                case 2:
                    tab.setText("WEATHER DATA");
                    tab.setIcon(R.drawable.ic_thermometer);
                    break;
            }
        }).attach();

        // Log the setup completion
        Log.d(TAG, "ViewPager setup completed with " + adapter.getItemCount() + " pages");
    }

    private void shareOnTwitter() {
        String tweetText = "Check Out " + cityName + "'s Weather! It is " + temperature + "°F! #CSCI571WeatherSearch";
        String tweetUrl = "https://twitter.com/intent/tweet?text=" + Uri.encode(tweetText);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }
    }
}