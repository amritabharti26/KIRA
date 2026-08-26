package com.Amrit_a_b.kiraapp;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class EvidenceActivity extends AppCompatActivity
        implements EvidenceAdapter.OnEvidenceActionListener {

    private EvidenceAdapter adapter;

    private final List<File> evidenceFiles =
            new ArrayList<>();

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_evidence);

        MaterialToolbar toolbar =
                findViewById(R.id.toolbar_evidence);

        toolbar.setNavigationOnClickListener(
                v -> finish()
        );

        RecyclerView recyclerView = findViewById(R.id.recycler_evidence);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new EvidenceAdapter(
                        evidenceFiles,
                        this
                );

        recyclerView.setAdapter(adapter);

        loadEvidenceFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Refresh the list whenever the user
         * returns to this screen.
         */
        loadEvidenceFiles();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadEvidenceFiles() {

        evidenceFiles.clear();

        File directory =
                new File(
                        getExternalFilesDir(null),
                        "Evidence"
                );

        if (!directory.exists()) {

            if (!directory.mkdirs()) {
                adapter.notifyDataSetChanged();
                return;
            }
        }

        File[] files =
                directory.listFiles();

        if (files != null) {

            evidenceFiles.addAll(
                    Arrays.asList(files)
            );

            evidenceFiles.sort(
                    Comparator.comparingLong(
                            File::lastModified
                    ).reversed()
            );
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPlay(File file) {

        try {

            stopCurrentPlayback();

            file.getName().toLowerCase();

            /*
             * Audio/video files are opened using
             * Android's media player.
             */
            mediaPlayer =
                    MediaPlayer.create(
                            this,
                            Uri.fromFile(file)
                    );

            if (mediaPlayer == null) {

                Toast.makeText(
                        this,
                        "Unable to play this recording",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            mediaPlayer.setOnCompletionListener(
                    mp -> stopCurrentPlayback()
            );

            mediaPlayer.start();

            Toast.makeText(
                    this,
                    "Playing " + file.getName(),
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to play recording",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onDelete(File file) {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Recording?")
                .setMessage(
                        "This evidence file will be permanently deleted from this device."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            stopCurrentPlayback();

                            if (file.exists()
                                    && file.delete()) {

                                Toast.makeText(
                                        this,
                                        "Recording deleted",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadEvidenceFiles();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Unable to delete recording",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
    }

    private void stopCurrentPlayback() {

        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

            } catch (Exception ignored) {
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {

        stopCurrentPlayback();

        super.onDestroy();
    }
}