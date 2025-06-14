package com.example.tripmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tripmate.R;
import com.example.tripmate.Trip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class TripCardAdapter extends RecyclerView.Adapter<TripCardAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripCardAdapter(List<Trip> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }



    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_card, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.tvTopic.setText("\uD83C\uDF0D "+trip.topic);
        holder.tvDescription.setText(trip.description);
        holder.tvDate.setText(trip.date);

        // Set a default while loading
        holder.tvCreator.setText("Created by: ...");

        // Fetch creator name from Firestore using userId
        if (trip.createdBy != null && !trip.createdBy.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(trip.createdBy)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String creatorName = userDoc.getString("name");
                        if (creatorName == null || creatorName.isEmpty()) creatorName = "Unknown";
                        // Make sure this holder is still displaying the same trip!
                        if (holder.getAdapterPosition() == position) {
                            holder.tvCreator.setText("Created by: " + creatorName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (holder.getAdapterPosition() == position) {
                            holder.tvCreator.setText("Created by: Unknown");
                        }
                    });
        } else {
            holder.tvCreator.setText("Created by: Unknown");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTripClick(trip);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvDescription, tvDate, tvCreator;
        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTripTopic);
            tvDescription = itemView.findViewById(R.id.tvTripDescription);
            tvDate = itemView.findViewById(R.id.tvTripDate);
            tvCreator = itemView.findViewById(R.id.tvTripCreator);
        }
    }
}