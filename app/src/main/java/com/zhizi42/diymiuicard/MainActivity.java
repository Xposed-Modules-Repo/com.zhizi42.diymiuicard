package com.zhizi42.diymiuicard;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.zhizi42.diymiuicard.databinding.ActivityMainBinding;

import java.io.File;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private MainAdapter adapter;
    private File myImagesFolder;
    private MyImagesAdapter myImagesAdapter;
    private ActivityResultLauncher<Intent> startSelectImageActivity;
    private Context context;

    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, insets) -> {
            Insets statusBars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout());//获取状态栏和屏幕缺口的inset
            int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);//获取默认toolbar高度
            binding.toolbar.setMinimumHeight(statusBars.top + toolbarHeight);//设置toolbar高度为默认高度+状态栏/屏幕缺口高度
            v.setPadding(v.getPaddingLeft(), statusBars.top, v.getPaddingRight(), v.getPaddingBottom());//设置顶部padding为状态栏/屏幕缺口高度
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, (v, insets) -> {
            Insets navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());//获取导航栏inset
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navigationBar.bottom);//设置底部padding为导航栏高度
            return insets;
        });

        SharedPreferences preferencesPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Utils.setDebug(preferencesPreferences.getBoolean("debug", false));
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("first_run", true)) {
            howToUse();
            sharedPreferences.edit().putBoolean("first_run", false).apply();
        }//如果第一次启动为默认true就显示使用说明并把第一次启动设置为false

        RecyclerView recyclerView = binding.recyclerView;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);//设置列表为垂直方向
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MainAdapter(this);
        recyclerView.setAdapter(adapter);

        //设置主界面下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            adapter.refresh();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        //如果本地图片文件夹不存在就新建
        myImagesFolder = new File("/data/data/com.zhizi42.diymiuicard/files/images/");
        if (! myImagesFolder.exists()) {
            if (! myImagesFolder.mkdirs()) {
                Toast.makeText(this, "create my app images folder failed!", Toast.LENGTH_LONG).show();
            }
        }

        //启动选择图片的activity
        startSelectImageActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                o -> {
                    if (o.getResultCode() == Activity.RESULT_OK && o.getData() != null) {
                        //选择完成后如果有返回就在我的diy图片里添加这张图片文件
                        myImagesAdapter.addDone(
                                new File(
                                        myImagesFolder,
                                        o.getData().getStringExtra("FILE_NAME"))
                        );
                        //提示用户如果图片错误可以清除缓存
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

        context = this;
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(@NonNull XposedService service) {
                MyXposedService.setService(service);

                binding.textView.setText(R.string.text_enable);

                adapter.refresh();//刷新适配器的数据
                int OSType = Utils.checkOSType(context);
                String packageName;
                if (OSType == 0) {
                    packageName = "com.miui.tsmclient";
                } else if (OSType == 1) {
                    packageName = "com.finshell.wallet";
                } else {
                    packageName = "";
                    Utils.showNoCardApp(context);
                }
                if (! MyXposedService.getService().getScope().contains(packageName)) {
                    new AlertDialog.Builder(context)
                            .setTitle("请添加卡包APP到作用域")
                            .setMessage("模块作用域没有卡包APP，请点击确定后，同意添加卡包APP到本模块作用域。")
                            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                MyXposedService.getService().requestScope(packageName, new XposedService.OnScopeEventListener() {});
                            })
                            .show();
                }
            }

            @Override
            public void onServiceDied(@NonNull XposedService service) {
            }
        });

    }

    //显示使用说明
    public void howToUse() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_use_title)
                .setMessage(R.string.how_to_use_msg)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    //当设置按钮被点击时打开设置activity
    public void onSettingClick(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    //当删除全部按钮被点击时
    public void onDeleteClick(View view) {
        //提示是否删除全部
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_delete_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    //点击确定就清空存储的卡面信息
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("all_card_url_set");
                    editor.apply();
                    adapter.refresh();
                })
                .show();
    }

    public void selectImageAdd(MyImagesAdapter adapter) {
        myImagesAdapter = adapter;//设置我的自定义图片适配器，用于添加图片完成后更新添加的图片到我的自定义图片
        //如果第一次添加图片就提示需要root权限，再启动添加图片activity，否则直接启动
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

    //启动选择图片的activity
    public void startSelectActivity() {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startSelectImageActivity.launch(intent);
    }

    public File getMyImagesFolder() {
        return myImagesFolder;
    }
}