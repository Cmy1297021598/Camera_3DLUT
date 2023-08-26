package com.demo.demos.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.demo.demos.R;
import com.demo.demos.base.BaseActivity;
import com.demo.demos.base.BaseFragment;
import com.demo.demos.filter.ColorFilter;
import com.demo.demos.render.CameraPreviewRender;
import com.demo.demos.utils.CameraUtils;

import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class GLPreviewFragment extends BaseFragment {

    GLSurfaceView glSurfaceView;

    boolean useFront = false;//是否使用的是前置相机
    String cameraId;
    CameraManager cameraManager;
    List<Size> outputSizes;
    Size photoSize;
    CameraDevice cameraDevice;
    CameraCaptureSession captureSession;
    CaptureRequest.Builder previewRequestBuilder;
    CaptureRequest previewRequest;

    CameraPreviewRender cameraPreviewRender;
    SurfaceTexture surfaceTexture;
    Surface surface;

    Button btnCamera, btnColorFilter, btnColorFilter2, btnPhoto;

    public GLPreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gl_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCamera();

        initViews(view);
    }

    private void initCamera() {
        cameraManager = CameraUtils.getInstance().getCameraManager();
        cameraId = CameraUtils.getInstance().getCameraId(useFront);
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        photoSize = outputSizes.get(16);
    }

    private void initViews(View view) {
        glSurfaceView = view.findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(3);
        cameraPreviewRender = new CameraPreviewRender();
        glSurfaceView.setRenderer(cameraPreviewRender);

        btnColorFilter = view.findViewById(R.id.btnColorFilter);
        btnColorFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ColorFilter.COLOR_FLAG == 0)
                    ColorFilter.COLOR_FLAG = 6;
                else
                    ColorFilter.COLOR_FLAG = 0;
                Log.d(TAG, "OpenGL-colorFlag: "+ColorFilter.COLOR_FLAG);
            }
        });

        btnColorFilter2 = view.findViewById(R.id.btnColorFilter2);
        btnColorFilter2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ColorFilter.COLOR_FLAG == 0)
                    ColorFilter.COLOR_FLAG = 11;
                else
                    ColorFilter.COLOR_FLAG = 0;
                Log.d(TAG, "OpenGL-colorFlag: "+ColorFilter.COLOR_FLAG);
            }
        });

        btnPhoto = view.findViewById(R.id.btnPhoto);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraPreviewRender.setTakingPhoto(true);
            }
        });

        btnCamera = view.findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //切换相机
                changeCamera();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).requestPermission("请给予相机、存储权限，以便app正常工作",
                new BaseActivity.Callback() {
                    @Override
                    public void success() {
//                        glSurfaceView.onResume();
                        openCamera();
                    }

                    @Override
                    public void failed() {
                        Toast.makeText(getContext(), "未授予相机、存储权限！", Toast.LENGTH_SHORT).show();
                    }
                },
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    }

    @Override
    public void onPause() {
//        glSurfaceView.onPause();
        releaseCamera();
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "相机访问异常");
        }
    }

    private void changeCamera(){
        releaseCamera();
        useFront = !useFront;
        cameraId = CameraUtils.getInstance().getCameraId(useFront);
        openCamera();

        cameraPreviewRender.setUseFront(useFront);
    }

    private void releaseCamera() {
        CameraUtils.getInstance().releaseCameraSession(captureSession);
        CameraUtils.getInstance().releaseCameraDevice(cameraDevice);
    }

    CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "相机已启动");
            surfaceTexture = cameraPreviewRender.getSurfaceTexture();
            if (surfaceTexture == null) {
                return;
            }
            surfaceTexture.setDefaultBufferSize(photoSize.getWidth(), photoSize.getHeight());
            surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                    glSurfaceView.requestRender();
                }
            });
            surface = new Surface(surfaceTexture);

            try {
                cameraDevice = camera;
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(surface);
                previewRequest = previewRequestBuilder.build();

                cameraDevice.createCaptureSession(Arrays.asList(surface), sessionsStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "相机已断开连接");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "相机打开出错");
        }
    };

    CameraCaptureSession.StateCallback sessionsStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (null == cameraDevice) {
                return;
            }

            captureSession = session;
            try {
                captureSession.setRepeatingRequest(previewRequest,
                        null,
                        null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "会话注册失败");
        }
    };
}
