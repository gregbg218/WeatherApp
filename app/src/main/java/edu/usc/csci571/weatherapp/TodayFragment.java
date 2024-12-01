package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodayFragment extends Fragment {
    private static final String TAG = "TodayFragment";
    private View view;
    private RequestQueue requestQueue;
    private TextView windSpeedValue, pressureValue, precipitationValue, temperatureValue;
    private TextView humidityValue, visibilityValue, cloudCoverValue, uvIndexValue, weatherStatus;
    private ImageView weatherIcon;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_today, container, false);
        initializeViews();
        requestQueue = Volley.newRequestQueue(requireContext());
        fetchTodayWeatherData();
        return view;
    }

    private void initializeViews() {
        windSpeedValue = view.findViewById(R.id.windSpeedValue);
        pressureValue = view.findViewById(R.id.pressureValue);
        precipitationValue = view.findViewById(R.id.precipitationValue);
        temperatureValue = view.findViewById(R.id.temperatureValue);
        humidityValue = view.findViewById(R.id.humidityValue);
        visibilityValue = view.findViewById(R.id.visibilityValue);
        cloudCoverValue = view.findViewById(R.id.cloudCoverValue);
        uvIndexValue = view.findViewById(R.id.uvIndexValue);
        weatherStatus = view.findViewById(R.id.weatherStatus);
        weatherIcon = view.findViewById(R.id.weatherIcon);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void fetchTodayWeatherData() {
        showLoading();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todaysDate = formatter.format(new Date());

        Bundle args = getArguments();
        if (args == null) {
            Log.e(TAG, "Arguments bundle is null");
            showError("Error: Unable to fetch weather data");
            return;
        }

        String latitude = args.getString("latitude");
        String longitude = args.getString("longitude");

        if (latitude == null || longitude == null) {
            Log.e(TAG, "Latitude or longitude is null");
            showError("Error: Invalid location data");
            return;
        }

        Log.d(TAG, "Fetching weather data for lat: " + latitude + ", lon: " + longitude + ", date: " + todaysDate);
        String url = "http://10.0.2.2:3001/api/weather/day-weather" +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&date=" + todaysDate;
        Log.d(TAG, "API URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    hideLoading();
                    Log.d(TAG, "API Response: " + response.toString());
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            Log.d(TAG, "Weather data: " + data.toString());
                            updateUI(data);
                        } else {
                            Log.e(TAG, "API returned success: false");
                            showError("Invalid response from server");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing data: " + e.getMessage());
                        showError("Error parsing weather data");
                    }
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "Error fetching data: " + error.getMessage());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error Status Code: " + error.networkResponse.statusCode);
                    }
                    showError("Error fetching weather data");
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void updateUI(final JSONObject data) {
        if (getActivity() == null) {
            Log.e(TAG, "Activity is null, cannot update UI");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                // Wind Speed
                Log.e(TAG, "Wind is "+windSpeedValue);
                double windSpeed = data.getDouble("windSpeed");
                windSpeedValue.setText(String.format(Locale.US, "%.2f mph", windSpeed));

                // Pressure
                double pressure = data.getDouble("pressureSeaLevel");
                pressureValue.setText(String.format(Locale.US, "%.2f inHg", pressure));

                // Precipitation
                double precipitation = data.getDouble("precipitationIntensity");
                precipitationValue.setText(String.format(Locale.US, "%.0f%%", precipitation));

                // Temperature
                double temperature = data.getDouble("temperature");
                temperatureValue.setText(String.format(Locale.US, "%d°F", Math.round(temperature)));

                // Humidity
                double humidity = data.getDouble("humidity");
                humidityValue.setText(String.format(Locale.US, "%.0f%%", humidity * 100));

                // Visibility
                double visibility = data.getDouble("visibility");
                visibilityValue.setText(String.format(Locale.US, "%.2f mi", visibility));

                // Cloud Cover
                double cloudCover = data.getDouble("cloudCover");
                cloudCoverValue.setText(String.format(Locale.US, "%.0f%%", cloudCover * 100));

                // UV Index
                int uvIndex = data.getInt("uvIndex");
                uvIndexValue.setText(String.valueOf(uvIndex));

                // Weather Status and Icon
                String status = data.getString("status");
                weatherStatus.setText(status);
                weatherIcon.setImageResource(getWeatherIconFromDescription(status));

                Log.d(TAG, "UI updated successfully");
            } catch (JSONException e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage());
                showError("Error updating weather information");
            }
        });
    }

    private int getWeatherIconFromDescription(String description) {
        switch (description.toLowerCase()) {
            case "clear": return R.drawable.clear_day;
            case "mostly clear": return R.drawable.mostly_clear_day;
            case "partly cloudy": return R.drawable.partly_cloudy_day;
            case "mostly cloudy": return R.drawable.mostly_cloudy;
            case "cloudy": return R.drawable.cloudy;
            case "fog": return R.drawable.fog;
            case "light fog": return R.drawable.fog_light;
            case "drizzle": return R.drawable.drizzle;
            case "rain": return R.drawable.rain;
            case "light rain": return R.drawable.rain_light;
            case "heavy rain": return R.drawable.rain_heavy;
            case "snow": return R.drawable.snow;
            case "flurries": return R.drawable.flurries;
            case "light snow": return R.drawable.snow_light;
            case "heavy snow": return R.drawable.snow_heavy;
            case "freezing drizzle": return R.drawable.freezing_drizzle;
            case "freezing rain": return R.drawable.freezing_rain;
            case "light freezing rain": return R.drawable.freezing_rain_light;
            case "heavy freezing rain": return R.drawable.freezing_rain_heavy;
            case "ice pellets": return R.drawable.ice_pellets;
            case "heavy ice pellets": return R.drawable.ice_pellets_heavy;
            case "light ice pellets": return R.drawable.ice_pellets_light;
            case "thunderstorm": return R.drawable.tstorm;
            default: return R.drawable.clear_day;
        }
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}