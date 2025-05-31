package com.samwale.writeit;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JournalUtils {

    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;

        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder capitalized = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }

        return capitalized.toString().trim();
    }

    public static File createImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public static Uri copyMediaToAppStorage(Uri sourceUri, String mediaType, Context context) throws IOException {
        long maxFileSize = 2 * 1024 * 1024; // 2 MB
        // Check if file size is allowed
        if (isFileSizeAllowed(sourceUri, maxFileSize, context)) {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            String envDir, defaultExtension, filePrefix;
            switch (mediaType)
            {
                case "image":
                    envDir = Environment.DIRECTORY_PICTURES;
                    defaultExtension = "jpg";
                    filePrefix = "IMG_";
                    break;
                case "audio":
                    envDir = Environment.DIRECTORY_MUSIC;
                    defaultExtension = "mp3";
                    filePrefix = "AUD_";
                    break;

                default:
                    envDir = Environment.DIRECTORY_PICTURES;
                    defaultExtension = "jpg";
                    filePrefix = "IMG_";
                    break;
            }

            String fileExtension = getFileExtension(sourceUri, context, defaultExtension);
            String fileName = filePrefix + System.currentTimeMillis() + "." + fileExtension;
            File storageDir = context.getExternalFilesDir(envDir);
            File destFile = new File(storageDir, fileName);

            OutputStream outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", destFile);
        }
        else {
            throw new IOException("You cannot upload more than 2MB");
        }
    }

    public static boolean isFileSizeAllowed(Uri uri, long maxFileSize, Context context) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size <= maxFileSize;
            }
            cursor.close();
        }
        // If size unknown, return false (to be safe)
        return false;
    }

    public static String getFileExtension(Uri uri, Context context, String defaultExtension) {
        String extension = null;

        // Try to get extension from ContentResolver MIME type
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        // Fallback: try to get extension from URI path
        if (extension == null) {
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot != -1) {
                    extension = path.substring(lastDot + 1);
                }
            }
        }

        return extension != null ? extension : defaultExtension;
    }
}