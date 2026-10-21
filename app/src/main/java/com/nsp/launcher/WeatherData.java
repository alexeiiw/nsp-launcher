package com.nsp.launcher;

public class WeatherData {

    public final double temperature;
    public final int code;
    public final long updated;

    public WeatherData(double temperature, int code, long updated) {
        this.temperature = temperature;
        this.code = code;
        this.updated = updated;
    }
}
