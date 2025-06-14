package com.example.tripmate;

public class WeatherForecast {
    public String destination;
    public String date;
    public String description;
    public double temperature; // in Celsius

    public WeatherForecast(String destination, String date, String description, double temperature) {
        this.destination = destination;
        this.date = date;
        this.description = description;
        this.temperature = temperature;
    }
}