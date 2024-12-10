package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.highsoft.highcharts.core.HIChartView;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.HIGradient;
import com.highsoft.highcharts.common.HIStop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeeklyFragment extends Fragment {
    private static final String TAG = "WeeklyFragment";
    private HIChartView chartView;
    private Random random;

    private static final String BASE_URL = "http://10.0.2.2:3001";

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

        random = new Random();
        generateAndDisplayData();
        return view;
    }

    private void generateAndDisplayData() {
        Log.d(TAG, "Generating random weather data");
        setupChart(generateRandomWeatherData());
    }



    private static class WeatherDataPoint {
        String date;
        double lowTemp;
        double highTemp;

        WeatherDataPoint(String date, double lowTemp, double highTemp) {
            this.date = date;
            this.lowTemp = lowTemp;
            this.highTemp = highTemp;
        }
    }

    private void setupChart(ArrayList<WeatherDataPoint> weatherData) {
        try {
            HIOptions options = new HIOptions();

            // Chart configuration
            HIChart chart = new HIChart();
            chart.setType("arearange");
            chart.setBackgroundColor(HIColor.initWithName("transparent"));
            chart.setPlotBackgroundColor(HIColor.initWithName("white"));
            chart.setMarginTop(35); // Add margin for title
            options.setChart(chart);

            // Add title back
            HITitle title = new HITitle();
            title.setText("Temperature variation by day");
            HICSSObject titleStyle = new HICSSObject();
            titleStyle.setColor("#8C8C8C");
            titleStyle.setFontSize("14px");
            title.setStyle(titleStyle);
            title.setAlign("center");
            title.setMargin(15);
            options.setTitle(title);

            // Y-Axis configuration
            HIYAxis yaxis = new HIYAxis();
            yaxis.setTitle(new HITitle());
            yaxis.getTitle().setText("Values");

            // Configure grid lines
            yaxis.setGridLineColor(HIColor.initWithHexValue("#E5E5E5"));
            yaxis.setGridLineWidth(1);
            yaxis.setGridLineDashStyle("Solid");
            yaxis.setMin(40);
            yaxis.setMax(80);
            yaxis.setTickInterval(5);

            // Show ALL grid lines
            yaxis.setMinorGridLineWidth(1);
            yaxis.setMinorTickInterval(1);
            yaxis.setMinorGridLineColor(HIColor.initWithHexValue("#E5E5E5"));

            HILabels yLabels = new HILabels();
            HICSSObject yLabelStyle = new HICSSObject();
            yLabelStyle.setColor("#8C8C8C");
            yLabels.setStyle(yLabelStyle);
            yaxis.setLabels(yLabels);

            options.setYAxis(new ArrayList<>(Arrays.asList(yaxis)));

            // X-Axis configuration
            HIXAxis xaxis = new HIXAxis();
            xaxis.setType("category");
            xaxis.setGridLineWidth(0);
            HILabels xLabels = new HILabels();
            HICSSObject xLabelStyle = new HICSSObject();
            xLabelStyle.setColor("#8C8C8C");
            xLabels.setStyle(xLabelStyle);
            xaxis.setLabels(xLabels);
            options.setXAxis(new ArrayList<>(Arrays.asList(xaxis)));

            // Legend
            HILegend legend = new HILegend();
            legend.setEnabled(false);
            options.setLegend(legend);

            // Series data
            HIArearange series = new HIArearange();
            series.setName("Temperature Range");
            ArrayList<Object[]> seriesData = new ArrayList<>();
            ArrayList<String> categories = new ArrayList<>();

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", Locale.US);

            for (int i = 0; i < 5; i++) {
                WeatherDataPoint day = weatherData.get(i);
                seriesData.add(new Object[]{i, day.lowTemp, day.highTemp});
                String formattedDate = outputFormat.format(calendar.getTime());
                categories.add(formattedDate);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            series.setData(seriesData);
            xaxis.setCategories(categories);

            // Gradient configuration
            HIGradient gradient = new HIGradient(0, 0, 0, 1);
            LinkedList<HIStop> stops = new LinkedList<>();
            stops.add(new HIStop(0, HIColor.initWithRGBA(255, 179, 148, 1))); // Peach color at top
            stops.add(new HIStop(1, HIColor.initWithRGBA(176, 214, 255, 1))); // Light blue at bottom
            series.setFillColor(HIColor.initWithLinearGradient(gradient, stops));

            // Add connecting line with gradient color
            series.setLineWidth(1);
            series.setLineColor(HIColor.initWithRGBA(255, 179, 148, 1));

            // Add markers with gradient color
            HIMarker marker = new HIMarker();
            marker.setEnabled(true);
            marker.setRadius(4);
            marker.setFillColor(HIColor.initWithRGBA(255, 179, 148, 1));
            marker.setLineWidth(0);
            series.setMarker(marker);

            // Disable hover effects
            HIStates states = new HIStates();
            HIHover hover = new HIHover();
            hover.setEnabled(false);
            states.setHover(hover);
            series.setStates(states);

            // Disable tooltip
            HITooltip tooltip = new HITooltip();
            tooltip.setEnabled(false);
            options.setTooltip(tooltip);

            options.setSeries(new ArrayList<>(Arrays.asList(series)));

            // Chart credits
            HICredits credits = new HICredits();
            credits.setEnabled(false);
            options.setCredits(credits);

            if (chartView != null) {
                chartView.setOptions(options);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up chart", Toast.LENGTH_SHORT).show();
        }
    } // Update the data generation method to match the 5-day requirement
    private ArrayList<WeatherDataPoint> generateRandomWeatherData() {
        ArrayList<WeatherDataPoint> data = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Generate data for 5 days
        for (int i = 0; i < 5; i++) {
            // Base temperature between 60°F and 80°F
            double baseTemp = 60 + random.nextDouble() * 20;

            // Variation of 5-15 degrees for high and low
            double variation = 5 + random.nextDouble() * 10;
            double lowTemp = baseTemp - variation;
            double highTemp = baseTemp + variation;

            // Ensure temperatures stay within the 40-80 range
            lowTemp = Math.max(40, Math.min(80, lowTemp));
            highTemp = Math.max(40, Math.min(80, highTemp));

            String date = dateFormat.format(calendar.getTime());
            data.add(new WeatherDataPoint(date, lowTemp, highTemp));

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return data;
    }
}