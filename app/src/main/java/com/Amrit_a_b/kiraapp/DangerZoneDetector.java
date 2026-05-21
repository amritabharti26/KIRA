package com.Amrit_a_b.kiraapp;

import android.location.Location;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class DangerZoneDetector {

    private static List<DangerZone> dangerZones = new ArrayList<>();

    public static void loadDangerZones() {

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://ai-powered-women-safety-ca54a-default-rtdb.firebaseio.com/")
                .getReference("danger_zones");

        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                dangerZones.clear();

                for (DataSnapshot zoneSnap : snapshot.getChildren()) {

                    DangerZone zone = zoneSnap.getValue(DangerZone.class);

                    if (zone != null) {
                        dangerZones.add(zone);
                    }
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
            }
        });
    }

    public static boolean isDangerZone(double lat, double lng) {

        float[] result = new float[1];

        for (DangerZone zone : dangerZones) {

            Location.distanceBetween(
                    lat, lng,
                    zone.lat, zone.lng,
                    result
            );

            if (result[0] < zone.radius) {
                return true;
            }
        }

        return false;
    }
}