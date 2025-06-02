package com.samwale.writeit;

import static org.junit.Assert.*;
import org.junit.Test;

public class JournalModelTest {

    @Test
    public void testTitleCapitalization() {
        JournalModel model = new JournalModel(1, "hello world", "2025-06-01", "details");
        assertEquals("Hello World", model.getTitle());

        JournalModel model2 = new JournalModel("multiple words here", "2025-06-02", "details");
        assertEquals("Multiple Words Here", model2.getTitle());
    }

    @Test
    public void testGetters() {
        JournalModel model = new JournalModel(10, "Test Title", "2025-06-03", "Some details");
        assertEquals(10, model.getId());
        assertEquals("Test Title", model.getTitle());
        assertEquals("2025-06-03", model.getDate());
        assertEquals("Some details", model.getDetails());
    }
}