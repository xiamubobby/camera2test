package com.xiamubobby.camera2test;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends Activity {

    Size mOutPutSize;
    CameraDevice mCameraDevice;
    CaptureRequest.Builder mCaptureRequestBuilder;
    CameraCaptureSession mCameraCaptureSession;
    TextureView mTextureView;
    SurfaceTexture mTVSurfaceTexture;
    Surface mTVSurface;
    ImageReader mImageReader;
    Surface mIRSurface;

    private Thread mProducerThread = null;
    private GLRendererImpl mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = (TextureView) findViewById(R.id.scanner_texture_view);
        mRenderer = new GLRendererImpl(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1, int arg2) {
                mTVSurfaceTexture = arg0;
                mTVSurface = new Surface(mTVSurfaceTexture);

                mRenderer.setViewport(arg1, arg2);
                mProducerThread = new GLProducerThread(arg0, mRenderer, new AtomicBoolean(true));
                mProducerThread.start();
            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
                mProducerThread = null;
                return true;
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1, int arg2) {
                mRenderer.resize(arg1, arg2);
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
//                    Canvas cvs = mTVSurface.lockCanvas(null);
//                    mTVSurface.unlockCanvasAndPost(cvs);
            }
        });
        mTVSurfaceTexture = mTextureView.getSurfaceTexture();
    }

    public void openCamera(View view) {
        openCamera();
    }
    public void openCamera() {
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mOutPutSize = map.getOutputSizes(SurfaceTexture.class)[0];
            Size [] wSize = map.getOutputSizes(SurfaceTexture.class);
            for (int i = 0; i != wSize.length; i++) {
                Log.v("size at " + i, wSize[i].toString());
            }

            manager.openCamera(
                    cameraId,
                    new CameraDevice.StateCallback() {
                        @Override
                        public void onDisconnected(CameraDevice arg0) {}
                        @Override
                        public void onError(CameraDevice arg0, int arg1) {}
                        @Override
                        public void onOpened(CameraDevice arg0) {
                            mCameraDevice = arg0;
                            /**
                             * =>createCaptureRequest
                             */
                            createCaptureRequest();
                        }},
                    null);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void createCaptureRequest() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_BARCODE);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO );
        mCaptureRequestBuilder.addTarget(mTVSurface);
        mImageReader = ImageReader.newInstance(mOutPutSize.getWidth(), mOutPutSize.getHeight(), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Log.v("yes this works", " it really works");
                    }
                },
                new Handler());
        mIRSurface = mImageReader.getSurface();
        //mCaptureRequestBuilder.addTarget(mIRSurface);
        /**
         * =>createCaptureSession
         */
        createCaptureSession();
    }

    public void createCaptureSession() {
        mTVSurfaceTexture.setDefaultBufferSize(mOutPutSize.getWidth(), mOutPutSize.getHeight());

        List<Surface> outputSurfaces = new ArrayList<Surface>(2);
        outputSurfaces.add(mTVSurface);
        //outputSurfaces.add(mIRSurface);

        try {
            mCameraDevice.createCaptureSession(
                    outputSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mCameraCaptureSession = session;
                            /**
                             * =>createRepeatRequest
                             */
                            createRepeatRequest();
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.e("cameraConfig", "Failed");
                        }},
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void createRepeatRequest() {
        try {
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void scannerOut(View view) {
        //scannerOut();
    }
    public void scannerOut() {
        Fragment scannerFragment = getFragmentManager().findFragmentById(R.id.scannerFragment);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        View v = scannerFragment.getView();
        int[] lcn = new int[2];
        scannerFragment.getView().getLocationOnScreen(lcn);
        scannerFragment.getView().setVisibility(View.VISIBLE);
        v .setTranslationY(v.getTranslationY()+100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
