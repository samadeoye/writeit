package com.samwale.writeit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import java.util.*;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class JournalAdapterTest {

    private List<JournalModel> initialList;
    private JournalAdapter.OnEntryUpdatedListener mockListener;
    private JournalAdapter.ImagePickerListener mockImagePickerListener;
    private JournalAdapter.ImageCaptureListener mockImageCaptureListener;
    private JournalAdapter.AudioPickerListener mockAudioPickerListener;
    private JournalAdapter adapter;

    @Before
    public void setUp() {
        // Set the initial list
        initialList = new ArrayList<>();

        // Mock the listeners
        mockListener = mock(JournalAdapter.OnEntryUpdatedListener.class);
        mockImagePickerListener = mock(JournalAdapter.ImagePickerListener.class);
        mockImageCaptureListener = mock(JournalAdapter.ImageCaptureListener.class);
        mockAudioPickerListener = mock(JournalAdapter.AudioPickerListener.class);

        // Instantiate adapter with mocks
        adapter = new JournalAdapter(
                null,
                initialList,
                mockListener,
                "dummyDeviceId",
                mockImagePickerListener,
                mockImageCaptureListener,
                mockAudioPickerListener
        );
    }

    @Test
    public void testAddJournalEntry_addsEntryAtTop() {
        JournalModel entry = new JournalModel("Title", "2025-06-01", "Details");

        adapter.addJournalEntry(entry);

        assertEquals(1, adapter.getItemCount());
        assertEquals(entry, initialList.get(0));
    }

    @Test
    public void testAddJournalEntries_addsAllToList() {
        List<JournalModel> newEntries = Arrays.asList(
                new JournalModel("New 1", "2025-06-02", "Content 1"),
                new JournalModel("New 2", "2025-06-03", "Content 2")
        );

        adapter.addJournalEntries(newEntries);

        assertEquals(2, adapter.getItemCount());
        assertTrue(initialList.containsAll(newEntries));
    }

    @Test
    public void testSetJournalEntries_replacesList() {
        initialList.add(new JournalModel("Old", "2024-12-12", "Old content"));

        List<JournalModel> newEntries = Arrays.asList(
                new JournalModel("Fresh", "2025-01-01", "Fresh")
        );

        adapter.setJournalEntries(newEntries);

        assertEquals(1, adapter.getItemCount());
        assertEquals("Fresh", initialList.get(0).getTitle());
    }

    @Test
    public void testClearAll_emptiesList() {
        initialList.add(new JournalModel("A", "B", "C"));

        adapter.clearAll();

        assertEquals(0, adapter.getItemCount());
        assertTrue(initialList.isEmpty());
    }
}