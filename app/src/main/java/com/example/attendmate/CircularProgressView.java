
package com.example.attendmate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF circleBounds = new RectF();

    private int progress = 0;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(18f);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setColor(0xFF244363);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(18f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(int value) {

        progress = Math.max(0, Math.min(100, value));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        float radius = Math.min(centerX, centerY) - 20f;

        circleBounds.set(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );

        canvas.drawArc(
                circleBounds,
                -90,
                360,
                false,
                backgroundPaint
        );

        if (progress > 0) {

            SweepGradient gradient = new SweepGradient(
                    centerX,
                    centerY,
                    new int[]{
                            0xFFFF4FA3,
                            0xFF9B5CFF,
                            0xFF39A5FF,
                            0xFFFF4FA3
                    },
                    null
            );

            progressPaint.setShader(gradient);

            canvas.save();
            canvas.rotate(-90, centerX, centerY);

            canvas.drawArc(
                    circleBounds,
                    0,
                    progress * 3.6f,
                    false,
                    progressPaint
            );

            canvas.restore();
            progressPaint.setShader(null);
        }
    }
}
