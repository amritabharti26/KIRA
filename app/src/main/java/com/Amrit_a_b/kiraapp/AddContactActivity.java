package com.Amrit_a_b.kiraapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

public class AddContactActivity extends AppCompatActivity {

    private EditText etName, etPhone, etPriority;
    private MaterialToolbar toolbar;
    private Button btnSave;
    
    private boolean isEditMode = false;
    private int editPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etPriority = findViewById(R.id.et_priority);
        btnSave = findViewById(R.id.btn_add_contact);

        // Check if we are in edit mode
        if (getIntent().hasExtra("isEdit")) {
            isEditMode = true;
            editPosition = getIntent().getIntExtra("position", -1);
            etName.setText(getIntent().getStringExtra("name"));
            etPhone.setText(getIntent().getStringExtra("phone"));
            etPriority.setText(String.valueOf(getIntent().getIntExtra("priority", 1)));
            
            if (toolbar != null) toolbar.setTitle("Edit Contact");
            btnSave.setText("Update Contact");
        }

        btnSave.setOnClickListener(v -> saveContact());
    }

    private void saveContact() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String priorityText = etPriority.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(priorityText)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() < 10) {
            Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        int priority;
        try {
            priority = Integer.parseInt(priorityText);
        } catch (Exception e) {
            Toast.makeText(this, "Priority must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SharedPreferences prefs = getSharedPreferences("contacts", MODE_PRIVATE);
            String existing = prefs.getString("contact_list", "[]");
            JSONArray array = new JSONArray(existing);

            JSONObject contact = new JSONObject();
            contact.put("name", name);
            contact.put("phone", phone);
            contact.put("priority", priority);

            if (isEditMode && editPosition != -1) {
                // Update existing
                array.put(editPosition, contact);
                Toast.makeText(this, "Contact Updated", Toast.LENGTH_SHORT).show();
            } else {
                // Add new
                array.put(contact);
                Toast.makeText(this, "Contact Saved", Toast.LENGTH_SHORT).show();
            }

            prefs.edit().putString("contact_list", array.toString()).apply();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving contact", Toast.LENGTH_SHORT).show();
        }
    }
}