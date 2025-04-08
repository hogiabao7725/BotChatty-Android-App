package com.hgb7725.botchattyapp.utilities;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.hgb7725.botchattyapp.R;

import java.io.File;

public class FileUtils {

    /**
     * Returns the appropriate drawable resource ID for the file icon
     * based on the file's extension. Falls back to generic file icon.
     */
    public static int getFileIconRes(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return R.drawable.ic_file; // default icon if no extension
        }

        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        switch (extension) {
            case "pdf": return R.drawable.ic_pdf;
            case "doc":
            case "docx": return R.drawable.ic_doc;
            case "ppt":
            case "pptx": return R.drawable.ic_ppt;
            case "xls":
            case "xlsx": return R.drawable.ic_excel;
            case "zip":
            case "rar": return R.drawable.ic_zip;
            case "jpg":
            case "jpeg": return R.drawable.ic_jpg;
            case "png": return R.drawable.ic_png;
            case "gif": return R.drawable.ic_gif;
            case "mp3":
            case "wav": return R.drawable.ic_mp3;
            case "svg": return R.drawable.ic_svg;
            case "txt": return R.drawable.ic_txt;
            default: return R.drawable.ic_file;
        }
    }

    /**
     * Gets the display file name from a content Uri.
     * Falls back to extracting from path, or returns "unknown_file".
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        if (result == null && uri.getPath() != null) {
            String path = uri.getPath();
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                result = path.substring(cut + 1);
            }
        }

        return result != null ? result : "unknown_file";
    }

    /**
     * Gets the file extension from the Uri (based on MIME type or path).
     * Returns "" if extension not found.
     */
    public static String getFileExtension(Context context, Uri uri) {
        String extension = null;

        if (uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = context.getContentResolver().getType(uri);
            if (type != null) {
                extension = mime.getExtensionFromMimeType(type);
            }
        }

        if (extension == null && uri.getPath() != null) {
            String path = uri.getPath();
            int dot = path.lastIndexOf('.');
            if (dot != -1 && dot < path.length() - 1) {
                extension = path.substring(dot + 1);
            }
        }

        return extension != null ? extension.toLowerCase() : "";
    }

    /**
     * Shortens a filename by truncating the middle part while keeping the file extension.
     * If the resulting string is shorter than the target length, it adds spaces after the ellipsis
     * to visually fill a fixed-width TextView for better UI alignment.
     *
     * @param fileName     The original file name (e.g. "super_long_filename.pdf")
     * @param targetLength The total number of characters desired (including extension and ellipsis)
     * @return A string that fits within the target length, with middle ellipsis and optional padding
     */
    public static String shortenMiddleToFit(String fileName, int targetLength) {
        if (fileName == null) return "";

        // Split filename into name and extension
        int dotIndex = fileName.lastIndexOf(".");
        String ext = dotIndex != -1 ? fileName.substring(dotIndex) : "";
        String name = dotIndex != -1 ? fileName.substring(0, dotIndex) : fileName;

        // Base = length of extension + 3 dots (...)
        int baseLength = ext.length() + 3;
        int keep = (targetLength - baseLength) / 2;

        // If filename is already short, pad it with spaces to match target length
        if (keep <= 0 || name.length() <= targetLength) {
            return String.format("%-" + targetLength + "s", fileName);
        }

        // Create shortened version with ellipsis in the middle
        String shortened = name.substring(0, keep) + "..." + name.substring(name.length() - keep) + ext;

        // If the shortened string is still shorter than target, pad with spaces after the ellipsis
        int missing = targetLength - shortened.length();
        if (missing > 0) {
            StringBuilder sb = new StringBuilder(shortened);
            for (int i = 0; i < missing; i++) {
                sb.insert(keep + 3, " "); // insert space after "..."
            }
            shortened = sb.toString();
        }

        return shortened;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void downloadAndOpenFile(Context context, String fileUrl, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName);
        request.setDescription("Downloading file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                    Uri fileUri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", file);

                    Intent openIntent = new Intent(Intent.ACTION_VIEW);
                    openIntent.setDataAndType(fileUri, getMimeType(fileName));
                    openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        ctx.startActivity(openIntent);
                    } catch (Exception e) {
                        Toast.makeText(ctx, "No app to open this file", Toast.LENGTH_SHORT).show();
                    }

                    ctx.unregisterReceiver(this); // cleanup
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(onComplete, filter);
        }
    }

    public static String getMimeType(String fileName) {
        String type = "*/*";
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type != null ? type : "*/*";
    }

    public static void confirmAndDownloadFile(Context context, String fileUrl, String fileName) {
        new AlertDialog.Builder(context)
                .setTitle("Download file")
                .setMessage("Do you want to download this file?\n\n" + fileName)
                .setPositiveButton("Yes", (dialog, which) -> {
                    downloadAndOpenFile(context, fileUrl, fileName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
