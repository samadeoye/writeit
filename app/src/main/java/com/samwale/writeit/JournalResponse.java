package com.samwale.writeit;

import java.util.List;

public class JournalResponse {
    private String status;
    private List<JournalModel> data;
    private int totalCount;

    // Getters
    public String getStatus() {
        return status;
    }

    public List<JournalModel> getData() {
        return data;
    }

    public int getTotalCount() {
        return totalCount;
    }
}