package edu.usc.csci571.weatherapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.os.Bundle;
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
    private String locationInWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "Activity created");
        requestQueue = Volley.newRequestQueue(this);
        getCurrentLocationData();
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

        String ipinfoUrl = "https://ipinfo.io/json?token=b2ac4982de4968";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ipinfoUrl, null,
                response -> {
                    try {
                        String city = response.getString("city");
                        String region = response.getString("region");
                        this.locationInWords = city + ", " + region;
                        String loc = response.getString("loc");

                        String[] coordinates = loc.split(",");
                        this.latitude = coordinates[0];
                        this.longitude = coordinates[1];

                        fetchWeatherData();
                        fillHomePageCard2();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing location data: " + e.getMessage());
                        fallbackToLosAngeles();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching location: " + error.getMessage());
                    fallbackToLosAngeles();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void fallbackToLosAngeles() {
        this.latitude = "34.0224";
        this.longitude = "-118.2851";
        this.locationInWords = "Los Angeles, California";
        fetchWeatherData();
        fillHomePageCard2();
    }

    private void fetchWeatherData() {
        String url = "http://10.0.2.2:3001/api/weather/forecast?latitude=" + this.latitude + "&longitude=" + this.longitude;


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {

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

                    showError("Error fetching weather data");
                    Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void fillHomePageCard2() {
        String url = "http://10.0.2.2:3001/api/weather/day-weather?latitude=" + this.latitude +
                "&longitude=" + this.longitude + "&date=2024-11-28";


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {

                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            fillHomePageCard1(data);

                            TextView humidityView = findViewById(R.id.Humidity_Value);
                            TextView windSpeedView = findViewById(R.id.Wind_Speed_Value);
                            TextView visibilityView = findViewById(R.id.Visibility_Value);
                            TextView pressureView = findViewById(R.id.Pressure_Value);

                            if (humidityView != null && windSpeedView != null &&
                                    visibilityView != null && pressureView != null) {

                                double humidity = Double.parseDouble(data.getString("humidity"));
                                humidityView.setText(String.format("%.0f%%", humidity));

                                double windSpeed = Double.parseDouble(data.getString("windSpeed"));
                                windSpeedView.setText(String.format("%.2f mph", windSpeed));

                                double visibility = Double.parseDouble(data.getString("visibility"));
                                visibilityView.setText(String.format("%.2f mi", visibility));

                                double pressure = Double.parseDouble(data.getString("pressureSeaLevel"));
                                pressureView.setText(String.format("%.2f inHg", pressure));
                            }
                        } else {
                            showError("Invalid weather data format");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("Error parsing weather data");
                    }
                },
                error -> {

                    showError("Error fetching weather data");
                    Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void fillHomePageCard1(JSONObject data) throws JSONException {
        Log.e(TAG, "Card 1 fetching weather data: " + data);
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
            row.setBackgroundColor(Color.parseColor("#1E1E1E"));

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
            dateView.setTextColor(Color.WHITE);
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
            lowTemp.setTextColor(Color.WHITE);
            lowTemp.setPadding(16, 0, 16, 0);
            lowTemp.setGravity(Gravity.CENTER);
            row.addView(lowTemp);

            TextView highTemp = new TextView(this);
            double tempHigh = Double.parseDouble(dayData.getString("tempHigh"));
            highTemp.setText(String.valueOf(Math.round(tempHigh)));
            highTemp.setTextColor(Color.WHITE);
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
        if (latitude != null && longitude != null) {
            fetchWeatherData();
            fillHomePageCard2();
        } else {
            getCurrentLocationData();
        }
    }
}