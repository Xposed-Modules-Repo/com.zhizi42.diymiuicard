package com.zhizi42.diymiuicard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zhizi42.diymiuicard.databinding.ItemDiyCardLogoBinding;
import com.zhizi42.diymiuicard.databinding.ItemMyImageBinding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DIYCardLogoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    List<String> cardLogoList;
    Map<String, String> cardLogoNameMap;
    onDIYCardLogoClick onDIYCardLogoClick;

    public interface onDIYCardLogoClick {
        void onClick(int position);
    }

    DIYCardLogoAdapter(Context context, List<String> cardLogoList, onDIYCardLogoClick onClick) {
        this.context = context;
        this.cardLogoList = cardLogoList;
        cardLogoNameMap = new LinkedHashMap<>();
        cardLogoNameMap.put("jingjinji.png", "京津冀互联互通卡");
        cardLogoNameMap.put("shanghai.png", "上海公告交通卡 交通联合版");
        cardLogoNameMap.put("qvanchengtong.png", "泉城通");
        cardLogoNameMap.put("yinchuan.png", "银川交通一卡通");
        this.onDIYCardLogoClick = onClick;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemDiyCardLogoBinding binding = ItemDiyCardLogoBinding.inflate(inflater, parent, false);
        return new DIYCardLogoAdapter.DIYCardLogoViewHolder(binding);
    }

    public static class DIYCardLogoViewHolder extends RecyclerView.ViewHolder {
        public ConstraintLayout layout;
        public ImageView imageViewItemDIYCardLogo;
        public TextView textViewDIYCardLogoText;

        public DIYCardLogoViewHolder(@NonNull ItemDiyCardLogoBinding binding) {
            super(binding.getRoot());
            layout = binding.layoutDIYCardLogo;
            imageViewItemDIYCardLogo = binding.imageViewItemDIYCardLogo;
            textViewDIYCardLogoText = binding.textViewDIYCardLogoText;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DIYCardLogoViewHolder myHolder = (DIYCardLogoViewHolder) holder;

        if (position == 0) {
            Glide.with(context)
                    .load(R.drawable.ic_do_not)
                    .into(myHolder.imageViewItemDIYCardLogo);
            myHolder.textViewDIYCardLogoText.setText("空");
        } else {
            String cardLogoFileName = cardLogoList.get(position - 1);
            Glide.with(context)
                    .load("file:///android_asset/card_logo/" + cardLogoFileName)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_error)
                    .into(myHolder.imageViewItemDIYCardLogo);
            String cardLogoRealName = cardLogoNameMap.get(cardLogoFileName);
            if (cardLogoRealName == null) {
                cardLogoRealName = cardLogoFileName;
            }
            myHolder.textViewDIYCardLogoText.setText(cardLogoRealName);
        }

        myHolder.layout.setOnClickListener(v -> {
            onDIYCardLogoClick.onClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return cardLogoList.size() + 1;
    }
}