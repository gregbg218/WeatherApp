// WeeklyFragment.java
package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.highsoft.highcharts.core.HIChartView;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.HIGradient;
import com.highsoft.highcharts.common.HIStop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class WeeklyFragment extends Fragment {
    private HIChartView chartView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weekly, container, false);
        chartView = view.findViewById(R.id.chart_view);
        setupChart();
        return view;
    }

    private void setupChart() {
        HIOptions options = new HIOptions();

        // Basic chart configuration
        HIChart chart = new HIChart();
        chart.setType("arearange");

        // Set zooming options
        HIZooming zooming = new HIZooming();
        zooming.setType("x");
        chart.setZooming(zooming);

        // Set background color
        chart.setBackgroundColor(HIColor.initWithName("transparent"));
        options.setChart(chart);

        // Title configuration
        HITitle title = new HITitle();
        title.setText("Temperature variation by day");
        HICSSObject titleStyle = new HICSSObject();
        titleStyle.setColor("white");
        title.setStyle(titleStyle);
        options.setTitle(title);

        // Y-Axis configuration
        HIYAxis yaxis = new HIYAxis();
        HITitle yAxisTitle = new HITitle();
        yAxisTitle.setText("Temperature (°F)");
        yaxis.setTitle(yAxisTitle);
        HILabels yLabels = new HILabels();
        HICSSObject yLabelStyle = new HICSSObject();
        yLabelStyle.setColor("white");
        yLabels.setStyle(yLabelStyle);
        yaxis.setLabels(yLabels);
        options.setYAxis(new ArrayList<>(Arrays.asList(yaxis)));

        // X-Axis configuration
        HIXAxis xaxis = new HIXAxis();
        HILabels xLabels = new HILabels();
        HICSSObject xLabelStyle = new HICSSObject();
        xLabelStyle.setColor("white");
        xLabels.setStyle(xLabelStyle);
        xaxis.setLabels(xLabels);
        options.setXAxis(new ArrayList<>(Arrays.asList(xaxis)));

        // Series data
        HIArearange series = new HIArearange();
        series.setName("Temperatures");

        // Sample data matching the image
        Object[][] seriesData = new Object[][]{
                {"17. Nov", 45, 66},
                {"18. Nov", 45, 68},
                {"19. Nov", 47, 66},
                {"20. Nov", 45, 69},
                {"21. Nov", 62, 76}
        };

        series.setData(new ArrayList<>(Arrays.asList(seriesData)));

        // Set gradient colors using LinkedList
        HIGradient gradient = new HIGradient(0, 0, 0, 1);
        LinkedList<HIStop> stops = new LinkedList<>();
        stops.add(new HIStop(0, HIColor.initWithRGBA(255, 170, 100, 0.8)));  // Peach color at top
        stops.add(new HIStop(1, HIColor.initWithRGBA(100, 170, 255, 0.8)));  // Blue color at bottom
        series.setFillColor(HIColor.initWithLinearGradient(gradient, stops));

        options.setSeries(new ArrayList<>(Arrays.asList(series)));

        chartView.setOptions(options);
    }
}