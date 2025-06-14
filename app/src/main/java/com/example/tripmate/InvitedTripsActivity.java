package com.example.tripmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class InvitedTripsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private TripCardAdapter adapter;
    private List<Trip> invitedTrips = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_trips);

        recyclerView = findViewById(R.id.recyclerInvitedTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripCardAdapter(invitedTrips, trip -> {
            // Open TripViewerActivity for the clicked trip
            Intent intent = new Intent(this, TripViewerActivity.class);
            intent.putExtra("tripId", trip.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadInvitedTrips();
    }

    private void loadInvitedTrips() {
        String userEmail = mAuth.getCurrentUser().getEmail();
        db.collection("trips")
                .whereArrayContains("invitedUsers", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    invitedTrips.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Trip trip = doc.toObject(Trip.class);
                        trip.id = doc.getId();
                        invitedTrips.add(trip);
                    }
                    adapter.notifyDataSetChanged();
                    if (invitedTrips.isEmpty()) {
                        Toast.makeText(this, "No invited trips found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}