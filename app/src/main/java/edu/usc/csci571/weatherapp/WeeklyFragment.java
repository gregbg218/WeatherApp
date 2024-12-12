package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import com.highsoft.highcharts.common.HIGradient;
import com.highsoft.highcharts.common.HIStop;
import com.highsoft.highcharts.core.HIChartView;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.common.HIColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeeklyFragment extends Fragment {
    private static final String TAG = "WeeklyFragment";
    private HIChartView chartView;
    private ProgressBar progressBar;
    private View mainContent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Starting fragment creation");
        View view = inflater.inflate(R.layout.fragment_weekly, container, false);

        // Initialize views
        chartView = view.findViewById(R.id.chart_view);
        progressBar = view.findViewById(R.id.progress_bar);
        mainContent = view.findViewById(R.id.textView6);

        if (chartView == null) {
            Log.e(TAG, "chartView is null! Check if R.id.chart_view exists in fragment_weekly.xml");
//            Toast.makeText(requireContext(), "Error: Chart view not found", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Load and process data
        loadForecastData();
        return view;
    }

    private void loadForecastData() {
        Log.d(TAG, "Starting to load forecast data");
        showLoading();

        Bundle args = getArguments();
        if (args != null && args.containsKey("forecast_data")) {
            try {
                String forecastDataStr = args.getString("forecast_data");
                Log.d(TAG, "Received forecast data string: " + forecastDataStr);

                JSONArray forecastData = new JSONArray(forecastDataStr);
                Log.d(TAG, "Successfully parsed forecast data with " + forecastData.length() + " days");

                setupChart(forecastData);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing forecast data: " + e.getMessage(), e);
                showError("Error processing weather data");
            } finally {
                hideLoading();
            }
        } else {
            Log.w(TAG, "No forecast data found in arguments");
            showError("No weather data available");
            hideLoading();
        }
    }

    private void setupChart(JSONArray forecastData) {
        try {
            HIOptions options = new HIOptions();

            // Chart configuration
            HIChart chart = new HIChart();
            chart.setType("arearange");
            chart.setBackgroundColor(HIColor.initWithName("white"));
            options.setChart(chart);



            // Y-Axis configuration
            HIYAxis yaxis = new HIYAxis();
            yaxis.setTitle(new HITitle());
            yaxis.getTitle().setText("Values");
            yaxis.setMin(30.0);
            yaxis.setMax(80.0);
            yaxis.setTickInterval(10.0);
            yaxis.setGridLineColor(HIColor.initWithHexValue("#E5E5E5"));
            yaxis.setGridLineWidth(1);

            // Configure Y-axis labels
            HILabels yLabels = new HILabels();
            HICSSObject yLabelStyle = new HICSSObject();
            yLabelStyle.setColor("#8C8C8C");
            yLabels.setStyle(yLabelStyle);
            yaxis.setLabels(yLabels);

            ArrayList<HIYAxis> yAxes = new ArrayList<>();
            yAxes.add(yaxis);
            options.setYAxis(yAxes);

            // X-Axis configuration
            HIXAxis xaxis = new HIXAxis();
            xaxis.setType("category");
            HILabels xLabels = new HILabels();
            HICSSObject xLabelStyle = new HICSSObject();
            xLabelStyle.setColor("#8C8C8C");
            xLabels.setStyle(xLabelStyle);
            xaxis.setLabels(xLabels);

            ArrayList<HIXAxis> xAxes = new ArrayList<>();
            xAxes.add(xaxis);
            options.setXAxis(xAxes);

            HITitle title = new HITitle();
            title.setText("Temperature Variation by Day");
            HISubtitle subtitle = new HISubtitle();
            subtitle.setText("");
            title.setAlign("center");
            title.setStyle(new HICSSObject());
            title.getStyle().setFontSize("18px");
            title.getStyle().setColor("#333333");
            options.setTitle(title);
            options.setSubtitle(subtitle);

            // Series data processing
            HIArearange series = new HIArearange();
            series.setShowInLegend(false);

            ArrayList<Object[]> seriesData = new ArrayList<>();
            ArrayList<String> categories = new ArrayList<>();

            SimpleDateFormat inputFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", Locale.US);

            for (int i = 0; i < Math.min(forecastData.length(), 5); i++) {
                JSONObject dayData = forecastData.getJSONObject(i);
                double lowTemp = Double.parseDouble(dayData.getString("tempLow"));
                double highTemp = Double.parseDouble(dayData.getString("tempHigh"));
                String dateStr = dayData.getString("date");

                Date date = inputFormat.parse(dateStr);
                String formattedDate = outputFormat.format(date);

                Log.d(TAG, String.format("Processing day %d: Date=%s, Low=%.1f, High=%.1f",
                        i, formattedDate, lowTemp, highTemp));

                categories.add(formattedDate);
                seriesData.add(new Object[]{i, lowTemp, highTemp});
            }

            series.setData(seriesData);
            xaxis.setCategories(categories);

            // Gradient configuration
            HIGradient gradient = new HIGradient(0, 0, 0, 1);
            LinkedList<HIStop> stops = new LinkedList<>();
            stops.add(new HIStop(0, HIColor.initWithRGBA(255, 170, 100, 0.8)));
            stops.add(new HIStop(1, HIColor.initWithRGBA(100, 170, 255, 0.8)));
            series.setFillColor(HIColor.initWithLinearGradient(gradient, stops));

            // Configure tooltip
            HITooltip tooltip = new HITooltip();
            tooltip.setValueSuffix("°F");
            tooltip.setPointFormat("Temperature Range:<br/>High: {point.high}°F<br/>Low: {point.low}°F");
            options.setTooltip(tooltip);

            // Add series to options
            ArrayList<HISeries> allSeries = new ArrayList<>();
            allSeries.add(series);
            options.setSeries(allSeries);

            // Remove credits
            HICredits credits = new HICredits();
            credits.setEnabled(false);
            options.setCredits(credits);

            // Apply options to chart
            if (chartView != null) {
                chartView.setOptions(options);
                Log.d(TAG, "Chart setup completed successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart: " + e.getMessage(), e);
            showError("Error displaying temperature chart");
        }
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (chartView != null) {
            chartView.setVisibility(View.GONE);
        }
        if (mainContent != null) {
            mainContent.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (chartView != null) {
            chartView.setVisibility(View.VISIBLE);
        }
        if (mainContent != null) {
            mainContent.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, "Error: " + message);
    }
}