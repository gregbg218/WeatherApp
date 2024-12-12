package edu.usc.csci571.weatherapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.usc.csci571.weatherapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String BASE_URL = "http://10.0.2.2:3001";
    private static int weatherApiCalls = 0;
    private static long lastResetTime = System.currentTimeMillis();
    private static final int API_CALL_LIMIT = 1000;
    private static final long RESET_INTERVAL = 24 * 60 * 60 * 1000;

    private ActivityMainBinding binding;
    private RequestQueue requestQueue;
    private String latitude;
    private String longitude;
    private String locationInWords;
    private boolean isDataLoaded = false;
    private ViewPager2 viewPager;
    private TabLayout tabDots;
    private JSONArray favoritesData;
    private TabLayout.OnTabSelectedListener currentTabListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_WeatherApp);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity onCreate started");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FloatingActionButton fab = findViewById(R.id.fab_favorite);
        fab.setVisibility(View.INVISIBLE);

        setupClickListeners();
        Log.d(TAG, "Activity created");
        requestQueue = Volley.newRequestQueue(this);
        getCurrentLocationData();
        setupFavoritesPager();
    }

    private void logWeatherApiCall(String endpoint, String source, String parameters) {
        weatherApiCalls++;
        long currentTime = System.currentTimeMillis();

        // Get stack trace for call origin
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callingMethod = stackTrace[3].getMethodName(); // Index 3 gets the calling method

        // Format timestamp for logging
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String timestamp = sdf.format(new Date(currentTime));

        // Log detailed API call information
        Log.d(TAG, String.format("Weather API Call [%s] {\n" +
                        "  Endpoint: %s\n" +
                        "  Source Method: %s\n" +
                        "  Called From: %s\n" +
                        "  Parameters: %s\n" +
                        "  Total Calls Today: %d/%d\n" +
                        "}",
                timestamp,
                endpoint,
                source,
                callingMethod,
                parameters,
                weatherApiCalls,
                API_CALL_LIMIT));

        // Check for reset
        if (currentTime - lastResetTime >= RESET_INTERVAL) {
            weatherApiCalls = 1;
            lastResetTime = currentTime;
            Log.i(TAG, "Weather API call counter reset. New day started.");
        }

        if (weatherApiCalls > API_CALL_LIMIT) {
            Log.w(TAG, "WARNING: Exceeded recommended daily API call limit!");
        }
    }



    private void updateFabVisibility() {
        FloatingActionButton fab = findViewById(R.id.fab_favorite);
        int currentPosition = tabDots.getSelectedTabPosition();
        fab.setVisibility(currentPosition == 0 ? View.INVISIBLE : View.VISIBLE);
    }



    private void setupFavoritesPager() {
        viewPager = findViewById(R.id.viewPager2);
        tabDots = findViewById(R.id.tabDots);

        // Track if this is initial setup
        boolean isInitialSetup = tabDots.getTabCount() == 0;

        // Clear existing tabs and listeners
        tabDots.removeAllTabs();
        if (currentTabListener != null) {
            tabDots.removeOnTabSelectedListener(currentTabListener);
        }

        String favoritesUrl = BASE_URL+"/api/favorites/list";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, favoritesUrl, null,
                response -> {
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONArray apiResponse = response.getJSONArray("data");
                            // Reverse the array to make newest entries appear last
                            JSONArray reversedData = new JSONArray();
                            for (int i = apiResponse.length() - 1; i >= 0; i--) {
                                reversedData.put(apiResponse.get(i));
                            }
                            favoritesData = reversedData;

                            Log.d(TAG, "Fetched favorites data: " + favoritesData.length() + " favorites");

                            // Clear tabs again to be safe
                            tabDots.removeAllTabs();

                            // Add current location tab first
                            tabDots.addTab(tabDots.newTab());

                            // Then add favorite tabs in reversed order
                            for (int i = 0; i < favoritesData.length(); i++) {
                                tabDots.addTab(tabDots.newTab());
                            }

                            currentTabListener = new TabLayout.OnTabSelectedListener() {
                                @Override
                                public void onTabSelected(TabLayout.Tab tab) {
                                    FloatingActionButton fab = findViewById(R.id.fab_favorite);
                                    int position = tab.getPosition();
                                    updateFabVisibility();

                                    // Skip data fetch if this is initial setup and we're on first tab
                                    if (isInitialSetup && position == 0) {
                                        Log.d(TAG, "Skipping initial data fetch as it's already loading");
                                        return;
                                    }

                                    // Cancel any pending requests
                                    requestQueue.cancelAll(TAG + "_geo");
                                    requestQueue.cancelAll(TAG + "_weather");

                                    if (position == 0) {
                                        Log.d(TAG, "Loading current location data");
                                        isDataLoaded = false;
                                        getCurrentLocationData();
                                    } else {
                                        try {
                                            JSONObject favorite = favoritesData.getJSONObject(position - 1);
                                            String city = favorite.getString("city");
                                            String state = favorite.getString("state");
                                            locationInWords = city + ", " + state;
                                            isDataLoaded = false;

                                            Log.d(TAG, "Loading favorite location: " + locationInWords);

                                            String encodedCity = URLEncoder.encode(city, "UTF-8");
                                            String encodedState = URLEncoder.encode(state.replace(", USA", ""), "UTF-8");
                                            String geocodingUrl = BASE_URL+"/api/geocoding/coordinates?address=" +
                                                    encodedCity + "," + encodedState;

                                            JsonObjectRequest geoRequest = new JsonObjectRequest(
                                                    Request.Method.GET, geocodingUrl, null,
                                                    geoResponse -> {
                                                        try {
                                                            if (geoResponse.getBoolean("success")) {
                                                                JSONObject data = geoResponse.getJSONObject("coordinates");
                                                                latitude = data.getString("latitude");
                                                                longitude = data.getString("longitude");
                                                                Log.d(TAG, "Got coordinates: " + latitude + "," + longitude);

                                                                // Only fetch weather if this is still the selected tab
                                                                if (tabDots.getSelectedTabPosition() == position) {
                                                                    isDataLoaded = false;
                                                                    fetchAllWeatherData();
                                                                }
                                                            }
                                                        } catch (JSONException e) {
                                                            Log.e(TAG, "Error parsing geocoding: " + e.getMessage() + " " + geocodingUrl);
                                                        }
                                                    },
                                                    error -> Log.e(TAG, "Geocoding error: " + error.getMessage())
                                            );

                                            geoRequest.setTag(TAG + "_geo");
                                            geoRequest.setRetryPolicy(new DefaultRetryPolicy(
                                                    15000,
                                                    3,
                                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                                            requestQueue.add(geoRequest);

                                        } catch (JSONException | UnsupportedEncodingException e) {
                                            Log.e(TAG, "Error processing favorite: " + e.getMessage());
                                        }
                                    }
                                }

                                @Override
                                public void onTabUnselected(TabLayout.Tab tab) {
                                    requestQueue.cancelAll(TAG + "_geo");
                                    requestQueue.cancelAll(TAG + "_weather");
                                }

                                @Override
                                public void onTabReselected(TabLayout.Tab tab) {
                                    // Handle tab reselection if needed
                                }
                            };

                            // Add the listener
                            tabDots.addOnTabSelectedListener(currentTabListener);

                            // If this isn't initial setup, trigger the current tab's data load
                            if (!isInitialSetup) {
                                int currentPosition = tabDots.getSelectedTabPosition();
                                TabLayout.Tab currentTab = tabDots.getTabAt(currentPosition);
                                if (currentTab != null) {
                                    currentTab.select();
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing favorites: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Error fetching favorites: " + error.getMessage()));

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setTag(TAG + "_favorites");
        requestQueue.add(request);
    }

    private void showLoading() {
        if (findViewById(R.id.progressBar) != null) {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
        if (findViewById(R.id.mainContentLayout) != null) {
            findViewById(R.id.mainContentLayout).setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (findViewById(R.id.progressBar) != null) {
            findViewById(R.id.progressBar).setVisibility(View.GONE);
        }
        if (findViewById(R.id.mainContentLayout) != null) {
            findViewById(R.id.mainContentLayout).setVisibility(View.VISIBLE);
        }
    }
    private void setupClickListeners() {
        // Search Icon Click Listener
        ImageView searchIcon = findViewById(R.id.imageView10);
        if (searchIcon != null) {
            Log.d(TAG, "Setting up search icon click listener");
            searchIcon.setVisibility(View.VISIBLE);
            searchIcon.setOnClickListener(v -> {
                Log.d(TAG, "Search icon clicked, launching SearchActivity");
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("locationInWords", locationInWords);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Search icon view not found!");
        }

        // Weather Card Click Listener
        findViewById(R.id.cardView1).setOnClickListener(v -> {
            Log.d(TAG, "Weather card clicked - Starting API call sequence");

            // Show loading state
            showLoading();

            // Prepare URLs for both API calls
            String forecastUrl = BASE_URL + "/api/weather/forecast" +
                    "?latitude=" + latitude +
                    "&longitude=" + longitude;

            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String dayWeatherUrl = BASE_URL + "/api/weather/day-weather" +
                    "?latitude=" + latitude +
                    "&longitude=" + longitude +
                    "&date=" + todayDate;

            Log.d(TAG, String.format("Making API calls {\n" +
                    "  Forecast URL: %s\n" +
                    "  Day Weather URL: %s\n" +
                    "  Latitude: %s\n" +
                    "  Longitude: %s\n" +
                    "}", forecastUrl, dayWeatherUrl, latitude, longitude));

            // Create requests for both APIs
            JsonObjectRequest forecastRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    forecastUrl,
                    null,
                    response -> {
                        Log.d(TAG, "Forecast API response received");

                        try {
                            if (response.has("success") && response.getBoolean("success")) {
                                JSONArray forecastData = response.getJSONArray("data");
                                Log.d(TAG, String.format("Successfully parsed forecast data {\n" +
                                        "  Number of days: %d\n" +
                                        "  Data: %s\n" +
                                        "}", forecastData.length(), forecastData.toString()));

                                // Now make the day weather request
                                JsonObjectRequest dayWeatherRequest = new JsonObjectRequest(
                                        Request.Method.GET,
                                        dayWeatherUrl,
                                        null,
                                        dayResponse -> {
                                            hideLoading(); // Hide loading after both requests complete
                                            Log.d(TAG, "Day weather API response received");

                                            try {
                                                if (dayResponse.has("success") && dayResponse.getBoolean("success")) {
                                                    JSONObject weatherData = dayResponse.getJSONObject("data");
                                                    Log.d(TAG, "Successfully parsed day weather data");

                                                    // Create and launch intent with both data sets
                                                    Intent intent = new Intent(MainActivity.this, WeatherDetailsActivity.class);
                                                    intent.putExtra("latitude", latitude);
                                                    intent.putExtra("longitude", longitude);
                                                    intent.putExtra("city_name", locationInWords);
                                                    intent.putExtra("forecast_data", forecastData.toString());
                                                    intent.putExtra("weather_data", weatherData.toString());

                                                    TextView tempView = findViewById(R.id.todayTemp);
                                                    String tempText = tempView.getText().toString();
                                                    int temperature = Integer.parseInt(tempText.replaceAll("[^0-9]", ""));
                                                    intent.putExtra("temperature", temperature);

                                                    Log.d(TAG, "Starting WeatherDetailsActivity with both forecast and weather data");
                                                    startActivity(intent);

                                                } else {
                                                    Log.e(TAG, "Day weather API returned success:false - " + dayResponse.toString());
                                                    showError("Error fetching weather data");
                                                }
                                            } catch (JSONException e) {
                                                Log.e(TAG, "Error parsing day weather response: " + e.getMessage(), e);
                                                showError("Error processing weather data");
                                            }
                                        },
                                        error -> {
                                            hideLoading();
                                            Log.e(TAG, "Day weather API error: " + error.getMessage());
                                            showError("Error fetching weather data");
                                        });

                                configureRequest(dayWeatherRequest);
                                requestQueue.add(dayWeatherRequest);

                            } else {
                                hideLoading();
                                Log.e(TAG, "Forecast API returned success:false - " + response.toString());
                                showError("Error fetching forecast data");
                            }
                        } catch (JSONException e) {
                            hideLoading();
                            Log.e(TAG, "Error parsing forecast response: " + e.getMessage(), e);
                            showError("Error processing weather data");
                        }
                    },
                    error -> {
                        hideLoading();
                        Log.e(TAG, "Forecast API error: " + error.getMessage());
                        showError("Error fetching weather data");
                    });

            configureRequest(forecastRequest);
            requestQueue.add(forecastRequest);
        });

        // Favorite Button Click Listener
        FloatingActionButton fab = findViewById(R.id.fab_favorite);
        fab.setOnClickListener(v -> {
            int selectedPosition = tabDots.getSelectedTabPosition();
            if (selectedPosition > 0) {
                try {
                    JSONObject favorite = favoritesData.getJSONObject(selectedPosition - 1);
                    String city = favorite.getString("city");
                    String state = favorite.getString("state");
                    Log.d(TAG, "Removing favorite: " + city + ", " + state);
                    removeFavorite(city, state);
                } catch (JSONException e) {
                    Log.e(TAG, "Error getting favorite data: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error removing favorite", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Cannot remove current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configureRequest(JsonObjectRequest request) {
        Log.d(TAG, "Configuring request with timeout=15000ms, retries=3");
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }


    private void setupTabListener(JSONArray favorites) {
        tabDots.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                isDataLoaded = false;

                if (position == 0) {
                    // Current location tab
                    getCurrentLocationData();
                } else {
                    try {
                        // Get the corresponding favorite
                        JSONObject favorite = favorites.getJSONObject(position - 1);
                        String city = favorite.getString("city");
                        String state = favorite.getString("state");
                        locationInWords = city + ", " + state;

                        // Get coordinates for the city through geocoding
                        getCoordinatesAndUpdateWeather(city, state);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error accessing favorite: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void getCoordinatesAndUpdateWeather(String city, String state) {
        String geocodingUrl = null;
        try {
            geocodingUrl = BASE_URL+"/api/geocoding/coordinates?address=" + URLEncoder.encode(city + "," + state, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, geocodingUrl, null,
                response -> {
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("coordinates");
                            latitude = data.getString("latitude");
                            longitude = data.getString("longitude");

                            // Fetch weather data with new coordinates
                            fetchAllWeatherData();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing geocoding response: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Error getting coordinates: " + error.getMessage()));

        requestQueue.add(request);
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void getCurrentLocationData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            fallbackToLosAngeles();
            return;
        }

        // Show loading state while getting location
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.mainContentLayout).setVisibility(View.GONE);

        String ipinfoUrl = "https://ipinfo.io/json?token=b2ac4982de4968";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ipinfoUrl, null,
                response -> {
                    try {
                        // Select the first tab (current location)
                        tabDots.selectTab(tabDots.getTabAt(0));

                        String city = response.getString("city");
                        String region = response.getString("region");
                        this.locationInWords = city + ", " + region;
                        String loc = response.getString("loc");

                        String[] coordinates = loc.split(",");
                        this.latitude = coordinates[0];
                        this.longitude = coordinates[1];

                        if (!isDataLoaded) {
                            fetchAllWeatherData();
                        }

                        // Hide loading state after data is loaded
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.mainContentLayout).setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing location data: " + e.getMessage());
                        fallbackToLosAngeles();
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.mainContentLayout).setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching location: " + error.getMessage());
                    fallbackToLosAngeles();
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    findViewById(R.id.mainContentLayout).setVisibility(View.VISIBLE);
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        request.setTag(TAG);
        requestQueue.add(request);
    }

    private void fallbackToLosAngeles() {
        // Add this line at the start
        tabDots.selectTab(tabDots.getTabAt(0));

        this.latitude = "34.0224";
        this.longitude = "-118.2851";
        this.locationInWords = "Los Angeles, California";
        if (!isDataLoaded) {
            fetchAllWeatherData();
        }
    }

    private void removeFavorite(String city, String state) {
        try {
            String encodedCity = URLEncoder.encode(city, "UTF-8");
            String encodedState = URLEncoder.encode(state, "UTF-8");
            String deleteUrl = String.format(BASE_URL+"/api/favorites/remove?city=%s&state=%s",
                    encodedCity, encodedState);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.DELETE,
                    deleteUrl,
                    null,
                    response -> {
                        try {
                            if (response.getBoolean("success")) {
                                // First, switch to current location tab
                                runOnUiThread(() -> {
                                    // Force selection of first tab (current location)
                                    if (tabDots.getTabCount() > 0) {
                                        TabLayout.Tab firstTab = tabDots.getTabAt(0);
                                        if (firstTab != null) {
                                            // Clear data before tab switch to prevent old data display
                                            isDataLoaded = false;
                                            firstTab.select();
                                            // Immediately load current location data
                                            getCurrentLocationData();
                                        }
                                    }

                                    // After ensuring we're on the current location tab, clean up the old data
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        // Clear existing tabs and listeners
                                        tabDots.clearOnTabSelectedListeners();
                                        tabDots.removeAllTabs();

                                        // Reset ViewPager
                                        if (viewPager != null) {
                                            viewPager.setAdapter(null);
                                        }

                                        // Clear favorites data
                                        favoritesData = null;

                                        // Now rebuild the pager with fresh data
                                        setupFavoritesPager();
                                    });
                                });

                                Toast.makeText(MainActivity.this,
                                        city + ", " + state + " removed from favorites",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Failed to remove from favorites",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing remove response: " + e.getMessage());
                            Toast.makeText(MainActivity.this,
                                    "Error removing from favorites",
                                    Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error removing favorite: " +
                                (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                        Toast.makeText(MainActivity.this,
                                "Error removing from favorites",
                                Toast.LENGTH_SHORT).show();
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000,
                    3,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding URL: " + e.getMessage());
            Toast.makeText(MainActivity.this,
                    "Error removing from favorites",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchAllWeatherData() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String todaysDate = formatter.format(new Date());

        // Log the state when fetchAllWeatherData is called
        Log.d(TAG, String.format("fetchAllWeatherData called {\n" +
                        "  isDataLoaded: %b\n" +
                        "  latitude: %s\n" +
                        "  longitude: %s\n" +
                        "  locationInWords: %s\n" +
                        "}",
                isDataLoaded,
                latitude,
                longitude,
                locationInWords));

        String forecastUrl = BASE_URL+"/api/weather/forecast?latitude=" + this.latitude +
                "&longitude=" + this.longitude;
        String todayUrl = BASE_URL+"/api/weather/day-weather?latitude=" + this.latitude +
                "&longitude=" + this.longitude + "&date=" + todaysDate;

        JsonObjectRequest forecastRequest = new JsonObjectRequest(Request.Method.GET, forecastUrl, null,
                response -> {
                    logWeatherApiCall("forecast", "fetchAllWeatherData",
                            String.format("lat=%s,lon=%s", latitude, longitude));
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONArray dailyData = response.getJSONArray("data");
                            populateWeatherTable(dailyData);
                        } else {
                            showError("Invalid weather data format");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("Error parsing weather data");
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                    showError("Error fetching weather data");
                });

        JsonObjectRequest todayRequest = new JsonObjectRequest(Request.Method.GET, todayUrl, null,
                response -> {
                    logWeatherApiCall("day-weather", "fetchAllWeatherData",
                            String.format("lat=%s,lon=%s,date=%s", latitude, longitude, todaysDate));
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            updateTodayWeather(data);
                        } else {
                            showError("Invalid weather data format");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("Error parsing weather data");
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                    showError("Error fetching weather data");
                });

        forecastRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        todayRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(forecastRequest);
        requestQueue.add(todayRequest);

        isDataLoaded = true;
    }

    private void updateTodayWeather(JSONObject data) throws JSONException {
        Log.d(TAG, "Updating today's weather with data: " + data);

        // Update Card 1
        ImageView todayImageView = findViewById(R.id.todayImage);
        TextView todayTempView = findViewById(R.id.todayTemp);
        TextView todayDescView = findViewById(R.id.todayDesc);
        TextView locationTextView = findViewById(R.id.locationInWords);

        String weatherDescription = data.getString("status");
        todayImageView.setImageResource(getWeatherIconFromDescription(weatherDescription));
        double temperature = Double.parseDouble(data.getString("temperature"));
        todayTempView.setText(Math.round(temperature) + "°F");
        todayDescView.setText(weatherDescription);
        locationTextView.setText(locationInWords);

        // Update Card 2
        TextView humidityView = findViewById(R.id.Humidity_Value);
        TextView windSpeedView = findViewById(R.id.Wind_Speed_Value);
        TextView visibilityView = findViewById(R.id.Visibility_Value);
        TextView pressureView = findViewById(R.id.Pressure_Value);

        if (humidityView != null && windSpeedView != null && visibilityView != null && pressureView != null) {
            double humidity = Double.parseDouble(data.getString("humidity"));
            humidityView.setText(String.format("%.0f%%", humidity));

            double windSpeed = Double.parseDouble(data.getString("windSpeed"));
            windSpeedView.setText(String.format("%.2f mph", windSpeed));
            windSpeedView.setTextColor(Color.parseColor("#BEBEBE"));

            double visibility = Double.parseDouble(data.getString("visibility"));
            visibilityView.setText(String.format("%.2f mi", visibility));

            double pressure = Double.parseDouble(data.getString("pressureSeaLevel"));
            pressureView.setText(String.format("%.2f inHg", pressure));
        }
    }





    private void populateWeatherTable(JSONArray dailyData) throws JSONException {
        TableLayout tableLayout = findViewById(R.id.weatherTable);
        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
        );
        tableLayout.setLayoutParams(tableParams);
        tableLayout.setStretchAllColumns(true);

        if (tableLayout == null) {
            return;
        }

        tableLayout.removeAllViews();

        for (int i = 0; i < dailyData.length(); i++) {
            JSONObject dayData = dailyData.getJSONObject(i);
            TableRow row = new TableRow(this);
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
            );
            row.setLayoutParams(rowParams);

            TextView dateView = new TextView(this);
            String dateString = dayData.getString("date");
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = inputFormat.parse(dateString);
                String formattedDate = outputFormat.format(date);
                dateView.setText(formattedDate);
            } catch (ParseException e) {
                dateView.setText(dateString);
            }
            dateView.setTextColor(Color.parseColor("#BEBEBE"));
            dateView.setPadding(16, 0, 16, 0);
            dateView.setGravity(Gravity.CENTER);
            row.addView(dateView);

            ImageView weatherIcon = new ImageView(this);
            String weatherCode = String.valueOf(dayData.getInt("status"));
            weatherIcon.setImageResource(getWeatherIconFromCode(weatherCode));
            TableRow.LayoutParams iconParams = new TableRow.LayoutParams(
                    dpToPx(35),
                    dpToPx(35)
            );
            iconParams.gravity = Gravity.CENTER;
            weatherIcon.setLayoutParams(iconParams);
            weatherIcon.setPadding(16, 0, 16, 0);
            row.addView(weatherIcon);

            TextView lowTemp = new TextView(this);
            double tempLow = Double.parseDouble(dayData.getString("tempLow"));
            lowTemp.setText(String.valueOf(Math.round(tempLow)));
            lowTemp.setTextColor(Color.parseColor("#BEBEBE"));
            lowTemp.setPadding(16, 0, 16, 0);
            lowTemp.setGravity(Gravity.CENTER);
            row.addView(lowTemp);

            TextView highTemp = new TextView(this);
            double tempHigh = Double.parseDouble(dayData.getString("tempHigh"));
            highTemp.setText(String.valueOf(Math.round(tempHigh)));
            highTemp.setTextColor(Color.parseColor("#BEBEBE"));
            highTemp.setPadding(16, 0, 16, 0);
            highTemp.setGravity(Gravity.CENTER);
            row.addView(highTemp);

            tableLayout.addView(row);

            if (i < dailyData.length() - 1) {
                View divider = new View(this);
                TableLayout.LayoutParams dividerParams = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        1
                );
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.parseColor("#3A3A3A"));
                tableLayout.addView(divider);
            }
        }
    }

    private int getWeatherIconFromCode(String input) {
        switch (input) {
            case "1000": return R.drawable.clear_day;
            case "1100": return R.drawable.mostly_clear_day;
            case "1101": return R.drawable.partly_cloudy_day;
            case "1102": return R.drawable.mostly_cloudy;
            case "1001": return R.drawable.cloudy;
            case "2000": return R.drawable.fog;
            case "2100": return R.drawable.fog_light;
            case "4000": return R.drawable.drizzle;
            case "4001": return R.drawable.rain;
            case "4200": return R.drawable.rain_light;
            case "4201": return R.drawable.rain_heavy;
            case "5000": return R.drawable.snow;
            case "5001": return R.drawable.flurries;
            case "5100": return R.drawable.snow_light;
            case "5101": return R.drawable.snow_heavy;
            case "6000": return R.drawable.freezing_drizzle;
            case "6001": return R.drawable.freezing_rain;
            case "6200": return R.drawable.freezing_rain_light;
            case "6201": return R.drawable.freezing_rain_heavy;
            case "7000": return R.drawable.ice_pellets;
            case "7101": return R.drawable.ice_pellets_heavy;
            case "7102": return R.drawable.ice_pellets_light;
            case "8000": return R.drawable.tstorm;
            default: return R.drawable.clear_day;
        }
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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupFavoritesPager();  // Add this line to refresh the favorites
        if (latitude == null || longitude == null) {
            getCurrentLocationData();
        }
    }
}