package com.zhizi42.diymiuicard;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yalantis.ucrop.UCrop;
import com.zhizi42.diymiuicard.databinding.ActivitySelectImageBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectImageActivity extends AppCompatActivity {

    private ActivitySelectImageBinding binding;
    private Uri uriSelect;
    private UCrop.Options options;
    private Bitmap bitmapCrop;
    private Bitmap bitmapResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySelectImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets statusBars = insets.getInsets(
                        WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
                int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
                binding.toolbar.setMinimumHeight(statusBars.top + toolbarHeight);
                v.setPadding(v.getPaddingLeft(), statusBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            }
        });

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //设置剪裁的参数，最大960px x 606px，长宽比与最大限制相同，格式png，质量100，颜色和主界面一样
        options = new UCrop.Options();
        options.withAspectRatio(160, 101);
        options.withMaxResultSize(960, 606);
        options.setCompressionFormat(Bitmap.CompressFormat.PNG);
        options.setCompressionQuality(100);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        //系统图片选择启动器
        ActivityResultLauncher<String> selectImageActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        finish();
                        return;
                    }
                    uriSelect = uri;//记录用户选择的图片的uri
                    binding.imageViewAfterSelect.setImageURI(uriSelect);//设置界面的image view为用户选择的图片
                    bitmapResult = getBitmapFromUri(uriSelect);//最终保存图片设为选择的图片
                }
        );
        selectImageActivityLauncher.launch("image/*");
    }

    public void startSave(View view) {
        EditText editText = new EditText(this);//新建edit text输入框
        editText.setHint(R.string.save_filename_dialog_edittext_hint);
        AlertDialog dialogFilename = new AlertDialog.Builder(this)
                .setTitle(R.string.save_filename_dialog_title)
                .setView(editText)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    save(editText.getText().toString() + ".png");
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialogFilename.setOnShowListener(dialog -> {
            Button saveButton = dialogFilename.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setEnabled(false);

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    saveButton.setEnabled(isValidFileName(s.toString()));//如果文件名合法就启用确定按钮，不合法就禁用
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        });
        dialogFilename.show();
    }

    private boolean isValidFileName(String fileName) {
        // 检查文件名是否为空或包含非法字符（例如：/ \ : * ? " < > |）
        if (fileName.trim().isEmpty()) {
            return false;
        }
        return !fileName.matches(".*[/\\\\:*?\"<>|].*");
    }

    public void save(String name) {
        File myImagesFolder = new File("/data/data/com.zhizi42.diymiuicard/files/images/");//应用的图片文件夹
        File imageCopy = new File(myImagesFolder, name);
        try {
            if (imageCopy.exists()) {
                Toast.makeText(this, R.string.save_toast_file_exists, Toast.LENGTH_LONG).show();
                return;
            } else {
                //尝试创建本地图片文件
                if (!imageCopy.createNewFile()) {
                    Toast.makeText(this, "create new image file failed!", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //打开本地图片文件的output stream，保存当前bitmap result变量到本地文件
        try (FileOutputStream outputStream = new FileOutputStream(imageCopy)) {
            bitmapResult.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //root权限执行命令 复制图片文件到小米智能卡目录并权限改为所有人可读
        ArrayList<String> cmdStringList = new ArrayList<>();
        cmdStringList.add(String.format("chmod 666 \"%s\"", imageCopy.getAbsolutePath()));
        cmdStringList.add(String.format("cp \"%s\" \"%s\"",
                imageCopy.getAbsolutePath(),
                "/data/data/com.miui.tsmclient/files/images/"));
        cmdStringList.add(String.format("chmod 666 \"%s\"",
                "/data/data/com.miui.tsmclient/files/images/" + imageCopy.getName()));
        boolean succ = Utils.executeShell(cmdStringList);
        if (!succ) {
            Toast.makeText(this, R.string.select_image_save_copy_error_toast, Toast.LENGTH_LONG).show();
            finish();
        }

        //删除裁剪后的缓存文件
        File cache = new File(getCacheDir(), "cache.png");
        if (cache.exists()) {
            cache.delete();
        }

        //返回图片文件名给主界面让主界面更新图片
        Intent intent = new Intent();
        intent.putExtra("FILE_NAME", name);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void startDIY(View view) {
        //开始裁剪图片
        Uri uriCrop = Uri.fromFile(new File(getCacheDir(), "cache.png"));
        UCrop.of(uriSelect, uriCrop)
                .withOptions(options)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP && data != null) {
            final Uri resultUri = UCrop.getOutput(data);//得到裁剪后的uri
            bitmapCrop = getBitmapFromUri(resultUri);
            bitmapResult = bitmapCrop;//从裁剪结果uri获取图片并裁剪圆角
            binding.imageViewAfterSelect.setImageBitmap(bitmapResult);//设置界面的图片为裁剪后的图片
            RecyclerView recyclerViewCardLogo = binding.recyclerViewCardLogo;
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            recyclerViewCardLogo.setLayoutManager(linearLayoutManager);
            AssetManager manager = getAssets();
            try {
                List<String> list = Arrays.asList(manager.list("card_logo"));//获取卡面标志图片文件资源列表
                DIYCardLogoAdapter adapter = new DIYCardLogoAdapter(this, list, position -> {
                    Bitmap bitmapImage = bitmapCrop;//得到裁剪后的bitmap
                    if (position == 0) {//如果position是0（点击的是不添加图片
                        binding.imageViewAfterSelect.setImageBitmap(bitmapImage);//界面的image view设置为裁剪后的图片
                        bitmapResult = bitmapImage;//结果bitmap为裁剪后的图片
                    } else {
                        try {
                            if (bitmapImage.getWidth() != 960 || bitmapImage.getHeight() != 606) {
                                Toast.makeText(
                                                this,
                                                R.string.diy_card_toast_image_resolution_small,
                                                Toast.LENGTH_LONG)
                                        .show();
                                //如果宽高不为960x606就缩放到这么多
                                bitmapImage = Bitmap.createScaledBitmap(bitmapImage, 960, 606, true);
                            }
                            bitmapImage = createRoundedCornerBitmap(bitmapImage, 40);
                            InputStream inputStream = getAssets().open("card_logo/" + list.get(position - 1));
                            Bitmap bitmapLogo = BitmapFactory.decodeStream(inputStream);//得到assets里logo的bitmap
                            Bitmap bitmapOverlay = overlayBitmap(bitmapImage, bitmapLogo);//叠加两个bitmap
                            binding.imageViewAfterSelect.setImageBitmap(bitmapOverlay);//设置界面的image view为叠加后的bitmap
                            bitmapResult = bitmapOverlay;//设置最终保存图片为叠加后的图片
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                recyclerViewCardLogo.setAdapter(adapter);
                recyclerViewCardLogo.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        }
    }

    public Bitmap overlayBitmap(Bitmap bitmapBottom, Bitmap bitmapUp) {
        Bitmap resultBitmap = bitmapBottom.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setAlpha(255);
        canvas.drawBitmap(bitmapUp, 0, 0, paint);
        return resultBitmap;
    }

    public Bitmap createRoundedCornerBitmap(Bitmap src, float radius) {
        // 创建一个与原图大小相同的空白Bitmap
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);

        // 创建Canvas以在新的Bitmap上绘制
        Canvas canvas = new Canvas(output);

        // 创建Paint对象
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 创建一个与原图大小相同的RectF
        RectF rectF = new RectF(0, 0, src.getWidth(), src.getHeight());

        // 使用Paint和RectF绘制一个圆角矩形
        canvas.drawRoundRect(rectF, radius, radius, paint);

        // 设置Paint的着色器为BitmapShader
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        // 设置PorterDuff模式以只绘制圆角内的区域
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // 再次绘制圆角矩形，将原图内容裁剪到圆角区域
        canvas.drawRoundRect(rectF, radius, radius, paint);

        // 返回带有圆角的Bitmap
        return output;
    }


    public Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void showTips(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_image_button_tips)
                .setMessage(R.string.select_image_tips)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }
}