package com.zhizi42.diymiuicard;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.zhizi42.diymiuicard.databinding.ActivityMainBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private MainAdapter adapter;
    private File myImagesFolder;
    private MyImagesAdapter myImagesAdapter;
    private ActivityResultLauncher<Intent> startSelectImageActivity;

    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (isOn()) {
            binding.textView.setText(R.string.text_enable);
        }

        SharedPreferences preferencesPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Utils.initSetting(preferencesPreferences);
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("first_run", true)) {
            howToUse();
            sharedPreferences.edit().putBoolean("first_run", false).apply();
        }

        RecyclerView recyclerView = binding.recyclerView;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MainAdapter(this);
        adapter.refresh();
        recyclerView.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            adapter.refresh();
            binding.swipeRefreshLayout.setRefreshing(false);
        });
        myImagesFolder = new File("/data/data/com.zhizi42.diymiuicard/files/images/");
        if (! myImagesFolder.exists()) {
            if (! myImagesFolder.mkdirs()) {
                Toast.makeText(this, "create my app images folder failed!", Toast.LENGTH_LONG).show();
            }
        }

        startSelectImageActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                o -> {
                    if (o.getResultCode() == Activity.RESULT_OK && o.getData() != null) {
                        myImagesAdapter.addDone(
                                new File(
                                        myImagesFolder,
                                        o.getData().getStringExtra("FILE_NAME"))
                        );
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.select_image_done_dialog_title)
                                .setMessage(R.string.select_image_done_dialog_text)
                                .setNeutralButton(R.string.select_image_done_dialog_clear_cache,
                                        (dialog, which) -> Utils.clearCache(this))
                                .setPositiveButton(R.string.confirm, null)
                                .show();
                    }
                }
        );

        if (Utils.sharedPreferences.getBoolean("debug", false)) {
            Log.e("diy miui card of zhizi42", "init main activity");
        }
    }

    public void howToUse() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_use_title)
                .setMessage(R.string.how_to_use_msg)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    public void onSettingClick(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void onDeleteClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_delete_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("all_card_url_set");
                    editor.apply();
                    adapter.refresh();
                })
                .show();
    }

    public void selectImageAdd(MyImagesAdapter adapter) {
        myImagesAdapter = adapter;
        if (sharedPreferences.getBoolean("first_add_image", true)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.input_image_dialog_confirm_root_title)
                    .setMessage(R.string.input_image_dialog_confirm_root_msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        sharedPreferences.edit().putBoolean("first_add_image", false).apply();
                        startSelectActivity();
                    })
                    .show();
        } else {
            startSelectActivity();
        }
    }

    public void startSelectActivity() {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startSelectImageActivity.launch(intent);
    }

    public File getMyImagesFolder() {
        return myImagesFolder;
    }


    @Keep
    boolean isOn() {
        return false;
    }
}