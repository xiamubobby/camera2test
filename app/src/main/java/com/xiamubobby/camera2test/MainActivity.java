package com.xiamubobby.camera2test;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.util.Arrays;

public class MainActivity extends Activity {

    Size mOutPutSize;

    CameraDevice mCameraDevice;
    CaptureRequest.Builder mCaptureRequestBuilder;
    CameraCaptureSession mCameraCaptureSession;
    TextureView mTextureView;
    SurfaceTexture mSurfaceTexture;
    Surface mSurface;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = (TextureView) findViewById(R.id.simp_texture);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1, int arg2) {
                mSurfaceTexture = arg0;
                mSurface = new Surface(mSurfaceTexture);
            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
                return false;
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1, int arg2) {}
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture arg0) {}
        });
        mSurfaceTexture = mTextureView.getSurfaceTexture();
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
                Log.v("size at "+i, wSize[i].toString());
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
        mCaptureRequestBuilder.addTarget(mSurface);
        /**
         * =>createCaptureSession
         */
        createCaptureSession();
    }

    public void createCaptureSession() {
        mSurfaceTexture.setDefaultBufferSize(mOutPutSize.getWidth(), mOutPutSize.getHeight());
        try {
            mCameraDevice.createCaptureSession(
                    Arrays.asList(mSurface),
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
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.close();
    }
}

