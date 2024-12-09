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

    private ArrayList<WeatherDataPoint> generateRandomWeatherData() {
        ArrayList<WeatherDataPoint> data = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Generate data for next 15 days
        for (int i = 0; i < 15; i++) {
            // Base temperature between 60°F and 80°F
            double baseTemp = 60 + random.nextDouble() * 20;

            // Variation of 5-15 degrees for high and low
            double variation = 5 + random.nextDouble() * 10;
            double lowTemp = baseTemp - variation;
            double highTemp = baseTemp + variation;

            String date = dateFormat.format(calendar.getTime());
            data.add(new WeatherDataPoint(date, lowTemp, highTemp));

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return data;
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
        Log.d(TAG, "Setting up chart with data size: " + weatherData.size());

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

            // Title configuration
            HITitle title = new HITitle();
            title.setText("Temperature variation by day");
            HICSSObject titleStyle = new HICSSObject();
            titleStyle.setColor("white");
            title.setStyle(titleStyle);
            options.setTitle(title);

            // Y-Axis
            HIYAxis yaxis = new HIYAxis();
            yaxis.setTitle(new HITitle());
            yaxis.getTitle().setText("Temperature (°F)");
            yaxis.setLabels(new HILabels());
            yaxis.getLabels().setStyle(new HICSSObject());
            yaxis.getLabels().getStyle().setColor("white");
            options.setYAxis(new ArrayList<>(Arrays.asList(yaxis)));

            // X-Axis
            HIXAxis xaxis = new HIXAxis();
            xaxis.setType("category");
            xaxis.setLabels(new HILabels());
            xaxis.getLabels().setStyle(new HICSSObject());
            xaxis.getLabels().getStyle().setColor("white");
            options.setXAxis(new ArrayList<>(Arrays.asList(xaxis)));

            // Series data
            HIArearange series = new HIArearange();
            series.setName("Temperatures");
            ArrayList<Object[]> seriesData = new ArrayList<>();
            ArrayList<String> categories = new ArrayList<>();

            for (int i = 0; i < weatherData.size(); i++) {
                WeatherDataPoint day = weatherData.get(i);
                seriesData.add(new Object[]{i, day.lowTemp, day.highTemp});
                categories.add(day.date);
                Log.d(TAG, String.format("Added data point: date=%s, low=%.2f, high=%.2f",
                        day.date, day.lowTemp, day.highTemp));
            }

            series.setData(seriesData);
            xaxis.setCategories(categories);

            // Gradient configuration
            HIGradient gradient = new HIGradient(0, 0, 0, 1);
            LinkedList<HIStop> stops = new LinkedList<>();
            stops.add(new HIStop(0, HIColor.initWithRGBA(255, 170, 100, 0.8)));
            stops.add(new HIStop(1, HIColor.initWithRGBA(100, 170, 255, 0.8)));
            series.setFillColor(HIColor.initWithLinearGradient(gradient, stops));

            options.setSeries(new ArrayList<>(Arrays.asList(series)));

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