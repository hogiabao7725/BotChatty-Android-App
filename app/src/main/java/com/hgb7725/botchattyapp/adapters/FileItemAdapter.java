package com.hgb7725.botchattyapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hgb7725.botchattyapp.R;
import com.hgb7725.botchattyapp.models.FileItem;
import com.hgb7725.botchattyapp.utilities.FileUtils;

import java.util.List;

/**
 * Adapter for displaying file items in the user profile
 */
public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.FileViewHolder> {
    
    private final List<FileItem> fileItems;
    private final Context context;

    public FileItemAdapter(Context context, List<FileItem> fileItems) {
        this.context = context;
        this.fileItems = fileItems;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileItems.get(position);
        
        // Set file name
        holder.fileName.setText(fileItem.getFileName());
        
        // Set file icon based on file extension using the existing FileUtils class
        holder.fileIcon.setImageResource(FileUtils.getFileIconRes(fileItem.getFileName()));
        
        // Handle download button click
        holder.downloadIcon.setOnClickListener(v -> 
            FileUtils.confirmAndDownloadFile(context, fileItem.getFileUrl(), fileItem.getFileName())
        );
        
        // Make the entire item clickable
        holder.itemView.setOnClickListener(v -> 
            FileUtils.confirmAndDownloadFile(context, fileItem.getFileUrl(), fileItem.getFileName())
        );
    }

    @Override
    public int getItemCount() {
        return fileItems.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        ImageView fileIcon;
        ImageView downloadIcon;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.textFileName);
            fileIcon = itemView.findViewById(R.id.imageFileIcon);
            downloadIcon = itemView.findViewById(R.id.imageDownload);
        }
    }
}