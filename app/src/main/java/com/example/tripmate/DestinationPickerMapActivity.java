package com.example.tripmate;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.io.IOException;
import java.util.*;

public class DestinationPickerMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<LatLng> selectedLatLngs = new ArrayList<>();
    private ArrayList<String> selectedPlaceNames = new ArrayList<>();
    private Button btnDone, btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination_picker_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        btnDone = findViewById(R.id.btnDone);
        btnClear = findViewById(R.id.btnClear);

        btnDone.setOnClickListener(v -> returnSelection());
        btnClear.setOnClickListener(v -> {
            selectedLatLngs.clear();
            selectedPlaceNames.clear();
            if (mMap != null) mMap.clear();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(6.9271, 79.8612), 7f));
        mMap.setOnMapClickListener(latLng -> {
            String placeName = getPlaceNameFromLatLng(latLng);
            selectedLatLngs.add(latLng);
            selectedPlaceNames.add(placeName);
            mMap.addMarker(new MarkerOptions().position(latLng).title(placeName));
        });
    }

    private String getPlaceNameFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                if (address.getFeatureName() != null && !address.getFeatureName().equals("Unnamed Road")) {
                    return address.getFeatureName();
                }
                if (address.getLocality() != null) return address.getLocality();
                if (address.getAdminArea() != null) return address.getAdminArea();
                if (address.getCountryName() != null) return address.getCountryName();
                if (address.getAddressLine(0) != null) return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", latLng.latitude, latLng.longitude);
    }

    private void returnSelection() {
        if (selectedLatLngs.isEmpty()) {
            Toast.makeText(this, "Please select at least one destination.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putStringArrayListExtra("placeNames", selectedPlaceNames);
        ArrayList<Double> lats = new ArrayList<>(), lngs = new ArrayList<>();
        for (LatLng latLng : selectedLatLngs) {
            lats.add(latLng.latitude);
            lngs.add(latLng.longitude);
        }
        data.putExtra("lats", lats);
        data.putExtra("lngs", lngs);
        setResult(RESULT_OK, data);
        finish();
    }
}