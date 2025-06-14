package com.example.tripmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
    private List<TripExpense> expenses;
    private OnExpenseActionListener listener;

    public interface OnExpenseActionListener {
        void onEdit(TripExpense expense);
        void onDelete(TripExpense expense);
    }

    public ExpenseAdapter(List<TripExpense> expenses, OnExpenseActionListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TripExpense exp = expenses.get(position);
        holder.tvName.setText(exp.name + " ($" + String.format("%.2f", exp.amount) + ")");
        String details = exp.category;
        if (exp.notes != null && !exp.notes.isEmpty()) details += " • " + exp.notes;
        holder.tvDetails.setText(details);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(exp);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(exp);
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;
        Button btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvExpenseName);
            tvDetails = v.findViewById(R.id.tvExpenseDetails);
            btnEdit = v.findViewById(R.id.btnEditExpense);
            btnDelete = v.findViewById(R.id.btnDeleteExpense);
        }
    }
}