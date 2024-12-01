package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.highsoft.highcharts.core.HIChartView;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.HIGradient;
import com.highsoft.highcharts.common.HIStop;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class WeeklyFragment extends Fragment {
    private static final String TAG = "WeeklyFragment";
    private HIChartView chartView;
    private RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Starting fragment creation");
        View view = inflater.inflate(R.layout.fragment_weekly, container, false);

        chartView = view.findViewById(R.id.chart_view);
        if (chartView == null) {
            Log.e(TAG, "chartView is null! Check if R.id.chart_view exists in fragment_weekly.xml");
            Toast.makeText(requireContext(), "Error: Chart view not found", Toast.LENGTH_SHORT).show();
            return view;
        }
        Log.d(TAG, "ChartView found successfully");

        requestQueue = Volley.newRequestQueue(requireContext());
        Log.d(TAG, "RequestQueue initialized");

        fetchWeatherData();
        return view;
    }

    private void fetchWeatherData() {
        String url = "http://10.0.2.2:3001/api/weather/forecast?latitude=34.0030&longitude=-118.2863";
        Log.d(TAG, "Fetching weather data from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Received response: " + response.toString());
                    try {
                        if (response.has("success") && response.getBoolean("success")) {
                            JSONArray data = response.getJSONArray("data");
                            Log.d(TAG, "Received data array with " + data.length() + " items");
                            setupChart(data);
                        } else {
                            Log.e(TAG, "API response indicates failure or missing success field");
                            Toast.makeText(requireContext(), "Error: API request failed", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Network error: " + error.toString(), error);
                    Toast.makeText(requireContext(), "Network error occurred", Toast.LENGTH_SHORT).show();
                }
        );

        Log.d(TAG, "Adding request to queue");
        requestQueue.add(request);
    }

    private void setupChart(JSONArray weatherData) {
        Log.d(TAG, "Setting up chart with data size: " + weatherData.length());

        try {
            HIOptions options = new HIOptions();
            Log.d(TAG, "Created HIOptions");

            // Chart configuration
            HIChart chart = new HIChart();
            chart.setType("arearange");
            HIZooming zooming = new HIZooming();
            zooming.setType("x");
            chart.setZooming(zooming);
            chart.setBackgroundColor(HIColor.initWithName("transparent"));
            options.setChart(chart);
            Log.d(TAG, "Basic chart configuration done");

            // Title configuration
            HITitle title = new HITitle();
            title.setText("Temperature variation by day");
            HICSSObject titleStyle = new HICSSObject();
            titleStyle.setColor("white");
            title.setStyle(titleStyle);
            options.setTitle(title);
            Log.d(TAG, "Title configuration done");

            // Y-Axis
            HIYAxis yaxis = new HIYAxis();
            yaxis.setTitle(new HITitle());
            yaxis.getTitle().setText("Temperature (°F)");
            yaxis.setLabels(new HILabels());
            yaxis.getLabels().setStyle(new HICSSObject());
            yaxis.getLabels().getStyle().setColor("white");
            options.setYAxis(new ArrayList<>(Arrays.asList(yaxis)));
            Log.d(TAG, "Y-axis configuration done");

            // X-Axis
            HIXAxis xaxis = new HIXAxis();
            xaxis.setType("category");
            xaxis.setLabels(new HILabels());
            xaxis.getLabels().setStyle(new HICSSObject());
            xaxis.getLabels().getStyle().setColor("white");
            options.setXAxis(new ArrayList<>(Arrays.asList(xaxis)));
            Log.d(TAG, "X-axis configuration done");

            // Series data
            HIArearange series = new HIArearange();
            series.setName("Temperatures");
            ArrayList<Object[]> seriesData = new ArrayList<>();
            ArrayList<String> categories = new ArrayList<>();

            for (int i = 0; i < weatherData.length(); i++) {
                JSONObject day = weatherData.getJSONObject(i);
                String date = day.getString("date");
                double low = Double.parseDouble(day.getString("tempLow"));
                double high = Double.parseDouble(day.getString("tempHigh"));
                seriesData.add(new Object[]{i, low, high});
                categories.add(date);
                Log.d(TAG, String.format("Added data point: date=%s, low=%.2f, high=%.2f", date, low, high));
            }

            series.setData(seriesData);
            xaxis.setCategories(categories);
            Log.d(TAG, "Series data configuration done");

            // Gradient configuration
            HIGradient gradient = new HIGradient(0, 0, 0, 1);
            LinkedList<HIStop> stops = new LinkedList<>();
            stops.add(new HIStop(0, HIColor.initWithRGBA(255, 170, 100, 0.8)));
            stops.add(new HIStop(1, HIColor.initWithRGBA(100, 170, 255, 0.8)));
            series.setFillColor(HIColor.initWithLinearGradient(gradient, stops));
            Log.d(TAG, "Gradient configuration done");

            options.setSeries(new ArrayList<>(Arrays.asList(series)));
            Log.d(TAG, "Setting options to chartView");

            if (chartView != null) {
                chartView.setOptions(options);
                Log.i(TAG, "Chart setup completed successfully");
            } else {
                Log.e(TAG, "chartView is null when trying to set options!");
                Toast.makeText(requireContext(), "Error: Cannot display chart", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up chart", Toast.LENGTH_SHORT).show();
        }
    }
}