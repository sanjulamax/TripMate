package com.example.tripmate;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ViewHolder> {
    private List<WeatherForecast> forecasts;

    public WeatherForecastAdapter(List<WeatherForecast> forecasts) {
        this.forecasts = forecasts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forcast, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherForecast forecast = forecasts.get(position);
        holder.tvDestination.setText("\uD83E\uDE82 "+forecast.destination);
        holder.tvWeatherDate.setText("\uD83D\uDDD3\uFE0F "+forecast.date);
        holder.tvWeatherDescription.setText("\uD83D\uDCCD "+forecast.description);
        holder.tvTemperature.setText("\uD83C\uDF21\uFE0F Temp: " + Math.round(forecast.temperature) + "°C");
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDestination, tvWeatherDate, tvWeatherDescription, tvTemperature;

        ViewHolder(View v) {
            super(v);
            tvDestination = v.findViewById(R.id.tvDestination);
            tvWeatherDate = v.findViewById(R.id.tvWeatherDate);
            tvWeatherDescription = v.findViewById(R.id.tvWeatherDescription);
            tvTemperature = v.findViewById(R.id.tvTemperature);
        }
    }
}