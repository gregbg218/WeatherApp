package edu.usc.csci571.weatherapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class WeatherDetailsActivity extends AppCompatActivity {
    private static final String TAG = "WeatherDetailsActivity";
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private String latitude;
    private String longitude;
    private String cityName;
    private int temperature;




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

        // Pass location data to adapter
        WeatherPagerAdapter adapter = new WeatherPagerAdapter(this, latitude, longitude, cityName);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);  // Prevent recreation of fragments

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
    }

    private void shareOnTwitter() {
        String tweetText = "Check Out " + cityName + "'s Weather! It is " + temperature + "°F! #CSCI571WeatherSearch";
        String tweetUrl = "https://twitter.com/intent/tweet?text=" + Uri.encode(tweetText);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
        startActivity(intent);
    }
}