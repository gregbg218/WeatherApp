package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "WeatherPagerAdapter";
    private final String latitude;
    private final String longitude;
    private JSONArray forecastData;
    private JSONObject weatherData;

    public WeatherPagerAdapter(FragmentActivity fragmentActivity, String latitude, String longitude) {
        super(fragmentActivity);
        this.latitude = latitude;
        this.longitude = longitude;
        Log.d(TAG, String.format("WeatherPagerAdapter initialized with lat=%s, lon=%s", latitude, longitude));
    }

    public void setForecastData(JSONArray data) {
        this.forecastData = data;
        Log.d(TAG, "Forecast data set with " + (data != null ? data.length() : 0) + " days");
        notifyDataSetChanged();
    }

    public void setWeatherData(JSONObject data) {
        this.weatherData = data;
        Log.d(TAG, "Weather data set");
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        args.putString("latitude", latitude);
        args.putString("longitude", longitude);

        if (forecastData != null) {
            args.putString("forecast_data", forecastData.toString());
            Log.d(TAG, "Adding forecast data to fragment at position " + position);
        }

        if (weatherData != null) {
            args.putString("weather_data", weatherData.toString());
            Log.d(TAG, "Adding weather data to fragment at position " + position);
        }

        switch (position) {
            case 0:
                fragment = new TodayFragment();
                Log.d(TAG, "Creating TodayFragment");
                break;
            case 1:
                fragment = new WeeklyFragment();
                Log.d(TAG, "Creating WeeklyFragment");
                break;
            case 2:
                fragment = new WeatherDataFragment();
                Log.d(TAG, "Creating WeatherDataFragment");
                break;
            default:
                throw new IllegalStateException("Invalid position " + position);
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}