package com.zhizi42.diymiuicard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zhizi42.diymiuicard.databinding.ImageInputDialogBinding;
import com.zhizi42.diymiuicard.databinding.ItemMainCardBinding;

import java.io.File;
import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<String> cardUrlList;
    Context context;

    MainAdapter(ArrayList<String> cardUrlList, Context context) {
        this.cardUrlList = cardUrlList;
        this.context = context;
    }

    public void setList(ArrayList<String> cardUrlList) {
        this.cardUrlList = cardUrlList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMainCardBinding binding = ItemMainCardBinding.inflate(inflater, parent, false);
        return new MainViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MainViewHolder mainViewHolder = (MainViewHolder) holder;
        String cardUrl = cardUrlList.get(position);
        Glide.with(context)
                .load(cardUrl)
                .placeholder(R.drawable.ic_card)
                .error(R.drawable.ic_error)
                .into(mainViewHolder.imageViewCard);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String imageName = sharedPreferences.getString(cardUrl, "");
        if (! imageName.equals("")) {
            if (imageName.startsWith("https://") || imageName.startsWith("http://")) {
                Glide.with(context)
                        .load(imageName)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_error)
                        .into(mainViewHolder.imageViewImage);
            } else {
                String path = "/data/data/com.zhizi42.diymiuicard/files/images/" + imageName;
                Glide.with(context)
                        .load(new File(path))
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_error)
                        .into(mainViewHolder.imageViewImage);
            }
        } else {
            mainViewHolder.imageViewImage.setImageResource(R.drawable.ic_image);
        }

        mainViewHolder.imageViewCard.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.card_url_title)
                    .setMessage(cardUrl)
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        });

        mainViewHolder.imageViewImage.setOnClickListener(v -> {
            ImageInputDialogBinding binding = ImageInputDialogBinding.inflate(LayoutInflater.from(context));

            new AlertDialog.Builder(context)
                    .setTitle(R.string.input_image_name_title)
                    .setView(binding.getRoot())
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        String imageName0 = binding.editTextText.getText().toString();
                        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                                .edit()
                                .putString(cardUrl, imageName0)
                                .apply();
                        notifyItemChanged(position);
                    })
                    .show();
        });
    }


    public static class MainViewHolder extends RecyclerView.ViewHolder {
        public ConstraintLayout layout;
        public ImageView imageViewCard;
        public ImageView imageViewImage;

        public MainViewHolder(@NonNull ItemMainCardBinding binding) {
            super(binding.getRoot());

            layout = binding.itemMainCardLayout;
            imageViewCard = binding.imageViewCard;
            imageViewImage = binding.imageViewImage;
        }
    }

    @Override
    public int getItemCount() {
        return cardUrlList.size();
    }
}
