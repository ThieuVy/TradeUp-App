package com.example.testapptradeup.adapters;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.Report;

import java.util.Objects;

public class AdminReportAdapter extends ListAdapter<Report, AdminReportAdapter.ReportViewHolder> {

    public interface OnReportActionListener {
        void onDismiss(Report report);
        void onSuspend(Report report);
    }

    private final OnReportActionListener listener;

    public AdminReportAdapter(@NonNull OnReportActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView textReportType, textReportTime, textReporter, textReported, textReason;
        Button btnDismiss, btnSuspend;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            textReportType = itemView.findViewById(R.id.text_report_type);
            textReportTime = itemView.findViewById(R.id.text_report_time);
            textReporter = itemView.findViewById(R.id.text_reporter_info);
            textReported = itemView.findViewById(R.id.text_reported_info);
            textReason = itemView.findViewById(R.id.text_reason);
            btnDismiss = itemView.findViewById(R.id.btn_dismiss);
            btnSuspend = itemView.findViewById(R.id.btn_suspend);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Report report, final OnReportActionListener listener) {
            textReportType.setText(report.getType().toUpperCase());

            if (report.getTimestamp() != null) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        report.getTimestamp().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                textReportTime.setText(relativeTime);
            }

            textReporter.setText("Người báo cáo: " + report.getReporterId());
            textReported.setText("Đối tượng: " + (report.getReportedUserId() != null ? report.getReportedUserId() : report.getReportedListingId()));
            textReason.setText(report.getReason());

            btnDismiss.setOnClickListener(v -> listener.onDismiss(report));
            btnSuspend.setOnClickListener(v -> listener.onSuspend(report));
        }
    }

    private static final DiffUtil.ItemCallback<Report> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Report oldItem, @NonNull Report newItem) {
            return oldItem.getStatus().equals(newItem.getStatus());
        }
    };
}