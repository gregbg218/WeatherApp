package edu.usc.csci571.weatherapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WeatherPagerAdapter extends FragmentStateAdapter {
    private final String latitude;
    private final String longitude;

    public WeatherPagerAdapter(FragmentActivity fragmentActivity, String latitude, String longitude) {
        super(fragmentActivity);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        args.putString("latitude", latitude);
        args.putString("longitude", longitude);

        switch (position) {
            case 0:
                fragment = new TodayFragment();
                break;
            case 1:
                fragment = new WeeklyFragment();
                break;
            case 2:
                fragment = new WeatherDataFragment();
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