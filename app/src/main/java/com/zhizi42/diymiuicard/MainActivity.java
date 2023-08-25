package com.zhizi42.diymiuicard;

import androidx.annotation.Keep;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.zhizi42.diymiuicard.databinding.ActivityMainBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private MainAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (isOn()) {
            binding.textView.setText(R.string.text_enable);
        }

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("first_run", true)) {
            howToUse();
            sharedPreferences.edit().putBoolean("first_run", false).apply();
        }
        Set<String> cardUrlSet = sharedPreferences.getStringSet("all_card_url_set", new HashSet<>());
        ArrayList<String> cardUrlList = new ArrayList<>(cardUrlSet);

        RecyclerView recyclerView = binding.recyclerView;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MainAdapter(cardUrlList, this);
        recyclerView.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshList();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        binding.imageButtonSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
        binding.imageButtonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm)
                    .setMessage(R.string.confirm_delete_msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("all_card_url_set");
                        editor.apply();
                        refreshList();
                    })
                    .show();
        });

        MultiprocessSharedPreferences.setAuthority("com.zhizi42.diymiuicard.provider");
        File file = new File("/data/data/com.zhizi42.diymiuicard/files/images/");
        if (! file.exists()) {
            file.mkdirs();
        }
    }

    public void refreshList() {
        Set<String> cardUrlSetNew = sharedPreferences.getStringSet("all_card_url_set", new HashSet<>());
        adapter.setList(new ArrayList<>(cardUrlSetNew));
        adapter.notifyDataSetChanged();
    }

    public void howToUse() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_use_title)
                .setMessage(R.string.how_to_use_msg)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    @Keep
    boolean isOn() {
        return false;
    }
}