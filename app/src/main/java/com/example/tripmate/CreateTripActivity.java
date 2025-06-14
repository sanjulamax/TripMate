package com.example.tripmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class CreateTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etTripTopic, etDescription, etDate, etTime, etDestinations, etInvites, etEstimatedBudget;
    private Button btnSaveTrip;
    private TextView tvLocation;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ArrayList<String> selectedPlaceNames = new ArrayList<>();
    private ArrayList<LatLng> selectedLatLngs = new ArrayList<>();

    private MapView mapView;
    private GoogleMap googleMap;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etTripTopic = findViewById(R.id.etTripTopic);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etDestinations = findViewById(R.id.etDestinations);
        etInvites = findViewById(R.id.etInvites);
        etEstimatedBudget = findViewById(R.id.etEstimatedBudget);
        btnSaveTrip = findViewById(R.id.btnSaveTrip);
        tvLocation = findViewById(R.id.tvLocation);
        mapView = findViewById(R.id.mapView);

        // Initialize MapView
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnSaveTrip.setOnClickListener(v -> saveTripCard());

        // Listen to destination changes and update map in real time
        etDestinations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                geocodeAndShowDestinations(s.toString());
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            etDate.setText(dateStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            etTime.setText(timeStr);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    /** Geocode each destination and update markers on the map in the UI thread */
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
            Geocoder geocoder = new Geocoder(CreateTripActivity.this, Locale.getDefault());
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

    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;
        googleMap.getUiSettings().setAllGesturesEnabled(false); // Small preview, not interactive
        // Center map somewhere neutral
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20,0), 2f));
        // Show initial if any text
        geocodeAndShowDestinations(etDestinations.getText().toString());
    }

    // MapView lifecycle
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

    private void saveTripCard() {
        String topic = etTripTopic.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String invitesStr = etInvites.getText().toString().trim();

        if (topic.isEmpty() || desc.isEmpty() || date.isEmpty() || time.isEmpty() ||
                selectedPlaceNames.isEmpty() || selectedLatLngs.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and mark destinations.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> trip = new HashMap<>();
        trip.put("topic", topic);
        trip.put("description", desc);
        trip.put("date", date);
        trip.put("time", time);
        trip.put("createdBy", mAuth.getCurrentUser().getUid());
        trip.put("createdAt", new Date());
        trip.put("destinations", new ArrayList<>(selectedPlaceNames));

        List<LatLngCoord> destinationCoords = new ArrayList<>();
        for (LatLng latlng : selectedLatLngs) {
            destinationCoords.add(new LatLngCoord(latlng.latitude, latlng.longitude));
        }
        trip.put("destinationCoords", destinationCoords);

        List<String> invites = new ArrayList<>();
        if (!invitesStr.isEmpty()) {
            for (String inv : invitesStr.split(",")) {
                invites.add(inv.trim());
            }
        }
        trip.put("invitedUsers", invites);

        String estimatedBudgetStr = etEstimatedBudget.getText().toString().trim();
        double estimatedBudget = 0;
        if (!estimatedBudgetStr.isEmpty()) {
            try {
                estimatedBudget = Double.parseDouble(estimatedBudgetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid estimated budget amount.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        trip.put("budget", estimatedBudget);

        db.collection("trips")
                .add(trip)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Trip Card Created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}