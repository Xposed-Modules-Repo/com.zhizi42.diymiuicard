package com.zhizi42.diymiuicard;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.zhizi42.diymiuicard.databinding.ActivityDonateBinding;

public class DonateActivity extends AppCompatActivity {

    private ActivityDonateBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDonateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void openAlipay(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://QR.ALIPAY.COM/FKX00236BXA07MOA2XU607"));
        startActivity(intent);
    }
}