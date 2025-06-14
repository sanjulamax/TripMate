package com.example.tripmate;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tripmate.R;
import com.example.tripmate.Trip;
import java.util.List;

public class TripCardAdapterSelf extends RecyclerView.Adapter<TripCardAdapterSelf.TripViewHolder> {

    private List<Trip> tripList;
    private OnTripClickListener listener;
    private OnTripDeleteListener deleteListener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }
    public interface OnTripDeleteListener {
        void onTripDelete(Trip trip);
    }

    public TripCardAdapterSelf(List<Trip> tripList, OnTripClickListener listener, OnTripDeleteListener deleteListener) {
        this.tripList = tripList;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_card_with_delete, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.tvTopic.setText(trip.topic);
        holder.tvDescription.setText(trip.description);
        holder.tvDate.setText(trip.date);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTripClick(trip);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onTripDelete(trip);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvDescription, tvDate;
        ImageButton btnDelete;
        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTripTopic);
            tvDescription = itemView.findViewById(R.id.tvTripDescription);
            tvDate = itemView.findViewById(R.id.tvTripDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteTrip);
        }
    }
}