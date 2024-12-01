package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.highsoft.highcharts.core.HIChartView;
import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.hichartsclasses.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class WeeklyFragment extends Fragment {
    private static final String TAG = "WeeklyFragment";
    private HIChartView chartView;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_weekly, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartView = view.findViewById(R.id.chart_view);
        progressBar = view.findViewById(R.id.progress_bar);
        requestQueue = Volley.newRequestQueue(requireContext());

        if (chartView == null) {
            Log.e(TAG, "Chart view not found!");
            return;
        }

        // Initialize chart base configuration
        initializeChart();

        Bundle args = getArguments();
        if (args != null) {
            String latitude = args.getString("latitude");
            String longitude = args.getString("longitude");
            if (latitude != null && longitude != null) {
                fetchMeteogramData(latitude, longitude);
            } else {
                Log.e(TAG, "Latitude or longitude is null");
                showError("Location data missing");
            }
        } else {
            Log.e(TAG, "No arguments found");
            showError("Location data missing");
        }
    }

    private void initializeChart() {
        try {
            HIOptions options = new HIOptions();
            HIChart chart = new HIChart();
            chart.setType("line");
            chart.setBackgroundColor(HIColor.initWithName("transparent"));
            options.setChart(chart);

            // Set base title
            HITitle title = new HITitle();
            title.setText("Temperature Range");
            HICSSObject titleStyle = new HICSSObject();
            titleStyle.setColor(HIColor.initWithName("white").toString());
            title.setStyle(titleStyle);
            options.setTitle(title);

            // Set empty series to initialize the chart
            HILine emptySeries = new HILine();
            emptySeries.setName("Temperature");
            options.setSeries(new ArrayList<>(Collections.singletonList(emptySeries)));

            chartView.setOptions(options);
            Log.d(TAG, "Base chart configuration set");
        } catch (Exception e) {
            Log.e(TAG, "Error setting base configuration: " + e.getMessage());
        }
    }

    private void fetchMeteogramData(String latitude, String longitude) {
        progressBar.setVisibility(View.VISIBLE);
        String url = "http://10.0.2.2:3001/api/weather/meteogram" +
                "?latitude=" + latitude +
                "&longitude=" + longitude;

        Log.d(TAG, "Making API request to: " + url);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "API Response received: " + response.toString());
                    progressBar.setVisibility(View.GONE);

                    if (response.has("data")) {
                        try {
                            JSONObject data = response.getJSONObject("data");
                            if (isAdded() && getActivity() != null) {
                                setupChart(data);
                            } else {
                                Log.e(TAG, "Fragment not attached to activity");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing data: " + e.getMessage());
                            showError("Error loading weather data");
                        }
                    } else {
                        Log.e(TAG, "Response does not contain data field");
                        showError("Invalid data received");
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching meteogram data: " + error.getMessage());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error code: " + error.networkResponse.statusCode);
                    }
                    showError("Error loading weather data");
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void setupChart(JSONObject weatherData) {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        try {
            getActivity().runOnUiThread(() -> {
                try {
                    HIOptions options = new HIOptions();

                    // Chart configuration with white background
                    HIChart chart = new HIChart();
                    chart.setType("line");
                    chart.setBackgroundColor(HIColor.initWithName("white"));
                    chart.setMarginRight(20);
                    chart.setMarginBottom(75);
                    options.setChart(chart);

                    // Configure title
                    HITitle title = new HITitle();
                    title.setText("Temperature Range");
                    HICSSObject titleStyle = new HICSSObject();
                    titleStyle.setColor("black");
                    title.setStyle(titleStyle);
                    options.setTitle(title);

                    // X axis configuration
                    HIXAxis xAxis = new HIXAxis();
                    xAxis.setType("datetime");
                    xAxis.setGridLineWidth(1);
                    xAxis.setTickInterval(24 * 3600 * 1000); // One day intervals

                    // Format date labels
                    HIDateTimeLabelFormats dateTimeLabelFormats = new HIDateTimeLabelFormats();
                    HIDay day = new HIDay();
                    day.setMain("%b %e"); // Format: "Nov 30"
                    dateTimeLabelFormats.setDay(day);
                    xAxis.setDateTimeLabelFormats(dateTimeLabelFormats);

                    HICSSObject xAxisStyle = new HICSSObject();
                    xAxisStyle.setColor("black");
                    HILabels xLabels = new HILabels();
                    xLabels.setStyle(xAxisStyle);
                    xLabels.setRotation(-45); // Rotate labels for better readability
                    xAxis.setLabels(xLabels);
                    options.setXAxis(new ArrayList<>(Collections.singletonList(xAxis)));

                    // Y axis configuration
                    HIYAxis yAxis = new HIYAxis();
                    yAxis.setTitle(new HITitle());
                    yAxis.getTitle().setText("Temperature (°F)");
                    yAxis.setGridLineWidth(1);
                    HICSSObject yAxisStyle = new HICSSObject();
                    yAxisStyle.setColor("black");
                    yAxis.getTitle().setStyle(yAxisStyle);
                    HILabels yLabels = new HILabels();
                    yLabels.setStyle(yAxisStyle);
                    yAxis.setLabels(yLabels);
                    options.setYAxis(new ArrayList<>(Collections.singletonList(yAxis)));

                    // Configure legend
                    HILegend legend = new HILegend();
                    legend.setEnabled(true);
                    legend.setAlign("center");
                    legend.setVerticalAlign("bottom");
                    options.setLegend(legend);

                    // Configure tooltip
                    HITooltip tooltip = new HITooltip();
                    tooltip.setValueSuffix("°F");
                    HIDay tooltipDay = new HIDay();
                    tooltipDay.setMain("%A, %b %e"); // Format: "Monday, Nov 30"
                    tooltip.setDateTimeLabelFormats(new HIDateTimeLabelFormats());
                    tooltip.getDateTimeLabelFormats().setDay(tooltipDay);
                    options.setTooltip(tooltip);

                    // Configure plot options for smoother line
                    HIPlotOptions plotOptions = new HIPlotOptions();
                    HILine plotLine = new HILine();
                    HIAnimationOptionsObject animation = new HIAnimationOptionsObject();
                    animation.setDuration(1000);
                    plotLine.setAnimation(animation);
                    plotOptions.setLine(plotLine);
                    options.setPlotOptions(plotOptions);

                    // Configure series
                    HILine series = new HILine();
                    series.setName("Temperature");
                    series.setColor(HIColor.initWithRGB(255, 69, 0));
                    series.setLineWidth(2);

                    // Process temperature data
                    ArrayList<ArrayList<Object>> data = new ArrayList<>();
                    JSONArray temperatures = weatherData.getJSONArray("temperatures");

                    for (int i = 0; i < temperatures.length(); i++) {
                        JSONObject temp = temperatures.getJSONObject(i);
                        ArrayList<Object> point = new ArrayList<>();
                        point.add(temp.getLong("x"));
                        point.add(temp.getDouble("y"));
                        data.add(point);
                    }

                    series.setData(data);
                    options.setSeries(new ArrayList<>(Collections.singletonList(series)));

                    // Set chart options
                    chartView.setOptions(options);
                    progressBar.setVisibility(View.GONE);

                } catch (Exception e) {
                    Log.e(TAG, "Error setting up chart: " + e.getMessage());
                    showError("Error displaying chart");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in setupChart: " + e.getMessage());
            showError("Error creating chart");
        }
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show()
            );
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