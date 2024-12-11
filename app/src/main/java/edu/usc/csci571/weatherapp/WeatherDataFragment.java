package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.core.HIChartView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class WeatherDataFragment extends Fragment {
    private static final String TAG = "WeatherDataFragment";
    private HIChartView chartView;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        view = inflater.inflate(R.layout.fragment_weather_data, container, false);
        chartView = view.findViewById(R.id.chart_view);

        Bundle args = getArguments();
        if (args != null && args.containsKey("weather_data")) {
            try {
                JSONObject data = new JSONObject(args.getString("weather_data"));
                Log.d(TAG, "Received weather data: " + data.toString());
                setupChartWithData(data);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing weather data: " + e.getMessage());
                showError("Error displaying weather data");
            }
        } else {
            Log.e(TAG, "No weather data received in arguments");
            showError("No weather data available");
        }

        return view;
    }

    private void setupChartWithData(JSONObject data) {
        Log.d(TAG, "Setting up chart with weather data");
        try {
            double cloudCover = data.getDouble("cloudCover"); // Already in percentage
            double precipitation = data.getDouble("precipitationIntensity");
            double humidity = data.getDouble("humidity"); // Already in percentage

            Log.d(TAG, String.format("Weather values: Cloud Cover=%.2f%%, Precipitation=%.2f%%, Humidity=%.2f%%",
                    cloudCover, precipitation, humidity));

            HIOptions options = new HIOptions();

            // Chart configuration
            HIChart chart = new HIChart();
            chart.setType("solidgauge");
            options.setChart(chart);

            // Title configuration
            HITitle title = new HITitle();
            title.setText("Weather Stats");
            title.setStyle(new HICSSObject());
            title.getStyle().setFontSize("24px");
            options.setTitle(title);

            // Tooltip configuration
            HITooltip tooltip = new HITooltip();
            tooltip.setBorderWidth(0);
            tooltip.setBackgroundColor(HIColor.initWithName("none"));

            HIShadowOptionsObject shadowOptions = new HIShadowOptionsObject();
            shadowOptions.setColor(HIColor.initWithRGBA(0, 0, 0, 0).toString());
            shadowOptions.setOffsetX(0);
            shadowOptions.setOffsetY(0);
            shadowOptions.setOpacity(0);
            shadowOptions.setWidth(0);
            tooltip.setShadow(shadowOptions);

            tooltip.setStyle(new HICSSObject());
            tooltip.getStyle().setFontSize("16px");
            tooltip.setPointFormat("{series.name}<br><span style=\"font-size:2em; color: {point.color}; font-weight: bold\">{point.y:.1f}%</span>");
            options.setTooltip(tooltip);

            // Pane configuration
            HIPane pane = new HIPane();
            pane.setStartAngle(0);
            pane.setEndAngle(360);

            ArrayList<HIBackground> backgrounds = new ArrayList<>();

            HIBackground bg1 = new HIBackground();
            bg1.setOuterRadius("100%");
            bg1.setInnerRadius("80%");
            bg1.setBackgroundColor(HIColor.initWithRGBA(76, 175, 80, 0.35));
            bg1.setBorderWidth(0);

            HIBackground bg2 = new HIBackground();
            bg2.setOuterRadius("75%");
            bg2.setInnerRadius("55%");
            bg2.setBackgroundColor(HIColor.initWithRGBA(33, 150, 243, 0.35));
            bg2.setBorderWidth(0);

            HIBackground bg3 = new HIBackground();
            bg3.setOuterRadius("50%");
            bg3.setInnerRadius("30%");
            bg3.setBackgroundColor(HIColor.initWithRGBA(244, 67, 54, 0.35));
            bg3.setBorderWidth(0);

            backgrounds.add(bg1);
            backgrounds.add(bg2);
            backgrounds.add(bg3);
            pane.setBackground(backgrounds);

            ArrayList<HIPane> panes = new ArrayList<>();
            panes.add(pane);
            options.setPane(panes);

            // Y-axis configuration
            HIYAxis yaxis = new HIYAxis();
            yaxis.setMin(0);
            yaxis.setMax(100);
            yaxis.setLineWidth(0);
            yaxis.setTickPositions(new ArrayList<>());
            options.setYAxis(new ArrayList<>(Collections.singletonList(yaxis)));

            // Plot options
            HIPlotOptions plotOptions = new HIPlotOptions();
            plotOptions.setSolidgauge(new HISolidgauge());

            ArrayList<HIDataLabels> dataLabels = new ArrayList<>();
            HIDataLabels label = new HIDataLabels();
            label.setEnabled(false);
            dataLabels.add(label);
            plotOptions.getSolidgauge().setDataLabels(dataLabels);

            plotOptions.getSolidgauge().setLinecap("round");
            plotOptions.getSolidgauge().setStickyTracking(false);
            plotOptions.getSolidgauge().setRounded(true);
            options.setPlotOptions(plotOptions);

            // Series data with real values
            ArrayList<HISeries> series = new ArrayList<>();

            HISolidgauge gauge1 = createGaugeSeries("Cloud Cover", cloudCover, "100%", "80%",
                    HIColor.initWithRGB(76, 175, 80));
            HISolidgauge gauge2 = createGaugeSeries("Precipitation", precipitation, "75%", "55%",
                    HIColor.initWithRGB(33, 150, 243));
            HISolidgauge gauge3 = createGaugeSeries("Humidity", humidity, "50%", "30%",
                    HIColor.initWithRGB(244, 67, 54));

            series.add(gauge1);
            series.add(gauge2);
            series.add(gauge3);
            options.setSeries(series);

            chartView.setOptions(options);
            Log.d(TAG, "Chart setup completed successfully");

        } catch (JSONException e) {
            Log.e(TAG, "Error setting up chart: " + e.getMessage());
            showError("Error setting up weather visualization");
        }
    }

    private HISolidgauge createGaugeSeries(String name, double value, String outerRadius,
                                           String innerRadius, HIColor color) {
        HISolidgauge gauge = new HISolidgauge();
        gauge.setName(name);
        HIData data = new HIData();
        data.setColor(color);
        data.setRadius(outerRadius);
        data.setInnerRadius(innerRadius);
        data.setY(value);
        gauge.setData(new ArrayList<>(Collections.singletonList(data)));
        return gauge;
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}