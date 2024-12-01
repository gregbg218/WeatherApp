package edu.usc.csci571.weatherapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.core.HIChartView;

import java.util.ArrayList;
import java.util.Collections;

public class WeatherDataFragment extends Fragment {
    private HIChartView chartView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather_data, container, false);
        chartView = view.findViewById(R.id.chart_view);
        setupChartWithDummyData();
        return view;
    }

    private void setupChartWithDummyData() {
        HIOptions options = new HIOptions();

        // Chart configuration
        HIChart chart = new HIChart();
        chart.setType("solidgauge");
        options.setChart(chart);

        // Title configuration
        HITitle title = new HITitle();
        title.setText("Weather Data");
        title.setStyle(new HICSSObject());
        title.getStyle().setFontSize("24px");
        options.setTitle(title);

        // Tooltip configuration
        HITooltip tooltip = new HITooltip();
        tooltip.setBorderWidth(0);
        tooltip.setBackgroundColor(HIColor.initWithName("none"));

        // Fixed shadow configuration
        // Replace the previous shadow code with this:
        HIShadowOptionsObject shadowOptions = new HIShadowOptionsObject();
        shadowOptions.setColor(HIColor.initWithRGBA(0, 0, 0, 0).toString());  // Transparent color for no shadow
        shadowOptions.setOffsetX(0);
        shadowOptions.setOffsetY(0);
        shadowOptions.setOpacity(0);
        shadowOptions.setWidth(0);
        tooltip.setShadow(shadowOptions);

        tooltip.setStyle(new HICSSObject());
        tooltip.getStyle().setFontSize("16px");
        tooltip.setPointFormat("{series.name}<br><span style=\"font-size:2em; color: {point.color}; font-weight: bold\">{point.y}%</span>");
        options.setTooltip(tooltip);

        // Pane configuration
        HIPane pane = new HIPane();
        pane.setStartAngle(0);
        pane.setEndAngle(360);

        ArrayList<HIBackground> backgrounds = new ArrayList<>();

        HIBackground bg1 = new HIBackground();
        bg1.setOuterRadius("112%");
        bg1.setInnerRadius("88%");
        bg1.setBackgroundColor(HIColor.initWithRGBA(76, 175, 80, 0.35));
        bg1.setBorderWidth(0);

        HIBackground bg2 = new HIBackground();
        bg2.setOuterRadius("87%");
        bg2.setInnerRadius("63%");
        bg2.setBackgroundColor(HIColor.initWithRGBA(33, 150, 243, 0.35));
        bg2.setBorderWidth(0);

        HIBackground bg3 = new HIBackground();
        bg3.setOuterRadius("62%");
        bg3.setInnerRadius("38%");
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

        // Series data
        ArrayList<HISeries> series = new ArrayList<>();

        HISolidgauge gauge1 = createGaugeSeries("Cloud Cover", 80, "112%", "88%",
                HIColor.initWithRGB(76, 175, 80));
        HISolidgauge gauge2 = createGaugeSeries("Precipitation", 65, "87%", "63%",
                HIColor.initWithRGB(33, 150, 243));
        HISolidgauge gauge3 = createGaugeSeries("Humidity", 50, "62%", "38%",
                HIColor.initWithRGB(244, 67, 54));

        series.add(gauge1);
        series.add(gauge2);
        series.add(gauge3);
        options.setSeries(series);

        chartView.setOptions(options);
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
}