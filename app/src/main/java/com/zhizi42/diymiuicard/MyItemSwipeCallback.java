package com.zhizi42.diymiuicard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class MyItemSwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final float threshold = 0.35f;//触发删除阈值
    private final int grayColor = Color.parseColor("#DDDDDD");
    private int swipeColor;
    MainCardAdapter adapter;
    Drawable icon;
    private final Paint paint = new Paint();

    public MyItemSwipeCallback(MainCardAdapter adapter, Drawable icon, int swipeColor) {
        super(0, ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.icon = icon;
        this.swipeColor = swipeColor;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                         int direction) {
        adapter.archive(viewHolder.getBindingAdapterPosition());
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            int actionState,
                            boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        float width = itemView.getWidth();
        float progress = Math.abs(dX) / width;

        // 判断颜色
        if (progress > threshold) {
            paint.setColor(swipeColor);
        } else {
            paint.setColor(grayColor);
        }

        // 绘制背景
        RectF background = new RectF(
                itemView.getRight() + dX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom()
        );

        c.drawRect(background, paint);

        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        if (Math.abs(dX) > (icon.getIntrinsicWidth() + iconMargin)) {
            drawIcon(c, itemView);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawIcon(Canvas c, View itemView) {

        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;

        int iconTop = itemView.getTop() + iconMargin;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        int iconRight = itemView.getRight() - iconMargin;
        int iconLeft = iconRight - icon.getIntrinsicWidth();

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        icon.draw(c);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return threshold;
    }
}
