package com.example.tripmate;


import java.util.List;

import java.util.List;

public class Trip {

    public String topic;
    public String description;
    public String date;
    public String time;
    public String createdBy;
    public List<String> destinations;
    public List<LatLngCoord> destinationCoords;

    public String id;

    public List<String> invitedUsers;
    public String locationName;

    public double locationLat;
    public double locationLng;


    public double budget;


    public Trip() {} // Required for Firestore


    public Trip(String id, String topic, String description, String date) {
        this.id = id;
        this.topic = topic;
        this.description = description;
        this.date = date;
    }
}