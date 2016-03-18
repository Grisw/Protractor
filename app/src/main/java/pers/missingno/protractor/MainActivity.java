package pers.missingno.protractor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import java.text.DecimalFormat;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private CameraManager camera;
    private CameraDevice device;
    private CameraCaptureSession session;
    private Size previewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });

        final SurfaceView ruler= (SurfaceView) findViewById(R.id.ruler);
        ruler.setZOrderOnTop(true);

        final SurfaceHolder holder=ruler.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);

        camera = (CameraManager) getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = camera.getCameraCharacteristics(CameraCharacteristics.LENS_FACING_FRONT + "");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        previewSize = map.getOutputSizes(SurfaceTexture.class)[0];

        final TextureView preview = (TextureView) findViewById(R.id.preview);
        preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                HandlerThread thread = new HandlerThread("Camera2");
                thread.start();
                final Handler handler = new Handler(thread.getLooper());
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    camera.openCamera(CameraCharacteristics.LENS_FACING_FRONT + "", new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(CameraDevice camera) {
                            device=camera;
                            try {
                                final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                SurfaceTexture texture = preview.getSurfaceTexture();
                                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                                Surface surface = new Surface(texture);
                                builder.addTarget(surface);
                                camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(CameraCaptureSession session) {
                                        MainActivity.this.session=session;
                                        try {
                                            session.setRepeatingRequest(builder.build(),null,handler);
                                        } catch (CameraAccessException e) {
                                            e.printStackTrace();
                                            session.close();
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(CameraCaptureSession session) {

                                    }
                                },handler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onDisconnected(CameraDevice camera) {
                            camera.close();
                        }

                        @Override
                        public void onError(CameraDevice camera, int error) {
                            camera.close();
                        }
                    }, handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        preview.setOnTouchListener(new View.OnTouchListener() {

            private float x,y;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_MOVE){
                    x=event.getX();
                    y=event.getY();
                    Canvas canvas=holder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Paint p = new Paint();
                    p.setColor(Color.RED);
                    p.setStrokeWidth(10);
                    p.setTextSize(40);
                    canvas.drawLine(100, 50, 100, preview.getHeight() - 50, p);
                    canvas.drawLine(100, (preview.getHeight()-100)/2+50,50,(preview.getHeight()-100)/2+50,p);
                    if(x>=100){
                        canvas.drawLine(100, (preview.getHeight() - 100) / 2 + 50, x, y, p);
                        double d=Math.toDegrees(Math.atan((x-100)/(y-(preview.getHeight()-100)/2-50)));
                        if(d<0){
                            d=180+d;
                        }
                        DecimalFormat format=new DecimalFormat("0.##");
                        String left=format.format(180-d);
                        String right=format.format(d);
                        drawText(canvas, left, (float) ((x + 100)/2+50*Math.cos(Math.toRadians(d))), (float) ((y +(preview.getHeight() - 100) / 2 + 50)/2-50*Math.sin(Math.toRadians(d))), p,  (float) (90 - d));
                        drawText(canvas, right, (float) ((x + 100)/2-80*Math.cos(Math.toRadians(d))), (float) ((y +(preview.getHeight() - 100) / 2 + 50)/2+80*Math.sin(Math.toRadians(d))), p, (float) (90 - d));
                    }
                    holder.unlockCanvasAndPost(canvas);
                }
                return true;
            }
        });

        new Thread(){
            public void run(){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Canvas canvas=holder.lockCanvas();
                Paint p = new Paint();
                p.setColor(Color.RED);
                p.setStrokeWidth(10);
                canvas.drawLine(100, 50, 100, preview.getHeight() - 50, p);
                canvas.drawLine(100,(preview.getHeight()-100)/2+50,50,(preview.getHeight()-100)/2+50,p);
                holder.unlockCanvasAndPost(canvas);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(device!=null)
                device.close();
            if(session!=null)
                session.close();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
