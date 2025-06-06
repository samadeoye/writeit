package com.samwale.writeit;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.ImageView;
import android.text.TextWatcher;
import android.text.Editable;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.activity.result.ActivityResultLauncher;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import android.util.Log;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jp.wasabeef.richeditor.RichEditor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private String deviceId;
    private JournalAdapter adapter;
    private JournalApi journalApi;
    private RecyclerView recyclerView;
    private TextView emptyJournalView;

    private List<JournalModel> allJournals = new ArrayList<>();

    private int currentPage = 1;
    private final int limit = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int journalListSize = 0;

    private JournalAdapter.ImagePickerListener imagePickerListener;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private RichEditor currentDetailsInput;

    private JournalAdapter.ImageCaptureListener imageCaptureListener;
    private ActivityResultLauncher<Uri> imageCaptureLauncher;
    private Uri photoUri;
    private JournalAdapter.AudioPickerListener audioPickerListener;
    private ActivityResultLauncher<Intent> audioPickerLauncher;

    public MainActivity() throws GeneralSecurityException, IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.journalListView);
        emptyJournalView = findViewById(R.id.emptyView);
        ImageView searchIcon = findViewById(R.id.searchIcon);
        ImageView filterIcon = findViewById(R.id.filterIcon);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        journalApi = ApiClient.getJournalApi();

        // Get device id from device shared storage
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String deviceIdString = "writeit_device_id";
        deviceId = sharedPreferences.getString(deviceIdString, null);

        // If device id cannot be found in storage, create a new one
        if (deviceId == null) {
            // UUID does not exist, generate a new one
            deviceId = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(deviceIdString, deviceId);  // Save the new UUID
            editor.apply();
        }

        // Picking image from gallery
        imagePickerListener = (position, detailsInput) -> {
            currentDetailsInput = detailsInput;

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (currentDetailsInput != null && selectedImageUri != null) {
                            try {
                                Uri persistentUri = JournalUtils.copyMediaToAppStorage(selectedImageUri, "image", MainActivity.this);
                                if (persistentUri != null) {
                                    //Toast.makeText(this, persistentUri.toString(), Toast.LENGTH_SHORT).show();
                                    currentDetailsInput.insertImage(persistentUri.toString(), "image", 200);
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to copy image", Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                String errorMessage = e.getMessage();
                                Toast.makeText(MainActivity.this, "Error copying image: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        );

        // Taking photo with camera
        imageCaptureListener = (position, detailsInput) -> {

            try {
                currentDetailsInput = detailsInput;

                File photoFile = JournalUtils.createImageFile(MainActivity.this);
                photoUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photoFile);

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
                }

                imageCaptureLauncher.launch(photoUri);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Could not create file for photo", Toast.LENGTH_SHORT).show();
            }
        };

        imageCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        if (currentDetailsInput != null && photoUri != null) {
                            try {
                                Uri persistentUri = JournalUtils.copyMediaToAppStorage(photoUri, "image", MainActivity.this);
                                if (persistentUri != null) {
                                    currentDetailsInput.insertImage(persistentUri.toString(), "image", 200);
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to copy image", Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                String errorMessage = e.getMessage();
                                Toast.makeText(MainActivity.this, "Error copying image: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        );

        // Picking audio from device
        audioPickerListener = (position, detailsInput) -> {
            currentDetailsInput = detailsInput;

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            audioPickerLauncher.launch(intent);
        };

        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();
                        try {
                            Uri persistentUri = JournalUtils.copyMediaToAppStorage(audioUri, "audio", MainActivity.this);
                            if (persistentUri != null) {
                                currentDetailsInput.insertAudio(persistentUri.toString());
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to copy audio", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            String errorMessage = e.getMessage();
                            Toast.makeText(MainActivity.this, "Error copying audio: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

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

        // Reload journal list on refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Clear old data
            if (allJournals != null) {
                allJournals.clear();
            }
            if (adapter != null) {
                adapter.clearAll();
            }

            isLastPage = false;
            currentPage = 1; // Reset pagination
            loadJournals(currentPage);

            swipeRefreshLayout.setRefreshing(false); // Stop the spinner after reload
        });

        // Set up "Add New" Button
        ImageButton addNewJournalEntryBtn = findViewById(R.id.addNewJournalEntryBtn);
        addNewJournalEntryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewEntryDialog(adapter);
            }
        });
    }

    // Add New Journal Entry Dialog
    private void showAddNewEntryDialog(JournalAdapter adapter) {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.save_journal_entry, null);

        // Position to place new entry
        int position = 0;

        // Find the input fields within the dialog view
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        TextView dateTextView = dialogView.findViewById(R.id.dateInput);
        //EditText detailsInput = dialogView.findViewById(R.id.detailsInput);
        RichEditor detailsInput = dialogView.findViewById(R.id.detailsInputRichEditor);
        Button detailsInputBtnBold = dialogView.findViewById(R.id.detailsInputBtnBold);
        Button detailsInputBtnItalics = dialogView.findViewById(R.id.detailsInputBtnItalics);
        Button detailsInputBtnUnderline = dialogView.findViewById(R.id.detailsInputBtnUnderline);
        Button detailsInputBtnStrikethrough = dialogView.findViewById(R.id.detailsInputBtnStrikethrough);
        ImageButton detailsInputBtnBulletList = dialogView.findViewById(R.id.detailsInputBtnBulletList);
        ImageButton detailsInputBtnNumberList = dialogView.findViewById(R.id.detailsInputBtnNumberList);

        ImageButton btnPickImage = dialogView.findViewById(R.id.btnPickImage);
        ImageButton btnCaptureImage = dialogView.findViewById(R.id.btnCaptureImage);
        ImageButton btnPickAudio = dialogView.findViewById(R.id.btnPickAudio);

        detailsInputBtnBold.setOnClickListener(v -> detailsInput.setBold());
        detailsInputBtnItalics.setOnClickListener(v -> detailsInput.setItalic());
        detailsInputBtnUnderline.setOnClickListener(v -> detailsInput.setUnderline());
        detailsInputBtnStrikethrough.setOnClickListener(v -> detailsInput.setStrikeThrough());
        detailsInputBtnBulletList.setOnClickListener(v -> detailsInput.setBullets());
        detailsInputBtnNumberList.setOnClickListener(v -> detailsInput.setNumbers());

        btnPickImage.setOnClickListener(v -> {
            if (imagePickerListener != null) {
                imagePickerListener.onPickImageRequested(position, detailsInput);
            }
        });

        btnCaptureImage.setOnClickListener(v -> {
            try {
                if (imageCaptureListener != null) {
                    imageCaptureListener.onCaptureImageRequested(position, detailsInput);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Could not create file for photo", Toast.LENGTH_SHORT).show();
            }
        });

        btnPickAudio.setOnClickListener(v -> {
            if (audioPickerListener != null) {
                audioPickerListener.onPickAudioRequested(position, detailsInput);
            }
        });

        // Set the dialog view
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
                //String details = detailsInput.getText().toString().trim();
                String details = detailsInput.getHtml();

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

                journalApi.createJournal(deviceId, newEntry).enqueue(new Callback<JournalModel>() {
                    @Override
                    public void onResponse(Call<JournalModel> call, Response<JournalModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JournalModel createdJournal = response.body();
                            // Add the new entry to the adapter
                            adapter.addJournalEntry(createdJournal);
                            allJournals.add(position, createdJournal);

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
        journalApi.getJournals(deviceId, page, limit).enqueue(new Callback<JournalResponse>() {
            @Override
            public void onResponse(Call<JournalResponse> call, Response<JournalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<JournalModel> journalList = response.body().getData();
                    int totalCount = response.body().getTotalCount();

                    if (adapter == null) {
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
                        }, deviceId, imagePickerListener, imageCaptureListener, audioPickerListener);

                        recyclerView.setAdapter(adapter);
                    }

                    allJournals.addAll(journalList);
                    adapter.addJournalEntries(journalList);

                    // Update total journal entries
                    updateTotalJournalsCount(totalCount);

                    if (journalList.size() < limit) {
                        isLastPage = true;
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load journals", Toast.LENGTH_SHORT).show();
                }
                isLoading = false;
                toggleEmptyView(adapter, emptyJournalView, recyclerView);
            }

            @Override
            public void onFailure(Call<JournalResponse> call, Throwable t) {
                isLoading = false;
                toggleEmptyView(adapter, emptyJournalView, recyclerView);
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Search Journals");

        final EditText searchInput = new EditText(MainActivity.this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
        boolean setEmpty = true;
        if (adapter != null) {
            if (adapter.getItemCount() > 0) {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                setEmpty = false;
            }
        }

        if (setEmpty)
        {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
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