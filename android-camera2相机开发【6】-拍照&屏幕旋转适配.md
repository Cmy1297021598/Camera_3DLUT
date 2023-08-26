# android-camera2相机开发【6】-拍照&屏幕旋转适配

前面几篇文章实现了相机的预览，对预览中出现的问题及需要注意的事项进行了梳理。

本篇文章对相机的拍照流程、拍照方向及屏幕旋转时的适配问题进行梳理。

### 初始化相机和view

这里选择最大的相机输出尺寸作为拍照尺寸和预览尺寸。

```java
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCamera();

        initViews(view);
    }
    
    private void initCamera() {
        cameraManager = CameraUtils.getInstance().getCameraManager();
        cameraId = CameraUtils.getInstance().getBackCameraId();
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        photoSize = outputSizes.get(0);
    }

    private void initViews(View view) {
        btnPhoto = view.findViewById(R.id.btn_photo);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        previewView = view.findViewById(R.id.preview_view);
    }
```

### 启动/释放相机

在 fragment 的 onResume 生命周期函数中启动相机，在 onPause 中释放相机。

```java
    @Override
    public void onResume() {
        super.onResume();
        if (previewView.isAvailable()) {
            openCamera();
        } else {
            previewView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
    }
```

### 屏幕旋转时适配预览窗口和方向

启动相机时需要根据此时屏幕的显示方向（横屏/竖屏）调整预览窗口的大小和显示方向。

* 调整预览窗口的大小，可以参考之前解决预览界面拉伸的文章，里面有描述。
* 调整预览方向：计算出预览窗口到相机输出窗口的变换矩阵。
    * 先将两个窗口的中心平移到同一点；
    * 设置两窗口缩放模式；
    * 计算缩放比例和旋转角度；
    * 将变换矩阵应用到预览窗口上。

```java
    @SuppressLint("MissingPermission")
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        } else {
            try {
                //根据屏幕的显示方向调整预览窗口大小
                displayRotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getOrientation();
                if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
                    previewView.setAspectRation(photoSize.getHeight(), photoSize.getWidth());
                }else {
                    previewView.setAspectRation(photoSize.getWidth(), photoSize.getHeight());
                }
                //根据屏幕的显示方向调整预览方向
                configureTransform(previewView.getWidth(), previewView.getHeight());
                cameraManager.openCamera(cameraId, cameraStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }
    }
    
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == previewView || null == photoSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, photoSize.getHeight(), photoSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / photoSize.getHeight(),
                    (float) viewWidth / photoSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        previewView.setTransform(matrix);
    }
```

surfaceTextureListener 之前的文章也提过，这里给出代码

```java
    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //启动相机
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
```

还有相机打开的状态回调监听，成功打开后，初始化预览、拍照用的Surface 以及 保存拍照数据用的 ImageReader。

```java
    CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "相机已启动");
            //初始化拍照用的 ImageReader 和 Surface
            initReaderAndSurface();

            cameraDevice = camera;
            try {
                //初始化预览 Surface
                SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
                if (surfaceTexture == null){
                    return;
                }

                surfaceTexture.setDefaultBufferSize(photoSize.getWidth(), photoSize.getHeight());//设置SurfaceTexture缓冲区大小
                previewSurface = new Surface(surfaceTexture);

                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(previewSurface);
                previewRequest = previewRequestBuilder.build();

                cameraDevice.createCaptureSession(Arrays.asList(previewSurface, photoSurface), sessionsStateCallback, null);
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
```

需要注意的是别忘了给 ImageReader 添加监听器。
当拍照数据可用时会回调该监听，在监听的回调方法中，添加图片的处理保存逻辑即可。

```java
    private void initReaderAndSurface() {

        //初始化拍照 ImageReader
        photoReader = ImageReader.newInstance(photoSize.getWidth(), photoSize.getHeight(), ImageFormat.JPEG, 2);
        photoReader.setOnImageAvailableListener(photoReaderImgListener, null);
        photoSurface = photoReader.getSurface();
    }
    
    ImageReader.OnImageAvailableListener photoReaderImgListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            writeImageToFile();
        }
    };
    
    private void writeImageToFile() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        } else {
            String filePath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/001.jpg";
            Image image = photoReader.acquireNextImage();
            if (image == null) {
                return;
            }
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[byteBuffer.remaining()];
            byteBuffer.get(data);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(filePath));
                fos.write(data);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                    fos = null;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                    image = null;
                }
            }
        }
    }
```

相机会话状态回调之前也说明过，在会话创建成功时，开始重复请求，以获得连续的预览。

```java
    CameraCaptureSession.StateCallback sessionsStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (null == cameraDevice) {
                return;
            }

            captureSession = session;
            try {
                captureSession.setRepeatingRequest(previewRequest, null, null);
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
```

### 拍照

经过上述步骤，可以在屏幕旋转时也能保证正常的预览窗口大小和预览方向。

拍照按钮的点击事件监听函数 takePhoto() 主要有三个步骤。

* 首先需要根据屏幕显示方向，矫正拍照方向
* 然后停止相机预览，创建拍照请求进行拍照
* 拍照成功后，重启相机预览

其中，屏幕显示方向和拍照方向的对应关系用 PHOTO_ORTATION 定义。

```java
    private static final SparseIntArray PHOTO_ORITATION = new SparseIntArray();

    static {
        PHOTO_ORITATION.append(Surface.ROTATION_0, 90);
        PHOTO_ORITATION.append(Surface.ROTATION_90, 0);
        PHOTO_ORITATION.append(Surface.ROTATION_180, 270);
        PHOTO_ORITATION.append(Surface.ROTATION_270, 180);
    }
```

```java
    private void takePhoto() {
        try {
            photoRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            cameraOritation = PHOTO_ORITATION.get(displayRotation);
            photoRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraOritation);
            photoRequestBuilder.addTarget(photoSurface);
            photoRequest = photoRequestBuilder.build();

            captureSession.stopRepeating();
            captureSession.capture(photoRequest, sessionCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "相机访问异常");
        }
    }
    

    CameraCaptureSession.CaptureCallback sessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            try {
                captureSession.setRepeatingRequest(previewRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }
    };
```

### 总结

整个流程并不是多复杂，主要就是要注意手机屏幕旋转时的预览窗口、方向适配，以及拍照方向的矫正。

到目前为止，相机预览、拍照方面的流程和注意事项基本梳理完毕，后续梳理相机特性控制和录像方面的知识。

[项目github地址](https://github.com/WangYantao/android-camera-demos)