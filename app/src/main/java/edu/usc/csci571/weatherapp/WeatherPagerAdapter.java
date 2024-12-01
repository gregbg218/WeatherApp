package edu.usc.csci571.weatherapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WeatherPagerAdapter extends FragmentStateAdapter {
    public WeatherPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TodayFragment();
            case 1:
                return new WeeklyFragment();
            case 2:
                return new WeatherDataFragment();
            default:
                throw new IllegalStateException("Invalid position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}