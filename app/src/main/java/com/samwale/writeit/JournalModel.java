package com.samwale.writeit;

public class JournalModel {
    private int id;
    private String title;
    private String date;
    private String details;

    // Constructor
    public JournalModel(int id, String title, String date, String details) {
        this.id = id;
        this.title = JournalUtils.capitalizeWords(title);
        this.date = date;
        this.details = details;
    }

    // Constructor without id (for creating new entries)
    public JournalModel(String title, String date, String details) {
        this.title = JournalUtils.capitalizeWords(title);
        this.date = date;
        this.details = details;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getDetails() {
        return details;
    }
}