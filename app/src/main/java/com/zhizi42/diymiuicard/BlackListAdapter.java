package com.zhizi42.diymiuicard;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zhizi42.diymiuicard.databinding.ItemBlackListImageBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BlackListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<String> blackUrlList;
    Context context;

    BlackListAdapter(Context context) {
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        SharedPreferences preferences = context.getSharedPreferences("settings", MODE_PRIVATE);
        Set<String> set = preferences.getStringSet("black_card_url_set", new HashSet<>());
        blackUrlList = new ArrayList<>(set);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBlackListImageBinding binding = ItemBlackListImageBinding.inflate(inflater, parent, false);
        return new BlackListAdapter.BlackListViewHolder(binding);
    }

    public static class BlackListViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;

        public BlackListViewHolder(@NonNull ItemBlackListImageBinding binding) {
            super(binding.getRoot());

            imageView = binding.imageViewBlackList;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BlackListViewHolder blackListViewHolder = (BlackListViewHolder) holder;
        Glide.with(context)
                .load(blackUrlList.get(position))
                .placeholder(R.drawable.ic_card)
                .error(R.drawable.ic_error)
                .into(blackListViewHolder.imageView);
        SharedPreferences preferences =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        blackListViewHolder.imageView.setOnLongClickListener(v -> {
            Set<String> cardUrlSet = new HashSet<>(preferences.getStringSet("black_card_url_set", new HashSet<>()));
            cardUrlSet.remove(blackUrlList.get(position));
            preferences.edit().putStringSet("black_card_url_set", cardUrlSet).commit();
            refresh();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return blackUrlList.size();
    }
}
