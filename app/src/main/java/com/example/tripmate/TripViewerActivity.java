package com.example.tripmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class TripViewerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String tripId;

    private TextView tvTripTopic, tvTripDate, tvTripDescription, tvTripDestination;
    private GoogleMap mMap;
    private RecyclerView recyclerChat;
    private EditText etMessage;
    private Button btnSend;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Trip trip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_viewer);

        tripId = getIntent().getStringExtra("tripId");
        if (tripId == null) {
            Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();
            if (id == R.id.menu_budget) {
                Intent intent = new Intent(TripViewerActivity.this, BudgetPlannerActivity.class);
                intent.putExtra("tripId", tripId);
                startActivity(intent);
                return true;
            } else if (id == R.id.menu_gallery) {
                Intent intent = new Intent(this, TripGalleryActivity.class);
                intent.putExtra("tripId", tripId);
                startActivity(intent);;
                return true;
            }
            else if (id == R.id.menue_weather) {
                Intent intent = new Intent(this, WeatherForecastActivity.class);
                intent.putExtra("date", trip.date);
                intent.putStringArrayListExtra("destinations", new ArrayList<>(trip.destinations));
                startActivity(intent);
                return true;
            }
            return false;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTripTopic = findViewById(R.id.tvTripTopic);
        tvTripDate = findViewById(R.id.tvTripDate);
        tvTripDescription = findViewById(R.id.tvTripDescription);
        tvTripDestination = findViewById(R.id.tvTripDestination);
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        chatAdapter = new ChatAdapter(this, messages, mAuth.getCurrentUser().getUid(), tripId);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> sendMessage());

        FrameLayout mapContainer = findViewById(R.id.mapContainer);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, mapFragment, "trip_map")
                    .commit();
        }
        mapFragment.getMapAsync(this);

        loadTrip();
        listenForMessages();
    }

    private void loadTrip() {
        db.collection("trips").document(tripId).get()
                .addOnSuccessListener(doc -> {
                    trip = doc.toObject(Trip.class);
                    if (trip == null) return;
                    tvTripTopic.setText("\uD83C\uDFD5\uFE0F " + trip.topic);
                    tvTripDate.setText("⏳ " + trip.date);
                    tvTripDescription.setText("\uD83D\uDCA1 " + trip.description);

                    if (trip.destinations != null && !trip.destinations.isEmpty()) {
                        tvTripDestination.setText(android.text.TextUtils.join("\uD83D\uDCCD ", trip.destinations));
                    } else {
                        tvTripDestination.setText("No destination set");
                    }

                    if (mMap != null) updateMap();

                    View headerView = navigationView.getHeaderView(0);
                    TextView tvTripName = headerView.findViewById(R.id.tvTripName);
                    TextView tvTripCreator = headerView.findViewById(R.id.tvTripCreator);
                    tvTripName.setText(trip.topic);

                    if (trip.createdBy != null && !trip.createdBy.isEmpty()) {
                        db.collection("users").document(trip.createdBy).get()
                                .addOnSuccessListener(userDoc -> {
                                    String creatorName = userDoc.getString("name");
                                    if (creatorName == null || creatorName.isEmpty()) creatorName = "Unknown";
                                    tvTripCreator.setText("Created by: " + creatorName);
                                })
                                .addOnFailureListener(e -> {
                                    tvTripCreator.setText("Created by: Unknown");
                                });
                    } else {
                        tvTripCreator.setText("Created by: Unknown");
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMap();
    }

    private void updateMap() {
        if (mMap == null || trip == null) return;
        mMap.clear();

        if (trip.destinationCoords != null && !trip.destinationCoords.isEmpty()) {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (int i = 0; i < trip.destinationCoords.size(); i++) {
                LatLngCoord coord = trip.destinationCoords.get(i);
                double lat = coord.getLat();
                double lng = coord.getLng();
                String title = (trip.destinations != null && i < trip.destinations.size()) ? trip.destinations.get(i) : "Destination";
                LatLng latLng = new LatLng(lat, lng);
                mMap.addMarker(new MarkerOptions().position(latLng).title(title));
                boundsBuilder.include(latLng);
            }
            if (trip.destinationCoords.size() == 1) {
                LatLngCoord coord = trip.destinationCoords.get(0);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(coord.getLat(), coord.getLng()), 12f));
            } else if (trip.destinationCoords.size() > 1) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
            }
        }
    }

    private void listenForMessages() {
        db.collection("trips").document(tripId).collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    messages.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null) msg.id = doc.getId();
                        messages.add(msg);
                    }
                    chatAdapter.notifyDataSetChanged();
                    recyclerChat.scrollToPosition(messages.size() - 1);
                });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    if (name == null || name.isEmpty()) {
                        name = "User";
                    }
                    ChatMessage msg = new ChatMessage(uid, name, text, new Date());
                    db.collection("trips").document(tripId).collection("chat").add(msg);
                    etMessage.setText("");
                });
    }
}