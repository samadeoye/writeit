package com.samwale.writeit;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.ImageView;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private JournalAdapter adapter;
    private JournalApi journalApi;
    private RecyclerView recyclerView;
    private TextView emptyJournalView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<JournalModel> allJournals = new ArrayList<>();

    private int currentPage = 1;
    private final int limit = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int journalListSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.journalListView);
        emptyJournalView = findViewById(R.id.emptyView);
        ImageView searchIcon = findViewById(R.id.searchIcon);
        ImageView filterIcon = findViewById(R.id.filterIcon);

        //swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        journalApi = ApiClient.getJournalApi();

        // Load journal entries from API
        loadJournals(currentPage);

        // Search icon action
        searchIcon.setOnClickListener(v -> showSearchDialog());

        // Filter icon action
        filterIcon.setOnClickListener(v -> showFilterOptions());

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !isLoading && !isLastPage) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        currentPage++;
                        loadJournals(currentPage);
                    }
                }
            }
        });

//        swipeRefreshLayout.setOnRefreshListener(() -> {
//            if (allJournals != null) {
//                allJournals.clear();
//            }
//            if (adapter != null) {
//                adapter.notifyDataSetChanged();
//            }
//
//            currentPage = 1;
//            isLastPage = false;
//            loadJournals(currentPage);
//            swipeRefreshLayout.setRefreshing(false);
//        });

        // Set up "Add New" Button
        Button addNewJournalEntryBtn = findViewById(R.id.addNewJournalEntryBtn);
        addNewJournalEntryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewEntryDialog(adapter); // Now adapter is accessible
            }
        });
    }

    // Add New Journal Entry Dialog
    private void showAddNewEntryDialog(JournalAdapter adapter) {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.save_journal_entry, null);

        // Find the input fields within the dialog view
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText detailsInput = dialogView.findViewById(R.id.detailsInput);
        TextView dateTextView = dialogView.findViewById(R.id.dateInput);

        // Set the dialog view
        //builder.setView(dialogView);
        builder.setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        // Create the dialog
        AlertDialog dialog = builder.create();

        // Set the save button click listener
        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                // Get the input values
                String title = titleInput.getText().toString().trim();
                String details = detailsInput.getText().toString().trim();

                // Validate inputs
                if (title.isEmpty() || details.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If no date is selected, set to current date
                String date = dateTextView.getText().toString().trim();
                if (date.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    date = sdf.format(new Date());  // Get the current date
                }

                // Now we have valid inputs, create a new JournalModel and add it to the adapter
                JournalModel newEntry = new JournalModel(title, date, details);
                //adapter.addJournalEntry(newEntry);

                journalApi.createJournal(newEntry).enqueue(new Callback<JournalModel>() {
                    @Override
                    public void onResponse(Call<JournalModel> call, Response<JournalModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JournalModel createdJournal = response.body();
                            // Add the new entry to the adapter
                            adapter.addJournalEntry(createdJournal);
                            allJournals.add(0, createdJournal);

                            // Update total journal entries
                            journalListSize++;
                            updateTotalJournalsCount(journalListSize);

                            // Re-check the empty state and update visibility
                            toggleEmptyView(adapter, findViewById(R.id.emptyView), findViewById(R.id.journalListView));

                            // Dismiss the dialog after a successful save
                            dialog.dismiss();

                            // Show a success message
                            Toast.makeText(MainActivity.this, "Journal entry saved successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            // Handle the case where the response is not successful
                            Toast.makeText(MainActivity.this, "Failed to save entry. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JournalModel> call, Throwable t) {
                        // Handle failure (e.g., no network connection)
                        Log.e("API_FAILURE", "Error: " + t.getMessage());
                        Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                // Dismiss the dialog after saving
                dialog.dismiss();
            });
        });

        // Set the OnClickListener to show the DatePickerDialog when the dateTextView is clicked
        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current date
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // Show DatePickerDialog
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(android.widget.DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                                // Format and display the selected date
                                calendar.set(selectedYear, selectedMonth, selectedDay);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                String formattedDate = sdf.format(calendar.getTime());
                                dateTextView.setText(formattedDate);  // Update TextView with selected date
                            }
                        }, year, month, day);

                // Show the DatePickerDialog
                datePickerDialog.show();
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void loadJournals(int page) {
        isLoading = true;
        journalApi.getJournals(page, limit).enqueue(new Callback<JournalResponse>() {
            @Override
            public void onResponse(Call<JournalResponse> call, Response<JournalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<JournalModel> journalList = response.body().getData();
                    int totalCount = response.body().getTotalCount();

                    if (adapter == null) {
                        //adapter = new JournalAdapter(MainActivity.this, new ArrayList<>());
                        /*
                        adapter = new JournalAdapter(MainActivity.this, new ArrayList<>(), updatedEntry -> {
                            // Find and update in allJournals
                            int allJournalsSize = allJournals.size();
                            for (int i = 0; i < allJournalsSize; i++) {
                                if (allJournals.get(i).getId() == (updatedEntry.getId())) {
                                    allJournals.set(i, updatedEntry);
                                    break;
                                }
                            }
                        });
                        */

                        adapter = new JournalAdapter(MainActivity.this, new ArrayList<>(), new JournalAdapter.OnEntryUpdatedListener() {
                            @Override
                            public void onEntryUpdated(JournalModel updatedEntry) {
                                for (int i = 0; i < allJournals.size(); i++) {
                                    if (allJournals.get(i).getId() == updatedEntry.getId()) {
                                        allJournals.set(i, updatedEntry);
                                        break;
                                    }
                                }
                            }

                            @Override
                            public void onEntryDeleted(JournalModel deletedEntry) {
                                for (int i = 0; i < allJournals.size(); i++) {
                                    if (allJournals.get(i).getId() == deletedEntry.getId()) {
                                        allJournals.remove(i);
                                        break;
                                    }
                                }
                            }
                        });

                        recyclerView.setAdapter(adapter);
                    }

                    allJournals.addAll(journalList);
                    adapter.addJournalEntries(journalList);

                    // Update total journal entries
                    updateTotalJournalsCount(totalCount);

                    if (journalList.size() < limit) {
                        isLastPage = true;
                    }

                    toggleEmptyView(adapter, emptyJournalView, recyclerView);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load journals", Toast.LENGTH_SHORT).show();
                }
                isLoading = false;
            }

            @Override
            public void onFailure(Call<JournalResponse> call, Throwable t) {
                isLoading = false;
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Journals");

        final EditText searchInput = new EditText(this);
        searchInput.setHint("Type to search...");
        builder.setView(searchInput);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String query = searchInput.getText().toString().trim();
            filterList(query);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        // Real-time filtering while typing
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        dialog.show();
    }

    private void filterList(String text) {
        List<JournalModel> filtered = new ArrayList<>();
        for (JournalModel entry : allJournals) {
            if (entry.getTitle().toLowerCase().contains(text.toLowerCase()) ||
                    entry.getDetails().toLowerCase().contains(text.toLowerCase())) {
                filtered.add(entry);
            }
        }
        adapter.setJournalEntries(filtered);
        // Update total journal entries
        updateTotalJournalsCount(filtered.size());
        toggleEmptyView(adapter, emptyJournalView, recyclerView);
    }

    private void showFilterOptions() {
        final String[] options = {"Date (Newest First)", "Date (Oldest First)", "Title (A–Z)", "Title (Z–A)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort Journals By")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Date (Newest First)
                            Collections.sort(allJournals, (o1, o2) -> o2.getDate().compareTo(o1.getDate()));
                            break;
                        case 1: // Date (Oldest First)
                            Collections.sort(allJournals, Comparator.comparing(JournalModel::getDate));
                            break;
                        case 2: // Title (A–Z)
                            Collections.sort(allJournals, Comparator.comparing(JournalModel::getTitle));
                            break;
                        case 3: // Title (Z–A)
                            Collections.sort(allJournals, (o1, o2) -> o2.getTitle().compareTo(o1.getTitle()));
                            break;
                    }
                    adapter.setJournalEntries(new ArrayList<>(allJournals));
                });
        builder.create().show();
    }

    private void toggleEmptyView(JournalAdapter adapter, TextView emptyView, RecyclerView recyclerView) {
        if (adapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Set total entries
    public void updateTotalJournalsCount(int newJournalListSize) {
        // Update current journal list size (in case call is from Delete action)
        journalListSize = newJournalListSize;
        TextView journalCountTextView = findViewById(R.id.journalCountTextView);
        journalCountTextView.setText("Total: " + newJournalListSize);
    }
}