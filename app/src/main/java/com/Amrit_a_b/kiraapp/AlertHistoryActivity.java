package com.Amrit_a_b.kiraapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AlertHistoryAdapter adapter;
    private List<Alert> alertList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private TextView tvEmpty;

    // FIXED FIREBASE URL
    private static final String DB_URL = "https://ai-powered-women-safety-ca54a-default-rtdb.firebaseio.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.rv_history);
        tvEmpty = findViewById(R.id.tv_empty);
        
        alertList = new ArrayList<>();
        adapter = new AlertHistoryAdapter(alertList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load Offline History First
        loadLocalHistory();

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            try {
                String userId = mAuth.getCurrentUser().getUid();
                mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference("alerts").child(userId);
                loadFirebaseHistory();
            } catch (Exception e) {
                Log.e("Firebase", "DB Init Error: " + e.getMessage());
            }
        }
    }

    private void loadLocalHistory() {
        SharedPreferences prefs = getSharedPreferences("local_data", MODE_PRIVATE);
        String data = prefs.getString("local_alerts", "[]");
        try {
            JSONArray array = new JSONArray(data);
            alertList.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                alertList.add(new Alert(
                        obj.getString("timestamp"),
                        obj.getString("location"),
                        obj.getString("status")
                ));
            }
            Collections.reverse(alertList);
            adapter.notifyDataSetChanged();
            updateEmptyState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFirebaseHistory() {
        if (mDatabase == null) return;
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alertList.clear();
                // Re-add local first or just use firebase if available
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Alert alert = postSnapshot.getValue(Alert.class);
                    if (alert != null) alertList.add(alert);
                }
                Collections.reverse(alertList);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Load failed: " + error.getMessage());
            }
        });
    }

    private void updateEmptyState() {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(alertList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}