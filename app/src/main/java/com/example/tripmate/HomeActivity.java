package com.example.tripmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FrameLayout homeContainer;
    private ImageView imgProfile, navImgProfile;
    private TextView tvUserName, navTvUserName;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onResume() {
        super.onResume();
        loadUserTripCards();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // Set up Drawer Toggle (hamburger icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Find Views
        homeContainer = findViewById(R.id.home_container);
        imgProfile = findViewById(R.id.imgProfile);
        tvUserName = findViewById(R.id.tvUserName);

        // Get header views from navigation drawer
        View headerView = navigationView.getHeaderView(0);
        navImgProfile = headerView.findViewById(R.id.nav_imgProfile);
        navTvUserName = headerView.findViewById(R.id.nav_tvUserName);

        // Set Navigation Item Clicks
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Load user profile info
        loadUserProfile();

        // Load trip cards
        loadUserTripCards();

        // Handle Floating Button for creating trip
        FloatingActionButton fabAddTrip = findViewById(R.id.fab_add_trip);
        fabAddTrip.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CreateTripActivity.class));
        });
    }

    private void showTripList(List<Trip> trips) {
        homeContainer.removeAllViews();
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        TripCardAdapter adapter = new TripCardAdapter(trips, trip -> {
            Intent intent = new Intent(this, TripViewerActivity.class);
            intent.putExtra("tripId", trip.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        homeContainer.addView(recyclerView);
    }

    private void loadUserProfile() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvUserName.setText("Hello " + name + " \uD83D\uDC4B");
                        navTvUserName.setText(name);

                        String photoUrl = null;
                        if (currentUser.getPhotoUrl() != null) {
                            photoUrl = currentUser.getPhotoUrl().toString();
                        }

                        if (photoUrl != null) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.persone)
                                    .into(imgProfile);
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.persone)
                                    .into(navImgProfile);
                        } else {
                            imgProfile.setImageResource(R.drawable.persone);
                            navImgProfile.setImageResource(R.drawable.persone);
                        }
                    }
                });
    }

    private void loadUserTripCards() {
        if (currentUser == null) return;

        db.collection("trips")
                .whereEqualTo("createdBy", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> tripList = queryDocumentSnapshots.getDocuments();
                    if (tripList.isEmpty()) {
                        showEmptyHome();
                    } else {
                        List<Trip> trips = new ArrayList<>();
                        for (DocumentSnapshot doc : tripList) {
                            Trip trip = doc.toObject(Trip.class);
                            trip.id = doc.getId();
                            trips.add(trip);
                        }
                        showTripList(trips);
                    }
                });
    }

    private void showEmptyHome() {
        homeContainer.removeAllViews();
        View emptyView = getLayoutInflater().inflate(R.layout.layout_home_empty, homeContainer, false);
        Button btnCreateTrip = emptyView.findViewById(R.id.btnCreateTrip);
        Button btnViewInvites = emptyView.findViewById(R.id.btnViewInvites);

        btnCreateTrip.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateTripActivity.class));
        });

        btnViewInvites.setOnClickListener(v -> {
            startActivity(new Intent(this, InvitedTripsActivity.class));
        });

        homeContainer.addView(emptyView);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        drawerLayout.closeDrawers();

        if (id == R.id.menu_my_cards) {
            Intent intent = new Intent(HomeActivity.this, MyJourneysActivity.class);
            startActivity(intent);

        } else if (id == R.id.menu_invites) {
            Intent intent = new Intent(HomeActivity.this, InvitedTripsActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_create_card) {
            Intent intent = new Intent(HomeActivity.this, CreateTripActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_logout) {
            mAuth.signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
        return true;
    }
}