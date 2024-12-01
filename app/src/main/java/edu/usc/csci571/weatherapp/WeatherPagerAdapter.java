package edu.usc.csci571.weatherapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WeatherPagerAdapter extends FragmentStateAdapter {
    private final String latitude;
    private final String longitude;
    private final String locationInWords;

    public WeatherPagerAdapter(@NonNull FragmentActivity fragmentActivity, String latitude,
                               String longitude, String locationInWords) {
        super(fragmentActivity);
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationInWords = locationInWords;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        args.putString("latitude", latitude);
        args.putString("longitude", longitude);
        args.putString("locationInWords", locationInWords);

        switch (position) {
            case 0:
                fragment = new TodayFragment();
                fragment.setArguments(args);
                return fragment;
            case 1:
                fragment = new WeeklyFragment();
                fragment.setArguments(args);
                return fragment;
            case 2:
                fragment = new WeatherDataFragment();
                fragment.setArguments(args);
                return fragment;
            default:
                throw new IllegalStateException("Invalid position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}