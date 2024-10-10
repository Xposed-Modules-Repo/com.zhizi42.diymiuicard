package com.zhizi42.diymiuicard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zhizi42.diymiuicard.databinding.ItemMyImageBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class MyImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    ArrayList<File> fileList;
    OnImageClick onImageClick;

    public interface OnImageClick {
        void onClick(String name);
    }

    MyImagesAdapter(Context context, OnImageClick onImageClick) {
        this.context = context;
        this.onImageClick = onImageClick;
        this.fileList = new ArrayList<>();
        refreshFilelist();
    }

    void refreshFilelist() {
        File[] files;
        files = ((MainActivity) context).getMyImagesFolder().listFiles();
        if (files != null) {
            Collections.addAll(fileList, files);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMyImageBinding binding = ItemMyImageBinding.inflate(inflater, parent, false);
        return new MyImagesViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyImagesViewHolder myImagesViewHolder = (MyImagesViewHolder) holder;
        ConstraintLayout layout = myImagesViewHolder.constraintLayout;
        ImageView imageView = myImagesViewHolder.imageView;
        Glide.with(context)
                .load(fileList.get(position))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_error)
                .into(imageView);
        layout.setOnClickListener(view -> onImageClick.onClick(
                fileList.get(position).getName()
        ));
        layout.setOnLongClickListener(view -> {
            ArrayList<String> cmdStringList = new ArrayList<>();
            cmdStringList.add(
                    String.format("rm \"/data/data/com.miui.tsmclient/files/images/%s\"",
                            fileList.get(position).getName()));
            if (Utils.executeShell(cmdStringList)) {
                if (fileList.get(position).delete()) {
                    fileList.remove(position);
                    notifyDataSetChanged();
                    return true;
                }
            }
            return false;
        });

    }

    public static class MyImagesViewHolder extends RecyclerView.ViewHolder {
        public ConstraintLayout constraintLayout;
        public ImageView imageView;

        public MyImagesViewHolder(@NonNull ItemMyImageBinding binding) {
            super(binding.getRoot());

            constraintLayout = binding.layoutMyImage;
            imageView = binding.imageView;
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void addDone(File file) {
        fileList.add(file);
        notifyItemInserted(getItemCount());
    }
}
