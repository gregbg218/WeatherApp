package edu.usc.csci571.weatherapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private static final String BASE_URL = "http://10.0.2.2:3001";

    private AutoCompleteTextView searchAutoComplete;
    private RequestQueue requestQueue;
    private List<String> suggestions;
    private ArrayAdapter<String> adapter;
    private View progressBar;
    private FloatingActionButton fabFavorite;

    private String selectedCity;
    private String selectedState;
    private String latitude;
    private String longitude;
    private String locationInWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Retrieve passed coordinates and location
        Intent intent = getIntent();
        latitude = intent.getStringExtra("latitude");
        longitude = intent.getStringExtra("longitude");
        locationInWords = intent.getStringExtra("locationInWords");

        // Initialize views
        searchAutoComplete = findViewById(R.id.search_autocomplete);
        progressBar = findViewById(R.id.progressBar);
        fabFavorite = findViewById(R.id.fab_favorite);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize AutoCompleteTextView with custom adapter
        suggestions = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, suggestions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                text.setPadding(32, 16, 32, 16);
                return view;
            }
        };
        searchAutoComplete.setAdapter(adapter);
        searchAutoComplete.setThreshold(3);
        searchAutoComplete.setTextColor(Color.WHITE);
        searchAutoComplete.setHintTextColor(Color.GRAY);

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Setup AutoComplete
        setupAutoComplete();

        // Setup click listeners for card and favorite button
        setupCardClickListener();
        setupFavoriteButton();

        // Fetch initial weather data
        if (latitude != null && longitude != null) {
            fetchWeatherData();
            fillHomePageCard2();
        }
    }


    private void initializeViews() {
        searchAutoComplete = findViewById(R.id.search_autocomplete);
        progressBar = findViewById(R.id.progressBar);
        fabFavorite = findViewById(R.id.fab_favorite);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        suggestions = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                text.setPadding(32, 16, 32, 16);
                return view;
            }
        };
        searchAutoComplete.setAdapter(adapter);
        searchAutoComplete.setThreshold(3);
        searchAutoComplete.setTextColor(Color.WHITE);
        searchAutoComplete.setHintTextColor(Color.GRAY);

        requestQueue = Volley.newRequestQueue(this);
    }

    private void setupAutoComplete() {
        searchAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (text.length() >= 3) {
                    getAutocompleteSuggestions(text);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            searchAutoComplete.setText(selection);
            searchAutoComplete.dismissDropDown();
            showLoading();
            performSearch(selection);
        });
    }

    private void setupCardClickListener() {
        View cardView = findViewById(R.id.cardView1);
        cardView.setOnClickListener(v -> {
            if (selectedCity != null && selectedState != null) {
                // TODO: Launch details activity
                Toast.makeText(this, "Opening details...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFavoriteButton() {
        fabFavorite.setOnClickListener(v -> {
            if (selectedCity != null && selectedState != null) {
                toggleFavorite();
            }
        });
    }

    private void getAutocompleteSuggestions(String text) {
        String url = BASE_URL + "/api/autocomplete/suggestions?input=" + Uri.encode(text);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray predictions = response.getJSONArray("predictions");
                        suggestions.clear();
                        for (int i = 0; i < predictions.length(); i++) {
                            JSONObject prediction = predictions.getJSONObject(i);
                            suggestions.add(prediction.getString("description"));
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, suggestions) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView text = (TextView) view.findViewById(android.R.id.text1);
                                text.setTextColor(Color.WHITE);
                                return view;
                            }
                        };
                        searchAutoComplete.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error fetching suggestions", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void performSearch(String query) {
        showLoading();
        // First get the place details from autocomplete
        String autocompleteUrl = BASE_URL + "/api/autocomplete/suggestions?input=" + Uri.encode(query);
        JsonObjectRequest autocompleteRequest = new JsonObjectRequest(Request.Method.GET, autocompleteUrl, null,
                response -> {
                    try {
                        JSONArray predictions = response.getJSONArray("predictions");
                        if (predictions.length() > 0) {
                            JSONObject prediction = predictions.getJSONObject(0);
                            JSONObject structuredFormatting = prediction.getJSONObject("structured_formatting");
                            selectedCity = structuredFormatting.getString("main_text");
                            selectedState = structuredFormatting.getString("secondary_text");
                            locationInWords = prediction.getString("description");

                            // Now get coordinates using geocoding API
                            String geocodingUrl = BASE_URL + "/api/geocoding/coordinates?address=" + Uri.encode(locationInWords);
                            JsonObjectRequest geocodingRequest = new JsonObjectRequest(Request.Method.GET, geocodingUrl, null,
                                    geoResponse -> {
                                        try {
                                            if (geoResponse.getBoolean("success")) {
                                                JSONObject coordinates = geoResponse.getJSONObject("coordinates");
                                                latitude = coordinates.getString("latitude");
                                                longitude = coordinates.getString("longitude");

                                                // Now fetch weather data with new coordinates
                                                fetchWeatherData();
                                                fillHomePageCard2();
                                                checkFavoriteStatus();
                                            } else {
                                                showError("Could not get location coordinates");
                                            }
                                        } catch (JSONException e) {
                                            showError("Error parsing geocoding data");
                                        }
                                    },
                                    error -> showError("Error getting coordinates")
                            );
                            requestQueue.add(geocodingRequest);
                        } else {
                            showError("No results found");
                        }
                    } catch (JSONException e) {
                        showError("Error parsing location data");
                    }
                },
                error -> showError("Error fetching location data")
        );
        requestQueue.add(autocompleteRequest);
    }

    private void fetchWeatherData() {
        String url = BASE_URL + "/api/weather/forecast?latitude=" + latitude + "&longitude=" + longitude;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    hideLoading();
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONArray dailyData = response.getJSONArray("data");
                            populateWeatherTable(dailyData);
                        } else {
                            showError("Invalid weather data format");
                        }
                    } catch (JSONException e) {
                        showError("Error parsing weather data");
                    }
                },
                error -> {
                    hideLoading();
                    showError("Error fetching weather data");
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void fillHomePageCard2() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String todaysDate = formatter.format(new Date());

        String url = BASE_URL + "/api/weather/day-weather?latitude=" + latitude +
                "&longitude=" + longitude + "&date=" + todaysDate;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            updateWeatherCard(data);
                        } else {
                            showError("Invalid weather data format");
                        }
                    } catch (JSONException e) {
                        showError("Error parsing weather data");
                    }
                },
                error -> showError("Error fetching weather data"));

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void updateWeatherCard(JSONObject data) throws JSONException {
        ImageView todayImageView = findViewById(R.id.todayImage);
        TextView todayTempView = findViewById(R.id.todayTemp);
        TextView todayDescView = findViewById(R.id.todayDesc);
        TextView locationTextView = findViewById(R.id.locationInWords);

        String weatherDescription = data.optString("status", "");
        todayImageView.setImageResource(getWeatherIconFromDescription(weatherDescription));

        double temperature = data.optDouble("temperature", 0);
        todayTempView.setText(Math.round(temperature) + "°F");
        todayDescView.setText(weatherDescription);
        locationTextView.setText(locationInWords);

        TextView humidityView = findViewById(R.id.Humidity_Value);
        TextView windSpeedView = findViewById(R.id.Wind_Speed_Value);
        TextView visibilityView = findViewById(R.id.Visibility_Value);
        TextView pressureView = findViewById(R.id.Pressure_Value);

        double humidity = data.optDouble("humidity", 0);
        humidityView.setText(String.format("%.0f%%", humidity * 100));

        double windSpeed = data.optDouble("windSpeed", 0);
        windSpeedView.setText(String.format("%.2f mph", windSpeed));

        double visibility = data.optDouble("visibility", 0);
        visibilityView.setText(String.format("%.2f mi", visibility));

        double pressure = data.optDouble("pressureSeaLevel", 0);
        pressureView.setText(String.format("%.2f inHg", pressure));
    }

    private void populateWeatherTable(JSONArray dailyData) throws JSONException {
        TableLayout tableLayout = findViewById(R.id.weatherTable);
        if (tableLayout == null) return;

        tableLayout.removeAllViews();
        tableLayout.setStretchAllColumns(true);

        for (int i = 0; i < dailyData.length(); i++) {
            JSONObject dayData = dailyData.getJSONObject(i);

            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
            ));

            // Add date
            TextView dateView = new TextView(this);
            String dateString = dayData.getString("date");
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = inputFormat.parse(dateString);
                dateView.setText(outputFormat.format(date));
            } catch (ParseException e) {
                dateView.setText(dateString);
            }
            dateView.setTextColor(Color.parseColor("#BEBEBE"));
            dateView.setGravity(Gravity.CENTER);
            row.addView(dateView);

            // Add weather icon
            ImageView weatherIcon = new ImageView(this);
            weatherIcon.setImageResource(getWeatherIconFromCode(String.valueOf(dayData.getInt("status"))));
            weatherIcon.setLayoutParams(new TableRow.LayoutParams(dpToPx(35), dpToPx(35)));
            row.addView(weatherIcon);

            // Add temperatures
            TextView lowTemp = new TextView(this);
            lowTemp.setText(String.valueOf(Math.round(Double.parseDouble(dayData.getString("tempLow")))));
            lowTemp.setTextColor(Color.parseColor("#BEBEBE"));
            lowTemp.setGravity(Gravity.CENTER);
            row.addView(lowTemp);

            TextView highTemp = new TextView(this);
            highTemp.setText(String.valueOf(Math.round(Double.parseDouble(dayData.getString("tempHigh")))));
            highTemp.setTextColor(Color.parseColor("#BEBEBE"));
            highTemp.setGravity(Gravity.CENTER);
            row.addView(highTemp);

            tableLayout.addView(row);

            // Add divider
            if (i < dailyData.length() - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        1
                ));
                divider.setBackgroundColor(Color.parseColor("#3A3A3A"));
                tableLayout.addView(divider);
            }
        }
    }

    private void checkFavoriteStatus() {
        String url = BASE_URL + "/api/favorites/status?city=" + selectedCity + "&state=" + selectedState;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean isFavorite = response.getBoolean("isFavorite");
                        updateFavoriteButton(isFavorite);
                    } catch (JSONException e) {
                        showError("Error checking favorite status");
                    }
                },
                error -> showError("Error checking favorite status"));

        requestQueue.add(request);
    }

    private void toggleFavorite() {
        if (fabFavorite.getTag() != null && (boolean) fabFavorite.getTag()) {
            removeFavorite();
        } else {
            addFavorite();
        }
    }

    private void addFavorite() {
        String url = BASE_URL + "/api/favorites/add";
        JSONObject body = new JSONObject();
        try {
            body.put("city", selectedCity);
            body.put("state", selectedState);
        } catch (JSONException e) {
            showError("Error adding to favorites");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    updateFavoriteButton(true);
                    Toast.makeText(this, selectedCity + " was added to favorites", Toast.LENGTH_SHORT).show();
                },
                error -> showError("Error adding to favorites"));

        requestQueue.add(request);
    }

    private void removeFavorite() {
        String url = BASE_URL + "/api/favorites/remove?city=" + selectedCity + "&state=" + selectedState;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> {
                    updateFavoriteButton(false);
                    Toast.makeText(this, selectedCity + " was removed from favorites", Toast.LENGTH_SHORT).show();
                },
                error -> showError("Error removing from favorites"));

        requestQueue.add(request);
    }

    private void updateFavoriteButton(boolean isFavorite) {
        fabFavorite.setImageResource(isFavorite ? R.drawable.map_marker_minus : R.drawable.map_marker_plus);
        fabFavorite.setTag(isFavorite);
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
        hideLoading();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private int getWeatherIconFromDescription(String description) {
        switch (description.toLowerCase()) {
            case "clear":
                return R.drawable.clear_day;
            case "mostly clear":
                return R.drawable.mostly_clear_day;
            case "partly cloudy":
                return R.drawable.partly_cloudy_day;
            case "mostly cloudy":
                return R.drawable.mostly_cloudy;
            case "cloudy":
                return R.drawable.cloudy;
            case "fog":
                return R.drawable.fog;
            case "light fog":
                return R.drawable.fog_light;
            case "drizzle":
                return R.drawable.drizzle;
            case "rain":
                return R.drawable.rain;
            case "light rain":
                return R.drawable.rain_light;
            case "heavy rain":
                return R.drawable.rain_heavy;
            case "snow":
                return R.drawable.snow;
            case "flurries":
                return R.drawable.flurries;
            case "light snow":
                return R.drawable.snow_light;
            case "heavy snow":
                return R.drawable.snow_heavy;
            case "freezing drizzle":
                return R.drawable.freezing_drizzle;
            case "freezing rain":
                return R.drawable.freezing_rain;
            case "light freezing rain":
                return R.drawable.freezing_rain_light;
            case "heavy freezing rain":
                return R.drawable.freezing_rain_heavy;
            case "ice pellets":
                return R.drawable.ice_pellets;
            case "heavy ice pellets":
                return R.drawable.ice_pellets_heavy;
            case "light ice pellets":
                return R.drawable.ice_pellets_light;
            case "thunderstorm":
                return R.drawable.tstorm;
            default:
                return R.drawable.clear_day;
        }
    }

    private int getWeatherIconFromCode(String code) {
        switch (code) {
            case "1000":
                return R.drawable.clear_day;
            case "1100":
                return R.drawable.mostly_clear_day;
            case "1101":
                return R.drawable.partly_cloudy_day;
            case "1102":
                return R.drawable.mostly_cloudy;
            case "1001":
                return R.drawable.cloudy;
            case "2000":
                return R.drawable.fog;
            case "2100":
                return R.drawable.fog_light;
            case "4000":
                return R.drawable.drizzle;
            case "4001":
                return R.drawable.rain;
            case "4200":
                return R.drawable.rain_light;
            case "4201":
                return R.drawable.rain_heavy;
            case "5000":
                return R.drawable.snow;
            case "5001":
                return R.drawable.flurries;
            case "5100":
                return R.drawable.snow_light;
            case "5101":
                return R.drawable.snow_heavy;
            case "6000":
                return R.drawable.freezing_drizzle;
            case "6001":
                return R.drawable.freezing_rain;
            case "6200":
                return R.drawable.freezing_rain_light;
            case "6201":
                return R.drawable.freezing_rain_heavy;
            case "7000":
                return R.drawable.ice_pellets;
            case "7101":
                return R.drawable.ice_pellets_heavy;
            case "7102":
                return R.drawable.ice_pellets_light;
            case "8000":
                return R.drawable.tstorm;
            default:
                return R.drawable.clear_day;
        }
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
}