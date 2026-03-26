package com.zhizi42.diymiuicard;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zhizi42.diymiuicard.databinding.ImageInputDialogBinding;
import com.zhizi42.diymiuicard.databinding.ItemCardOriginalBinding;
import com.zhizi42.diymiuicard.databinding.ItemCardReplacedBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class MainCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ArrayList<String> cardUrlList = new ArrayList<>();
    Context context;
    SharedPreferences sharedPreferences;
    boolean isBlacklist = false;

    private static final int VIEW_ORIGINAL = 0;
    private static final int VIEW_REPLACED = 1;

    MainCardAdapter(Context context) {
        this.context = context;
    }

    MainCardAdapter(Context context, boolean isBlacklist) {
        this.context = context;
        this.isBlacklist = isBlacklist;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        sharedPreferences =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String k = isBlacklist ? "black_card_url_set" : "all_card_url_set";
        Set<String> cardUrlSet = sharedPreferences.getStringSet(k, new HashSet<>());
        cardUrlList = new ArrayList<>(cardUrlSet);
        cardUrlList.sort(new CustomComparator(sharedPreferences));
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        String imageName = sharedPreferences.getString(cardUrlList.get(position), "");
        if (imageName.isEmpty()) {
            return VIEW_ORIGINAL;
        } else {
            return VIEW_REPLACED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_REPLACED) {
            ItemCardReplacedBinding replacedBinding = ItemCardReplacedBinding.inflate(inflater, parent, false);
            return new CardReplacedViewHolder(replacedBinding);
        } else {
            ItemCardOriginalBinding originalBinding = ItemCardOriginalBinding.inflate(inflater, parent, false);
            return new CardOriginalViewHolder(originalBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ImageView imageViewCard;
        ImageView imageViewImage;
        if (holder instanceof CardOriginalViewHolder) {
            CardOriginalViewHolder cardOriginalViewHolder = (CardOriginalViewHolder) holder;
            imageViewCard = cardOriginalViewHolder.imageViewCard;
            imageViewImage = null;
        } else if (holder instanceof CardReplacedViewHolder) {
            CardReplacedViewHolder cardReplacedViewHolder = (CardReplacedViewHolder) holder;
            imageViewCard = cardReplacedViewHolder.imageViewCard;
            imageViewImage = cardReplacedViewHolder.imageViewImage;
        } else {
            return;
        }
        String cardUrl = cardUrlList.get(position);//获取当前卡片链接
        //加载当前卡片链接到组件
        Glide.with(context)
                .load(cardUrl)
                .placeholder(R.drawable.ic_card)
                .error(R.drawable.ic_error)
                .into(imageViewCard);

        //长按卡片图片弹出图片链接
        imageViewCard.setOnLongClickListener(v -> {
            showCardUrl(cardUrl);
            return true;
        });
        if (! isBlacklist) {
            imageViewCard.setOnClickListener(v -> showSelectDialog(cardUrl, null));
        }

        if (holder instanceof CardReplacedViewHolder) {
            if (imageViewImage == null) {
                return;
            }
            //如果diy图片不为空就加载到组件
            String imageName = sharedPreferences.getString(cardUrl, "");
            if (!imageName.isEmpty()) {
                if (imageName.startsWith("https://") || imageName.startsWith("http://")) {
                    Glide.with(context)
                            .load(imageName)
                            .placeholder(R.drawable.ic_image)
                            .error(R.drawable.ic_error)
                            .into(imageViewImage);
                } else {
                    @SuppressLint("SdCardPath") String path = "/data/data/com.zhizi42.diymiuicard/files/images/" + imageName;
                    Glide.with(context)
                            .load(new File(path))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(R.drawable.ic_image)
                            .error(R.drawable.ic_error)
                            .into(imageViewImage);
                }
            } else {
                imageViewImage.setImageResource(R.drawable.ic_image);
            }

            imageViewImage.setOnClickListener(v -> showSelectDialog(cardUrl, imageViewImage));
        }
    }

    //弹出卡片链接
    private void showCardUrl(String url) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.card_url_title)
                .setMessage(url)
                .setNeutralButton(R.string.dialog_button_copy, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clipData = ClipData.newPlainText("image url", url);
                        clipboard.setPrimaryClip(clipData);
                    }
                })
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    //弹出选择图片dialog
    private void showSelectDialog(String cardUrl, ImageView imageViewImage) {

        //获取要显示的view binding
        ImageInputDialogBinding binding = ImageInputDialogBinding.inflate(LayoutInflater.from(context));

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.input_image_name_title)
                .setView(binding.getRoot())
                .setNeutralButton(R.string.input_image_button_tips_title, null)
                .setNegativeButton(R.string.input_image_button_clear, ((dialog, which) -> {
                    clearMyImage(cardUrl);
                    refresh();
                }))
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String myImageName = binding.editTextText.getText().toString();

                    //将军的恩情还不完
                    if (myImageName.equals("rechrd what")) {
                        if (imageViewImage != null) {
                            imageViewImage.setImageResource(R.drawable.general);
                            return;
                        }
                    }

                    setMyImage(cardUrl, myImageName);
                    refresh();
                })
                .create();

        //显示我有的图片列表
        RecyclerView recyclerView = binding.recyclerViewMyImages;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        MyImagesAdapter adapter = new MyImagesAdapter(context, name -> {
            setMyImage(cardUrl, name);
            alertDialog.dismiss();
            refresh();
        });
        recyclerView.setAdapter(adapter);
        binding.imageButtonAdd.setOnClickListener(view -> ((MainActivity) context).selectImageAdd(adapter));
        binding.editTextText.setText(sharedPreferences.getString(cardUrl, ""));

        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(view -> new AlertDialog.Builder(context)
                        .setTitle(R.string.input_image_button_tips_title)
                        .setMessage(R.string.input_image_tips_text)
                        .setPositiveButton(R.string.confirm, null)
                        .show());
    }

    //设置指定卡片对应的diy图片关系
    private void setMyImage(String cardUrl, String imageUrl) {
        sharedPreferences.edit().putString(cardUrl, imageUrl).apply();
        SharedPreferences serviceSharedPreferences = MyXposedService.getSharedPreferences();
        if (serviceSharedPreferences != null) {
            serviceSharedPreferences.edit().putString(cardUrl, imageUrl).apply();
        } else {
            Toast.makeText(context, "xposed service not exist", Toast.LENGTH_LONG).show();
        }
    }

    //清除指定卡片对应的diy图片关系
    private void clearMyImage(String cardUrl) {
        sharedPreferences.edit().remove(cardUrl).apply();
        SharedPreferences serviceSharedPreferences = MyXposedService.getSharedPreferences();
        if (serviceSharedPreferences != null) {
            serviceSharedPreferences.edit().remove(cardUrl).apply();
        } else {
            Toast.makeText(context, "xposed service not exist", Toast.LENGTH_LONG).show();
        }
    }

    //长按删除并添加到黑名单/从黑名单删除
    public void archive(int position) {
        String cardUrl = cardUrlList.get(position);//获取滑动位置的图片url
        cardUrlList.remove(position);//删除数据源的滑动位置的url
        Set<String> cardUrlBlackSet = new HashSet<>(sharedPreferences.getStringSet("black_card_url_set", new HashSet<>()));//获取设置储存的黑名单列表
        if (! isBlacklist) {//如果是主界面
            Set<String> cardUrlSet = new HashSet<>(sharedPreferences.getStringSet("all_card_url_set", new HashSet<>()));//获取设置储存的卡片列表并删除当前条目
            cardUrlSet.remove(cardUrl);
            cardUrlBlackSet.add(cardUrl);//添加当前条目到黑名单
            //保存设置
            sharedPreferences.edit().putStringSet("all_card_url_set", cardUrlSet).apply();
            clearMyImage(cardUrl);
        } else {//如果是黑名单界面
            cardUrlBlackSet.remove(cardUrl);//删除黑名单的当前条目
        }
        //保存设置
        sharedPreferences.edit().putStringSet("black_card_url_set", cardUrlBlackSet).apply();
        //获取xp的设置并添加黑名单卡片
        SharedPreferences serviceSharedPreferences = MyXposedService.getSharedPreferences();
        if (serviceSharedPreferences != null) {
            serviceSharedPreferences.edit().putStringSet("black_card_url_set", cardUrlBlackSet).apply();
        } else {
            Toast.makeText(context, "xposed service not exist", Toast.LENGTH_LONG).show();
        }
        notifyItemRemoved(position);
    }

    public static class CardOriginalViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageViewCard;

        public CardOriginalViewHolder(@NonNull ItemCardOriginalBinding binding) {
            super(binding.getRoot());

            imageViewCard = binding.imageViewCardOriginal;
        }
    }

    public static class CardReplacedViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageViewCard;
        public ImageView imageViewImage;

        public CardReplacedViewHolder(@NonNull ItemCardReplacedBinding binding) {
            super(binding.getRoot());

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
