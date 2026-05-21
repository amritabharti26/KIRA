package com.Amrit_a_b.kiraapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    List<Contact> contactList;
    Context context;

    public ContactAdapter(List<Contact> contactList) {
        this.contactList = contactList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvPhone;
        ImageView ivCall, ivDelete, ivEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);

            ivCall = itemView.findViewById(R.id.iv_call);
            ivDelete = itemView.findViewById(R.id.iv_delete);
            ivEdit = itemView.findViewById(R.id.iv_edit);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_contact, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Contact contact = contactList.get(position);

        holder.tvName.setText(contact.name);
        holder.tvPhone.setText(contact.phone);

        // Call button
        holder.ivCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + contact.phone));
            context.startActivity(intent);
        });

        // Edit button
        if (holder.ivEdit != null) {
            holder.ivEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddContactActivity.class);
                intent.putExtra("isEdit", true);
                intent.putExtra("position", position);
                intent.putExtra("name", contact.name);
                intent.putExtra("phone", contact.phone);
                intent.putExtra("priority", contact.priority);
                context.startActivity(intent);
            });
        }

        // Delete button
        holder.ivDelete.setOnClickListener(v -> {
            deleteContact(position);
        });
    }

    private void deleteContact(int position) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("contacts", Context.MODE_PRIVATE);
            String data = prefs.getString("contact_list", "[]");
            JSONArray array = new JSONArray(data);
            
            // Remove from list and array
            contactList.remove(position);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                if (i != position) {
                    newArray.put(array.get(i));
                }
            }
            
            // Update SharedPreferences
            prefs.edit().putString("contact_list", newArray.toString()).apply();
            
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, contactList.size());
            Toast.makeText(context, "Contact Deleted", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error deleting contact", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }
}