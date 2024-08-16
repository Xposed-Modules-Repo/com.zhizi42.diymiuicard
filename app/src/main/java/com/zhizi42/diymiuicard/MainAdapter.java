package com.zhizi42.diymiuicard;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.zhizi42.diymiuicard.databinding.ImageInputDialogBinding;
import com.zhizi42.diymiuicard.databinding.ItemMainCardBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<String> cardUrlList;
    Context context;

    MainAdapter(Context context) {
        this.context = context;
    }

    public void setList(ArrayList<String> cardUrlList) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        cardUrlList.sort(new CustomComparator(sharedPreferences));
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
        if (!imageName.isEmpty()) {
            if (imageName.startsWith("https://") || imageName.startsWith("http://")) {
                Glide.with(context)
                        .load(imageName)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_error)
                        .into(mainViewHolder.imageViewImage);
            } else {
                @SuppressLint("SdCardPath") String path = "/data/data/com.zhizi42.diymiuicard/files/images/" + imageName;
                Glide.with(context)
                        .load(new File(path))
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_error)
                        .into(mainViewHolder.imageViewImage);
            }
        } else {
            mainViewHolder.imageViewImage.setImageResource(R.drawable.ic_image);
        }

        mainViewHolder.imageViewCard.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle(R.string.card_url_title)
                .setMessage(cardUrl)
                .setNeutralButton(R.string.dialog_button_copy, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clipData = ClipData.newPlainText("image url", cardUrl);
                        clipboard.setPrimaryClip(clipData);
                    }
                })
                .setPositiveButton(R.string.confirm, null)
                .show());

        mainViewHolder.imageViewImage.setOnClickListener(v -> {
            ImageInputDialogBinding binding = ImageInputDialogBinding.inflate(LayoutInflater.from(context));
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.input_image_name_title)
                    .setView(binding.getRoot())
                    .setNeutralButton(R.string.input_image_button_tips_title, null)
                    .setNegativeButton(R.string.input_image_button_clear, ((dialog, which) -> {
                        sharedPreferences.edit().remove(cardUrl).apply();
                        notifyItemChanged(position);
                    }))
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        String imageName0 = binding.editTextText.getText().toString();
                        sharedPreferences.edit().putString(cardUrl, imageName0).apply();
                        notifyItemChanged(position);
                    })
                    .create();


            RecyclerView recyclerView = binding.recyclerViewMyImages;
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            MyImagesAdapter adapter = new MyImagesAdapter(context, cardUrl, name -> {
                sharedPreferences.edit().putString(cardUrl, name).apply();
                alertDialog.dismiss();
                notifyItemChanged(position);
            });
            recyclerView.setAdapter(adapter);
            binding.imageButtonAdd.setOnClickListener(view -> ((MainActivity)context).selectImageAdd(adapter));
            binding.editTextText.setText(sharedPreferences.getString(cardUrl, ""));

            alertDialog.show();
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(view -> new AlertDialog.Builder(context)
                            .setTitle(R.string.input_image_button_tips_title)
                            .setMessage(R.string.input_image_tips_text)
                            .setPositiveButton(R.string.confirm, null)
                            .show());
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

    static class CustomComparator implements Comparator<String> {
        private final SharedPreferences sharedPreferences;

        public CustomComparator(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public int compare(String s1, String s2) {
            boolean isFirstGroup1 = isFirstGroup(s1); // 判断第一组
            boolean isFirstGroup2 = isFirstGroup(s2);

            if (isFirstGroup1 && !isFirstGroup2) {
                return -1; // s1 在前
            } else if (!isFirstGroup1 && isFirstGroup2) {
                return 1; // s2 在前
            } else {
                // 同组按字母顺序排序
                return s1.compareTo(s2);
            }
        }

        private boolean isFirstGroup(String s) {
            return !sharedPreferences.getString(s, "").isEmpty();
        }
    }
}
