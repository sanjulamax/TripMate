package com.example.tripmate;



import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tripmate.TripCardAdapter;
import com.example.tripmate.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class MyJourneysActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private TripCardAdapterSelf adapter;
    private List<Trip> myTrips = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_journeys);

        recyclerView = findViewById(R.id.recyclerMyJourneys);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TripCardAdapterSelf(myTrips, trip -> {
            // On card click: edit
            Intent intent = new Intent(this, EditTripActivity.class);
            intent.putExtra("tripId", trip.id);
            startActivity(intent);
        }, trip -> {
            // On delete click
            showDeleteDialog(trip);
        });
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadMyTrips();
    }

    private void loadMyTrips() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("trips")
                .whereEqualTo("createdBy", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myTrips.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Trip trip = doc.toObject(Trip.class);
                        trip.id = doc.getId();
                        myTrips.add(trip);
                    }
                    adapter.notifyDataSetChanged();
                    if (myTrips.isEmpty()) {
                        Toast.makeText(this, "No journeys created yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteDialog(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Journey")
                .setMessage("Are you sure you want to delete this journey?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTrip(trip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTrip(Trip trip) {
        db.collection("trips").document(trip.id)
                .delete()
                .addOnSuccessListener(unused -> {
                    myTrips.remove(trip);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Journey deleted.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyTrips();
    }
}