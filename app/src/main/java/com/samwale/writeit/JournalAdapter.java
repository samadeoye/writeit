package com.samwale.writeit;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.app.DatePickerDialog;

import androidx.appcompat.app.AlertDialog;
import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.wasabeef.richeditor.RichEditor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {

    private final List<JournalModel> journalList;
    private Context context;
    private String deviceId;

    public interface OnEntryUpdatedListener {
        void onEntryUpdated(JournalModel updatedEntry);
        void onEntryDeleted(JournalModel deletedEntry);
    }

    private OnEntryUpdatedListener updateListener;

    // Constructor to initialize data list
    public JournalAdapter(Context context, List<JournalModel> journalList, OnEntryUpdatedListener listener, String deviceId) {
        this.context = context;
        this.journalList = journalList;
        this.updateListener = listener;
        this.deviceId = deviceId;
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the same layout for both header and item rows
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.journal_entry, parent, false);
        return new JournalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalModel data = journalList.get(position);
        String title = data.getTitle();
        String date = data.getDate();
        String details = data.getDetails();

        holder.titleView.setText(title);
        holder.dateView.setText(date);

        // Show full content in dialog when the preview body is clicked
        View.OnClickListener showFullContentDialog = v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(title);

            WebView webView = new WebView(context);
            webView.loadDataWithBaseURL(null, "<html><body>Date: " + date + "<br><br>" + details + "</body></html>", "text/html", "utf-8", null);

            builder.setView(webView);
            builder.setNegativeButton("Close", null);
            builder.setPositiveButton("Edit", (dialog, which) -> {
                showEditDialog(data, position);
            });
            builder.show();
        };

        holder.editBtn.setOnClickListener(v -> {
            showEditDialog(data, position);
        });

        holder.deleteBtn.setOnClickListener(v -> {
            showDeleteDialog(data, position);
        });

        // Open the full details dialog on click of the preview body
        holder.detailsView.setOnClickListener(showFullContentDialog);

        // Show only a few lines for preview. On click, to show all content
        holder.detailsView.setText(Html.fromHtml(details, Html.FROM_HTML_MODE_LEGACY));
        holder.detailsView.setMaxLines(5);
        holder.detailsView.setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    // Add a new entry to the existing data in the journal list
    public void addJournalEntry(JournalModel journalEntry) {
        journalList.add(0, journalEntry); // Insert at the top
        notifyItemInserted(0); // Notify that item was inserted at the top
    }

    // Add new entries to the existing data in the journal list
    public void addJournalEntries(List<JournalModel> newEntries) {
        journalList.addAll(newEntries);
        notifyDataSetChanged();
    }

    // Remove current entries and set new entries into the journal list
    public void setJournalEntries(List<JournalModel> entries) {
        journalList.clear(); // Clear existing entries
        journalList.addAll(entries); // Add new ones
        notifyDataSetChanged();
    }

    // Clear all entries in the journal list
    public void clearAll() {
        journalList.clear();
        notifyDataSetChanged();
    }

    // ViewHolder for journal entries
    public static class JournalViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, dateView, detailsView;
        ImageButton editBtn, deleteBtn;

        public JournalViewHolder(View itemView) {
            super(itemView);
            // Initialize the TextViews for each column
            titleView = itemView.findViewById(R.id.titleView);
            dateView = itemView.findViewById(R.id.dateView);
            detailsView = itemView.findViewById(R.id.detailsView);
            editBtn = itemView.findViewById(R.id.editBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }

    private void showEditDialog(JournalModel journal, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.save_journal_entry, null);

        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText dateInput = dialogView.findViewById(R.id.dateInput);
        //EditText detailsInput = dialogView.findViewById(R.id.detailsInput);
        RichEditor detailsInput = dialogView.findViewById(R.id.detailsInputRichEditor);
        Button detailsInputBtnBold = dialogView.findViewById(R.id.detailsInputBtnBold);
        Button detailsInputBtnItalics = dialogView.findViewById(R.id.detailsInputBtnItalics);
        Button detailsInputBtnUnderline = dialogView.findViewById(R.id.detailsInputBtnUnderline);
        Button detailsInputBtnStrikethrough = dialogView.findViewById(R.id.detailsInputBtnStrikethrough);
        ImageButton detailsInputBtnBulletList = dialogView.findViewById(R.id.detailsInputBtnBulletList);
        ImageButton detailsInputBtnNumberList = dialogView.findViewById(R.id.detailsInputBtnNumberList);

        titleInput.setText(journal.getTitle());
        dateInput.setText(journal.getDate());
        //detailsInput.setText(journal.getDetails());
        detailsInput.setHtml(journal.getDetails());

        detailsInputBtnBold.setOnClickListener(v -> detailsInput.setBold());
        detailsInputBtnItalics.setOnClickListener(v -> detailsInput.setItalic());
        detailsInputBtnUnderline.setOnClickListener(v -> detailsInput.setUnderline());
        detailsInputBtnStrikethrough.setOnClickListener(v -> detailsInput.setStrikeThrough());
        detailsInputBtnBulletList.setOnClickListener(v -> detailsInput.setBullets());
        detailsInputBtnNumberList.setOnClickListener(v -> detailsInput.setNumbers());

        builder.setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            Button updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            updateButton.setOnClickListener(v -> {
                String newTitle = titleInput.getText().toString().trim();
                String newDate = dateInput.getText().toString().trim();
                //String newDetails = detailsInput.getText().toString().trim();
                String newDetails = detailsInput.getHtml();

                if (!newTitle.isEmpty() && !newDate.isEmpty() && !newDetails.isEmpty()) {
                    // Prepare updated model
                    JournalModel updatedJournal = new JournalModel(journal.getId(), newTitle, newDate, newDetails);

                    // Call API to update the journal
                    JournalApi journalApi = ApiClient.getJournalApi();
                    journalApi.updateJournal(deviceId, journal.getId(), updatedJournal).enqueue(new Callback<JournalModel>() {
                        @Override
                        public void onResponse(Call<JournalModel> call, Response<JournalModel> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                JournalModel updatedEntry = response.body();
                                // Update local data and notify adapter
                                journalList.set(position, updatedEntry);
                                notifyItemChanged(position);

                                if (updateListener != null) {
                                    updateListener.onEntryUpdated(updatedEntry);
                                }

                                Toast.makeText(context, "Entry updated successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(context, "Update failed. Try again.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<JournalModel> call, Throwable t) {
                            Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String currentDate = dateInput.getText().toString().trim();

            // Try to parse the current date from the input
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            try {
                Date parsedDate = sdf.parse(currentDate);
                if (parsedDate != null) {
                    calendar.setTime(parsedDate);
                }
            } catch (ParseException e) {
                e.printStackTrace(); // fallback to today if parsing fails
            }

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        calendar.set(selectedYear, selectedMonth, selectedDay);
                        dateInput.setText(sdf.format(calendar.getTime()));
                    },
                    year, month, day);
            datePickerDialog.show();
        });

        dialog.show();
    }

    private void showDeleteDialog(JournalModel journal, int position)
    {
        new AlertDialog.Builder(context)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this journal entry?")
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    JournalApi journalApi = ApiClient.getJournalApi();
                    journalApi.deleteJournal(deviceId, journal.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                journalList.remove(position);
                                notifyItemRemoved(position);

                                int journalListSize = journalList.size();
                                notifyItemRangeChanged(position, journalListSize);

                                if (updateListener != null) {
                                    updateListener.onEntryDeleted(journal);
                                }

                                // Update the journal entries count
                                if (context instanceof MainActivity) {
                                    ((MainActivity) context).updateTotalJournalsCount(journalListSize);
                                }

                                Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}