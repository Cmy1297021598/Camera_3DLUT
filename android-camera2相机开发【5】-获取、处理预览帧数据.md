# android-camera2相机开发【5】-获取、处理预览帧数据

camera2 api 中使用 `ImageReader` 类间接获取预览帧数据。

ImageReader 使用之前，需要设置一个监听 `OnImageAvailableListener`，在预览帧可用时会被回调，在回调方法中可以接收到预览帧，并实现具体的处理逻辑。

然后，获取 ImageReader 的 Surface ，在创建相机捕获会话时，添加进去作为输出Sruface。

最后，构建捕获请求时，需要将 ImageReader 的 Surface 添加进去。

相机使用完关闭时，需要将 ImageReader 也关闭。

```java
        //获取 ImageReader 和 surface
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据，本例的2代表ImageReader中最多可以获取两帧图像流
        previewReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 2);
        previewReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        //获取预览帧数据
                        Image image = reader.acquireLatestImage();
                        //处理逻辑
                        if (image != null){
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] data = new byte[buffer.remaining()];
                            Log.d(TAG, "data-size=" + data.length);
                            buffer.get(data);
                            image.close();
                        }
                    }
                },
                null);
        
        //获取 ImageReader 的 Surface
        final Surface readerSurface = previewReader.getSurface();
        
        //预览帧数据会同时输出到 previewSurface，readerSurface
        cameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        ……
                    });
        
        //构建预览捕获请求时，添加 readerSurface
        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(readerSurface);
        
        //释放ImageReader资源
        if (previewReader != null){
            previewReader.close();
            previewReader = null;
        }
```



