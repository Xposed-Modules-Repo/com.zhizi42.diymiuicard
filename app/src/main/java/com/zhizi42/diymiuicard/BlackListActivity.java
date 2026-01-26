package com.zhizi42.diymiuicard;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zhizi42.diymiuicard.databinding.ActivityBlackListBinding;
import com.zhizi42.diymiuicard.databinding.ActivityMainBinding;

public class BlackListActivity extends AppCompatActivity {

    private ActivityBlackListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityBlackListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = binding.recyclerViewBlackList;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);//设置列表为垂直方向
        recyclerView.setLayoutManager(linearLayoutManager);
        BlackListAdapter adapter = new BlackListAdapter(this);
        adapter.refresh();//刷新适配器的数据
        recyclerView.setAdapter(adapter);

        setSupportActionBar(binding.toolbarBlacklist);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.refreshLayoutBlackList.setOnRefreshListener(() -> {
            adapter.refresh();
            binding.refreshLayoutBlackList.setRefreshing(false);
        });
    }
}