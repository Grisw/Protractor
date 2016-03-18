package pers.missingno.protractor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.text.DecimalFormat;

public class ProtractorView extends SurfaceView {

    private int O_X=0;
    private int O_Y=0;

    private int TEXT_SIZE=40;
    private int MAIN_COLOR=Color.RED;
    private int STROKE_WIDTH=10;
    private DecimalFormat format=new DecimalFormat("0.##");

    private SurfaceHolder holder;
    private Paint paint;

    public ProtractorView(Context context) {
        super(context);
        init();
    }

    public ProtractorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProtractorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ProtractorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
        new Thread(){
            public void run(){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                O_X=100;
                O_Y=(getHeight()-100)/2+50;
                holder=getHolder();
                paint=new Paint();
                paint.setColor(MAIN_COLOR);
                paint.setStrokeWidth(STROKE_WIDTH);
                paint.setTextSize(TEXT_SIZE);
                holder.setFormat(PixelFormat.TRANSPARENT);

                Canvas canvas = holder.lockCanvas();
                canvas.drawLine(O_X, 50, O_X, getHeight() - 50, paint);
                canvas.drawLine(O_X, O_Y, O_X - 50, O_Y, paint);
                holder.unlockCanvasAndPost(canvas);

                setOnTouchListener(new View.OnTouchListener() {

                    private float x, y;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            x = event.getX();
                            y = event.getY();

                            Canvas canvas = holder.lockCanvas();
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            canvas.drawLine(O_X, 50, O_X, getHeight() - 50, paint);
                            canvas.drawLine(O_X, O_Y, O_X - 50, O_Y, paint);
                            if (x >= 100) {
                                canvas.drawLine(O_X, O_Y, x, y, paint);
                                double d = Math.toDegrees(Math.atan((x - O_X) / (y - O_Y)));
                                if (d < 0) {
                                    d = 180 + d;
                                }
                                String left = format.format(180 - d);
                                String right = format.format(d);
                                drawText(canvas, left, (float) ((x + O_X) / 2 + 50 * Math.cos(Math.toRadians(d))), (float) ((y + O_Y) / 2 - 50 * Math.sin(Math.toRadians(d))), paint, (float) (90 - d));
                                drawText(canvas, right, (float) ((x + O_X) / 2 - 80 * Math.cos(Math.toRadians(d))), (float) ((y + O_Y) / 2 + 80 * Math.sin(Math.toRadians(d))), paint, (float) (90 - d));
                            }
                            holder.unlockCanvasAndPost(canvas);
                        }
                        return true;
                    }
                });
            }
        }.start();
    }

    public void drawText(Canvas canvas ,String text , float x ,float y,Paint paint ,float angle){
        if(angle != 0){
            canvas.rotate(angle, x, y);
        }
        canvas.drawText(text, x, y, paint);
        if(angle != 0){
            canvas.rotate(-angle, x, y);
        }
    }
}
