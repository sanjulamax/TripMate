package com.example.tripmate;



import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // Not logged in, show LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Close MainActivity so user can't go back here
        } else {
            // User is logged in, show home screen (setContentView, etc.)

            startActivity(new Intent(this, HomeActivity.class));

            // TODO: Display your Home UI here
        }


    }


}

