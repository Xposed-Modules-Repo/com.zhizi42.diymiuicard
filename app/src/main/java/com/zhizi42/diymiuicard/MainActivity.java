package com.zhizi42.diymiuicard;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.zhizi42.diymiuicard.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private MainAdapter adapter;
    private ActivityResultLauncher<String> selectImageActivityLauncher;
    private File myImagesFolder;
    private MyImagesAdapter myImagesAdapter;

    @SuppressLint("SdCardPath")
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

        RecyclerView recyclerView = binding.recyclerView;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MainAdapter(this);
        refreshList();
        recyclerView.setAdapter(adapter);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshList();
            binding.swipeRefreshLayout.setRefreshing(false);
        });
        MultiprocessSharedPreferences.setAuthority("com.zhizi42.diymiuicard.provider");
        myImagesFolder = new File("/data/data/com.zhizi42.diymiuicard/files/images/");
        if (! myImagesFolder.exists()) {
            if (! myImagesFolder.mkdirs()) {
                Toast.makeText(this, "create my app images folder failed!", Toast.LENGTH_LONG).show();
            }
        }

        selectImageActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        if (inputStream != null) {
                            String name = getFileName(uri);
                            Log.e("test", name);
                            File imageCopy = new File(myImagesFolder, name);
                            if (! imageCopy.createNewFile()) {
                                Toast.makeText(this, "create new image file failed!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            try (FileOutputStream outputStream = new FileOutputStream(imageCopy)) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                            }
                            ArrayList<String> cmdStringList = new ArrayList<>();
                            cmdStringList.add(String.format("chmod 666 \"%s\"", imageCopy.getAbsolutePath()));
                            cmdStringList.add(String.format("cp \"%s\" \"%s\"",
                                    imageCopy.getAbsolutePath(),
                                    "/data/data/com.miui.tsmclient/files/images/"));
                            cmdStringList.add(String.format("chmod 666 \"%s\"",
                                    "/data/data/com.miui.tsmclient/files/images/" + imageCopy.getName()));
                            Utils.executeShell(cmdStringList);
                            myImagesAdapter.addDone(imageCopy);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @SuppressLint("NotifyDataSetChanged")
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
                    refreshList();
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
                        selectImageActivityLauncher.launch("image/*");
                    })
                    .show();
        } else {
            selectImageActivityLauncher.launch("image/*");
        }
    }

    public File getMyImagesFolder() {
        return myImagesFolder;
    }

    public String getFileName(Uri uri) {
        String fileName = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        fileName = cursor.getString(columnIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            assert fileName != null;
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName;
    }


    @Keep
    boolean isOn() {
        return false;
    }
}