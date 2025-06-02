package com.samwale.writeit;

import org.junit.Test;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class JournalUtilsTest {

    @Test
    public void testCapitalizeWords() {
        assertEquals("Hello World", JournalUtils.capitalizeWords("hello world"));
        assertEquals("Hello World", JournalUtils.capitalizeWords("  hello   world  "));
        assertEquals("", JournalUtils.capitalizeWords(""));
        assertNull(JournalUtils.capitalizeWords(null));
        assertEquals("A", JournalUtils.capitalizeWords("a"));
        assertEquals("Test Case", JournalUtils.capitalizeWords("TEST CASE"));
    }

    @Test
    public void testIsFileSizeAllowed_fileWithinLimit() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Cursor mockCursor = mock(Cursor.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.query(eq(mockUri), any(), any(), any(), any())).thenReturn(mockCursor);
        when(mockCursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getLong(0)).thenReturn(1024L * 1024L); // 1 MB

        boolean allowed = JournalUtils.isFileSizeAllowed(mockUri, 2 * 1024 * 1024, mockContext);
        assertTrue(allowed);

        verify(mockCursor).close();
    }

    @Test
    public void testIsFileSizeAllowed_fileExceedsLimit() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Cursor mockCursor = mock(Cursor.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.query(eq(mockUri), any(), any(), any(), any())).thenReturn(mockCursor);
        when(mockCursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getLong(0)).thenReturn(3L * 1024 * 1024); // 3 MB

        boolean allowed = JournalUtils.isFileSizeAllowed(mockUri, 2 * 1024 * 1024, mockContext);
        assertFalse(allowed);

        verify(mockCursor).close();
    }

    @Test
    public void testIsFileSizeAllowed_cursorNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.query(eq(mockUri), any(), any(), any(), any())).thenReturn(null);

        boolean allowed = JournalUtils.isFileSizeAllowed(mockUri, 2 * 1024 * 1024, mockContext);
        assertFalse(allowed);
    }

    @Test
    public void testGetFileExtension_knownMimeType() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn("image/jpeg");

        // Call method
        String ext = JournalUtils.getFileExtension(mockUri, mockContext, "default");

        // Verify extension from MIME type map
        assertEquals("jpg", ext);
    }

    @Test
    public void testGetFileExtension_unknownMimeType_fallbackToUriPath() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn("application/unknown");
        when(mockUri.getPath()).thenReturn("/some/path/file.png");

        // Call method
        String ext = JournalUtils.getFileExtension(mockUri, mockContext, "default");

        // Verify fallback to URI path extension
        assertEquals("png", ext);
    }

    @Test
    public void testGetFileExtension_nullMimeType_fallbackToUriPath() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn(null);
        when(mockUri.getPath()).thenReturn("/some/path/file.mp3");

        // Call method
        String ext = JournalUtils.getFileExtension(mockUri, mockContext, "default");

        // Verify fallback to URI path extension
        assertEquals("mp3", ext);
    }

    @Test
    public void testGetFileExtension_noExtensionInUri_returnsDefault() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn(null);
        when(mockUri.getPath()).thenReturn("/some/path/file"); // No extension

        // Call method
        String ext = JournalUtils.getFileExtension(mockUri, mockContext, "default");

        // Verify fallback to default extension
        assertEquals("default", ext);
    }

    @Test
    public void testGetFileExtension_withNullUriPath_returnsDefault() {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = mock(Uri.class);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn(null);
        when(mockUri.getPath()).thenReturn(null);

        // Call method
        String ext = JournalUtils.getFileExtension(mockUri, mockContext, "default");

        // Verify fallback to default extension
        assertEquals("default", ext);
    }
}
