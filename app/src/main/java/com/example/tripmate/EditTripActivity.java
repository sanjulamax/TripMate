package com.example.tripmate;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class EditTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etTripTopic, etDescription, etDate, etTime, etDestinations, etInvites;
    private Button btnSaveTrip;
    private TextView tvLocation;
    private MapView mapView;
    private GoogleMap googleMap;

    private ArrayList<String> selectedPlaceNames = new ArrayList<>();
    private ArrayList<LatLng> selectedLatLngs = new ArrayList<>();

    private FirebaseFirestore db;
    private String tripId;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);

        db = FirebaseFirestore.getInstance();

        // Find views
        etTripTopic = findViewById(R.id.etTripTopic);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etDestinations = findViewById(R.id.etDestinations);
        etInvites = findViewById(R.id.etInvites);
        btnSaveTrip = findViewById(R.id.btnSaveTrip);
        tvLocation = findViewById(R.id.tvLocation);
        mapView = findViewById(R.id.mapView);

        // --- MapView setup ---
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        tripId = getIntent().getStringExtra("tripId");
        if (TextUtils.isEmpty(tripId)) {
            Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTrip();

        // Update map in real time as user types destinations
        etDestinations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                geocodeAndShowDestinations(s.toString());
            }
        });

        btnSaveTrip.setOnClickListener(v -> saveTripCard());
    }

    private void loadTrip() {
        db.collection("trips").document(tripId).get()
                .addOnSuccessListener(doc -> {
                    Trip trip = doc.toObject(Trip.class);
                    if (trip == null) {
                        Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    etTripTopic.setText(trip.topic);
                    etDescription.setText(trip.description);
                    etDate.setText(trip.date);
                    etTime.setText(trip.time);
                    etDestinations.setText(trip.destinations != null ? TextUtils.join(", ", trip.destinations) : "");
                    etInvites.setText(trip.invitedUsers != null ? TextUtils.join(", ", trip.invitedUsers) : "");
                    // When trip is loaded, update map for destinations
                    geocodeAndShowDestinations(etDestinations.getText().toString());
                });
    }

    private void geocodeAndShowDestinations(String destStr) {
        if (googleMap == null) return;
        if (destStr.trim().isEmpty()) {
            googleMap.clear();
            selectedPlaceNames.clear();
            selectedLatLngs.clear();
            tvLocation.setText("No location selected");
            return;
        }
        new GeocodeTask().execute(destStr.split(","));
    }

    private class GeocodeTask extends AsyncTask<String, Void, List<GeocodeResult>> {
        @Override
        protected List<GeocodeResult> doInBackground(String... dests) {
            Geocoder geocoder = new Geocoder(EditTripActivity.this, Locale.getDefault());
            List<GeocodeResult> results = new ArrayList<>();
            for (String s : dests) {
                String place = s.trim();
                if (place.isEmpty()) continue;
                try {
                    List<Address> addresses = geocoder.getFromLocationName(place, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        results.add(new GeocodeResult(place, new LatLng(address.getLatitude(), address.getLongitude())));
                    } else {
                        results.add(new GeocodeResult(place, null));
                    }
                } catch (Exception e) {
                    results.add(new GeocodeResult(place, null));
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<GeocodeResult> results) {
            if (googleMap == null) return;
            googleMap.clear();
            selectedPlaceNames.clear();
            selectedLatLngs.clear();
            int found = 0;
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (GeocodeResult r : results) {
                if (r.latLng != null) {
                    MarkerOptions mo = new MarkerOptions().position(r.latLng).title(r.placeName);
                    googleMap.addMarker(mo);
                    selectedPlaceNames.add(r.placeName);
                    selectedLatLngs.add(r.latLng);
                    boundsBuilder.include(r.latLng);
                    found++;
                }
            }
            if (found > 0) {
                tvLocation.setText("Destinations marked: " + found);
                try {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 48));
                } catch (Exception ignore) {}
            } else {
                tvLocation.setText("No location selected");
            }
        }
    }

    private static class GeocodeResult {
        String placeName;
        LatLng latLng;
        GeocodeResult(String n, LatLng l) { placeName = n; latLng = l; }
    }

    private void saveTripCard() {
        String topic = etTripTopic.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String destinationsStr = etDestinations.getText().toString().trim();
        String invitesStr = etInvites.getText().toString().trim();

        if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(desc) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)
                || selectedPlaceNames.isEmpty() || selectedLatLngs.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and mark destinations.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> trip = new HashMap<>();
        trip.put("topic", topic);
        trip.put("description", desc);
        trip.put("date", date);
        trip.put("time", time);

        trip.put("destinations", new ArrayList<>(selectedPlaceNames));
        // Optionally, add lat/lngs as a separate list for each destination:
        List<Map<String, Double>> coords = new ArrayList<>();
        for (LatLng latLng : selectedLatLngs) {
            Map<String, Double> coord = new HashMap<>();
            coord.put("lat", latLng.latitude);
            coord.put("lng", latLng.longitude);
            coords.add(coord);
        }
        trip.put("destinationCoords", coords);

        // Invites: split by comma and trim
        List<String> invites = new ArrayList<>();
        if (!TextUtils.isEmpty(invitesStr)) {
            for (String inv : invitesStr.split(",")) {
                invites.add(inv.trim());
            }
        }
        trip.put("invitedUsers", invites);

        db.collection("trips").document(tripId)
                .update(trip)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Trip updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- MapView lifecycle ---
    @Override public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;
        // Show destinations if there is text
        geocodeAndShowDestinations(etDestinations.getText().toString());
        googleMap.getUiSettings().setAllGesturesEnabled(false); // Preview only
    }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}