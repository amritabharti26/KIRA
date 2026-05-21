package com.Amrit_a_b.kiraapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Contact> contactList;
    private ContactAdapter adapter;
    private FloatingActionButton fab;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.rv_contacts);
        fab = findViewById(R.id.fab_add_contact);

        contactList = new ArrayList<>();
        adapter = new ContactAdapter(contactList);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        if (fab != null) {
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddContactActivity.class);
                startActivity(intent);
            });
        }

        loadContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void loadContacts() {
        contactList.clear();

        SharedPreferences prefs = getSharedPreferences("contacts", MODE_PRIVATE);
        String data = prefs.getString("contact_list", null);

        if (data == null) {
            adapter.notifyDataSetChanged();
            return;
        }

        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                String phone = obj.getString("phone");
                int priority = obj.getInt("priority");
                contactList.add(new Contact(name, phone, priority));
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}