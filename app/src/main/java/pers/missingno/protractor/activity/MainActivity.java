package pers.missingno.protractor.activity;

import android.Manifest;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.util.Arrays;

import pers.missingno.protractor.R;
import pers.missingno.protractor.view.ProtractorView;

public class MainActivity extends AppCompatActivity {

    private CameraDevice device;
    private CameraCaptureSession session;
    private TextureView preview;
    private ProtractorView ruler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ruler= (ProtractorView) findViewById(R.id.ruler);
        ruler.setZOrderOnTop(true);

        preview = (TextureView) findViewById(R.id.preview);

        initCamera();
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

    private void initCamera(){
        final CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(CameraCharacteristics.LENS_FACING_FRONT + "");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        final Size previewSize=map.getOutputSizes(SurfaceTexture.class)[0];

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
                    cameraManager.openCamera(CameraCharacteristics.LENS_FACING_FRONT + "", new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(CameraDevice camera) {
                            device = camera;
                            try {
                                final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                SurfaceTexture texture = preview.getSurfaceTexture();
                                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                                Surface surface = new Surface(texture);
                                builder.addTarget(surface);
                                camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(CameraCaptureSession session) {
                                        MainActivity.this.session = session;
                                        try {
                                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                            session.setRepeatingRequest(builder.build(), null, handler);
                                        } catch (CameraAccessException e) {
                                            e.printStackTrace();
                                            session.close();
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(CameraCaptureSession session) {

                                    }
                                }, handler);
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
    }
}
