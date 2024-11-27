package edu.usc.csci571.weatherapp;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.csci571.weatherapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private RequestQueue requestQueue;
    private static final String TAG = "MainActivity";

    private String latitude;

    private String longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "Activity created");

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch weather data
        getCurrentLocationData();

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
            Log.e(TAG, "TableLayout is null!");
            return;
        }

        Log.d(TAG, "Starting to populate table");
        tableLayout.removeAllViews();

        // Add data rows
        for (int i = 0; i < dailyData.length(); i++) {
            JSONObject dayData = dailyData.getJSONObject(i);
            TableRow row = new TableRow(this);
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
            );
            row.setLayoutParams(rowParams);

            // Alternate row colors
            row.setBackgroundColor(Color.parseColor("#1E1E1E"));

            // Date column
            TextView dateView = new TextView(this);
            String dateString = dayData.getString("date");
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = inputFormat.parse(dateString);
                String formattedDate = outputFormat.format(date);
                dateView.setText(formattedDate);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + e.getMessage());
                dateView.setText(dateString); // Fallback to original date string if parsing fails
            }
            dateView.setTextColor(Color.WHITE);
            dateView.setPadding(16, 0, 16, 0);
            dateView.setGravity(Gravity.CENTER);
            row.addView(dateView);

            // Weather icon column
            ImageView weatherIcon = new ImageView(this);
            String weatherCode = String.valueOf(dayData.getInt("status"));
            weatherIcon.setImageResource(getWeatherIconResource(weatherCode));
            TableRow.LayoutParams iconParams = new TableRow.LayoutParams(
                    dpToPx(35),
                    dpToPx(35)
            );
            iconParams.gravity = Gravity.CENTER;
            weatherIcon.setLayoutParams(iconParams);
            weatherIcon.setPadding(16, 0, 16, 0);
            row.addView(weatherIcon);

            // Temperature Low column
            TextView lowTemp = new TextView(this);
            double tempLow = Double.parseDouble(dayData.getString("tempLow"));
            lowTemp.setText(String.valueOf(Math.round(tempLow)));
            lowTemp.setTextColor(Color.WHITE);
            lowTemp.setPadding(16, 0, 16, 0);
            lowTemp.setGravity(Gravity.CENTER);
            row.addView(lowTemp);

            // Temperature High column
            TextView highTemp = new TextView(this);
            double tempHigh = Double.parseDouble(dayData.getString("tempHigh"));
            highTemp.setText(String.valueOf(Math.round(tempHigh)));
            highTemp.setTextColor(Color.WHITE);
            highTemp.setPadding(16, 0, 16, 0);
            highTemp.setGravity(Gravity.CENTER);
            row.addView(highTemp);

            tableLayout.addView(row);

            // Add divider after each row (except last)
            if (i < dailyData.length() - 1) {
                View divider = new View(this);
                TableLayout.LayoutParams dividerParams = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        1  // height of 1px
                );
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.parseColor("#3A3A3A"));
                tableLayout.addView(divider);
            }
        }

        // Add bottom padding to table
//        tableLayout.setPadding(0, 0, 0, dpToPx(16));
    }



    private void styleRow(TableRow row, boolean isAlternate) {
        row.setBackgroundColor(isAlternate ? Color.DKGRAY : Color.BLACK);
        row.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        // Add this debug code
        Log.d(TAG, "Row styled and added");
    }

    private void styleHeaderRow(TableRow row) {
        row.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        row.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
    }

    private void styleTextView(TextView textView) {
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setGravity(Gravity.CENTER);
    }

    private void styleHeaderTextView(TextView textView) {
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
    }

    private String formatDate(String dateString) {
        // Add date formatting if needed
        return dateString;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Your existing getWeatherIconResource method remains the same

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


    private void addTestData() {
        TableLayout tableLayout = findViewById(R.id.weatherTable);
        if (tableLayout == null) {
            Log.e(TAG, "TableLayout is null");
            return;
        }

        TableRow row = new TableRow(this);
        TextView tv = new TextView(this);
        tv.setText("TEST DATA");
        tv.setTextColor(Color.WHITE);
        row.addView(tv);
        tableLayout.addView(row);
        Log.d(TAG, "Test data added");
    }
    private int getWeatherIconResource(String weatherCode) {
        switch (weatherCode) {
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
                return R.drawable.clear_day; // Default icon
        }
    }


    private void getCurrentLocationData() {


        String ipinfoUrl = "https://ipinfo.io/json?token=b2ac4982de4968";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ipinfoUrl, null,
                response -> {
                    try {
                        Log.d(TAG, "IPinfo response: " + response.toString());

                        // Get location details
                        String city = response.getString("city");
                        String region = response.getString("region");
                        String loc = response.getString("loc"); // "this.latitude,longitude"

                        // Parse coordinates
                        String[] coordinates = loc.split(",");
                        this.latitude = coordinates[0];
                        this.longitude = coordinates[1];

                        // Set location name (City, Region format)
                        String locationName = city + ", " + region;
                        // TODO: Update UI with location name

                        // Fetch weather using coordinates
                        fetchWeatherData();
                        fillHomePageCard2();;


                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing location data: " + e.getMessage());
                        Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show();

                        // Fallback to Los Angeles if IPinfo fails
                        fallbackToLosAngeles();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching location: " + error.getMessage());
                    Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();

                    // Fallback to Los Angeles if IPinfo fails
                    fallbackToLosAngeles();
                });

        // Add timeouts to prevent hanging
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);

    }

    private void fallbackToLosAngeles() {
        // Use LA as fallback as per assignment requirements
        this.latitude = "34.0224";
        this.longitude = "-118.2851";
        fetchWeatherData();
        fillHomePageCard2();
    }

    private void fetchWeatherData() {
        String url = "http://10.0.2.2:3001/api/weather/forecast?latitude=" + this.latitude + "&longitude=" + this.longitude;

        Log.d(TAG, "Fetching weather data from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Got response: " + response.toString());
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONArray dailyData = response.getJSONArray("data");
                            populateWeatherTable(dailyData);
                        } else {
                            Log.e(TAG, "Invalid response format");
                            Toast.makeText(this, "Error fetching weather data", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                    Toast.makeText(this, "Error fetching weather data", Toast.LENGTH_SHORT).show();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }


    private void fillHomePageCard2() {
        String url = "http://10.0.2.2:3001/api/weather/day-weather?latitude=" + this.latitude +
                "&longitude=" + this.longitude + "&date=2024-11-28";
        Log.e(TAG, " greg weather data: " + url);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {

                        if (response.has("success") && response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            Log.e(TAG, " greg weather data: " + data);
                            // Get references to TextViews using their IDs
                            TextView humidityView = (TextView) findViewById(R.id.Humidity_Value);
                            TextView windSpeedView = (TextView) findViewById(R.id.Wind_Speed_Value);
                            TextView visibilityView = (TextView) findViewById(R.id.Visibility_Value);
                            TextView pressureView = (TextView) findViewById(R.id.Pressure_Value);

                            if (humidityView != null && windSpeedView != null &&
                                    visibilityView != null && pressureView != null) {
                                Log.e(TAG, " inside" );
                                // Humidity
                                double humidity = Double.parseDouble(data.getString("humidity"));
                                humidityView.setText(String.format("%.0f%%", humidity));

                                // Wind Speed
                                double windSpeed = Double.parseDouble(data.getString("windSpeed"));
                                windSpeedView.setText(String.format("%.2f mph", windSpeed));

                                // Visibility
                                double visibility = Double.parseDouble(data.getString("visibility"));
                                visibilityView.setText(String.format("%.2f mi", visibility));

                                // Pressure
                                double pressure = Double.parseDouble(data.getString("pressureSeaLevel"));
                                pressureView.setText(String.format("%.2f inHg", pressure));

                                // Set text color to white for all values
//                                humidityView.setTextColor(Color.WHITE);
//                                windSpeedView.setTextColor(Color.WHITE);
//                                visibilityView.setTextColor(Color.WHITE);
//                                pressureView.setTextColor(Color.WHITE);

                                // Log the values to verify they're being set
                                Log.d(TAG, "Humidity: " + humidityView.getText());
                                Log.d(TAG, "Wind Speed: " + windSpeedView.getText());
                                Log.d(TAG, "Visibility: " + visibilityView.getText());
                                Log.d(TAG, "Pressure: " + pressureView.getText());
                            } else {
                                Log.e(TAG, "One or more TextViews not found");
                            }

                        } else {
                            Log.e(TAG, "Invalid response format");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                });

        requestQueue.add(request);
    }

}