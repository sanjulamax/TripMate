package com.example.tripmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import java.util.*;

public class BudgetPlannerActivity extends AppCompatActivity {
    private TextView tvTotalBudget, tvTotalSpent, tvRemaining;
    private RecyclerView recyclerExpenses;
    private Button btnAddExpense;
    private ExpenseAdapter expenseAdapter;
    private List<TripExpense> expenseList = new ArrayList<>();
    private String tripId;
    private double totalBudget = 0;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_planner);

        tvTotalBudget = findViewById(R.id.tvTotalBudget);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvRemaining = findViewById(R.id.tvRemaining);
        recyclerExpenses = findViewById(R.id.recyclerExpenses);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        tripId = getIntent().getStringExtra("tripId");
        db = FirebaseFirestore.getInstance();

        expenseAdapter = new ExpenseAdapter(expenseList, new ExpenseAdapter.OnExpenseActionListener() {
            @Override
            public void onEdit(TripExpense expense) {
                showAddExpenseDialog(expense);
            }
            @Override
            public void onDelete(TripExpense expense) {
                deleteExpense(expense);
            }
        });
        recyclerExpenses.setLayoutManager(new LinearLayoutManager(this));
        recyclerExpenses.setAdapter(expenseAdapter);

        loadTripAndExpenses();

        btnAddExpense.setOnClickListener(v -> showAddExpenseDialog(null));
    }

    private void deleteExpense(TripExpense exp) {
        db.collection("trips").document(tripId)
                .collection("expenses").document(exp.id)
                .delete();
    }

    // Handles both add and edit
    private void showAddExpenseDialog(@Nullable TripExpense expense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_expense, null);

        EditText etName = view.findViewById(R.id.etExpenseName);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etCategory = view.findViewById(R.id.etCategory);
        EditText etNotes = view.findViewById(R.id.etNotes);

        // Pre-fill if editing
        if (expense != null) {
            etName.setText(expense.name);
            etAmount.setText(String.valueOf(expense.amount));
            etCategory.setText(expense.category);
            etNotes.setText(expense.notes);
        }

        builder.setView(view)
                .setTitle(expense == null ? "Add Expense" : "Edit Expense")
                .setPositiveButton(expense == null ? "Add" : "Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String cat = etCategory.getText().toString();
                    String notes = etNotes.getText().toString();
                    double amt = 0;
                    try { amt = Double.parseDouble(etAmount.getText().toString()); } catch (Exception ignored) {}

                    if (expense == null) {
                        // Add new
                        TripExpense exp = new TripExpense();
                        exp.name = name;
                        exp.category = cat;
                        exp.notes = notes;
                        exp.amount = amt;
                        exp.timestamp = System.currentTimeMillis();
                        db.collection("trips").document(tripId)
                                .collection("expenses").add(exp);
                    } else {
                        // Update existing
                        Map<String, Object> updated = new HashMap<>();
                        updated.put("name", name);
                        updated.put("amount", amt);
                        updated.put("category", cat);
                        updated.put("notes", notes);
                        db.collection("trips").document(tripId)
                                .collection("expenses").document(expense.id)
                                .update(updated);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTripAndExpenses() {
        // Load trip's total budget
        db.collection("trips").document(tripId).get()
                .addOnSuccessListener(doc -> {
                    Trip trip = doc.toObject(Trip.class);
                    if (trip != null) {
                        totalBudget = trip.budget;
                        tvTotalBudget.setText("Total Budget: $" + String.format("%.2f", totalBudget));
                    }
                });

        // Listen for expenses
        db.collection("trips").document(tripId).collection("expenses")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    expenseList.clear();
                    double spent = 0;
                    for (DocumentSnapshot doc : snapshots) {
                        TripExpense expense = doc.toObject(TripExpense.class);
                        if (expense != null) {
                            expense.id = doc.getId();
                            expenseList.add(expense);
                            spent += expense.amount;
                        }
                    }
                    expenseAdapter.notifyDataSetChanged();
                    tvTotalSpent.setText("Total Spent: $" + String.format("%.2f", spent));
                    tvRemaining.setText("Remaining: $" + String.format("%.2f", totalBudget - spent));
                });
    }
}