package com.Amrit_a_b.kiraapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EvidenceAdapter
        extends RecyclerView.Adapter<EvidenceAdapter.EvidenceViewHolder> {

    public interface OnEvidenceActionListener {

        void onPlay(File file);

        void onDelete(File file);
    }

    private final List<File> files;

    private final OnEvidenceActionListener listener;

    public EvidenceAdapter(
            List<File> files,
            OnEvidenceActionListener listener
    ) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EvidenceViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(
                        parent.getContext()
                ).inflate(
                        R.layout.item_evidence,
                        parent,
                        false
                );

        return new EvidenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EvidenceViewHolder holder,
            int position
    ) {

        File file =
                files.get(position);

        String fileName =
                file.getName();

        holder.tvFileName.setText(
                fileName
        );

        String type;

        if (fileName
                .toLowerCase()
                .endsWith(".mp4")) {

            type = "🎥 Video + Audio";

        } else if (fileName
                .toLowerCase()
                .endsWith(".3gp")) {

            type = "🎙 Audio";

        } else {

            type = "Evidence";
        }

        holder.tvFileType.setText(type);

        String date =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                ).format(
                        new Date(
                                file.lastModified()
                        )
                );

        holder.tvDate.setText(date);

        long sizeBytes =
                file.length();

        holder.tvSize.setText(
                formatFileSize(sizeBytes)
        );

        holder.btnPlay.setOnClickListener(
                v -> listener.onPlay(file)
        );

        holder.btnDelete.setOnClickListener(
                v -> listener.onDelete(file)
        );
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    private String formatFileSize(
            long bytes
    ) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    static class EvidenceViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvFileName;
        TextView tvFileType;
        TextView tvDate;
        TextView tvSize;

        ImageButton btnPlay;
        ImageButton btnDelete;

        EvidenceViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            tvFileName =
                    itemView.findViewById(
                            R.id.tv_evidence_name
                    );

            tvFileType =
                    itemView.findViewById(
                            R.id.tv_evidence_type
                    );

            tvDate =
                    itemView.findViewById(
                            R.id.tv_evidence_date
                    );

            tvSize =
                    itemView.findViewById(
                            R.id.tv_evidence_size
                    );

            btnPlay =
                    itemView.findViewById(
                            R.id.btn_play_evidence
                    );

            btnDelete =
                    itemView.findViewById(
                            R.id.btn_delete_evidence
                    );
        }
    }
}