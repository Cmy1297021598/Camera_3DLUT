# android-camera2相机开发【4】-选取最优预览输出尺寸

### 1. 拍照模式下的最优输出预览尺寸选择

一般来说拍照的时候尽量选择最大的输出尺寸，以达到最好的预览效果（和拍出的照片效果一样）。

### 2. 视频模式下的最优输出预览尺寸选择

* 相机输出尺寸默认是横向的（宽>高），手机窗口一般是竖向的（不考虑旋转横置的情况），所以比较时将输出尺寸的 __宽高比__ 与 预览窗口的 __高宽比__ 进行比较。 
* 录制视频的时候，为了预览和播放效果好（充满窗口），可以选择宽高比与预览窗口高宽比一致的输出尺寸。
* 如果没有宽高比一致的输出尺寸，则可以选择高宽比接近的。
* 但是如果上面选择的尺寸过小，则预览图像画质比较模糊，所以可以设定一个阈值，如果不满足阈值，则退而求其次选择和预览窗口面积最接近的输出尺寸。

### 3. 代码实现

在前文的基础上进行调整，添加两个按钮，拍照模式、录像模式，点击后更新预览窗口。

首先，在获取相机输出尺寸列表时拍个序，按降序排列，方便后面的筛选。

```java
        //获取指定相机的输出尺寸列表，并降序排序
        outputSizes = CameraUtils.getInstance().getCameraOutputSizes(cameraId, SurfaceTexture.class);
        Collections.sort(outputSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight();
            }
        });
        Collections.reverse(outputSizes);
```

然后设置按钮点击事件

```java
        btnImageMode = view.findViewById(R.id.btn_image_mode);
        btnImageMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //拍照模式，选择最大输出尺寸
                updateCameraPreviewWithImageMode();
            }
        });

        btnVideoMode = view.findViewById(R.id.btn_video_mode);
        btnVideoMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //录像模式，选择宽高比和预览窗口高宽比一致或最接近且的输出尺寸
                //如果该输出尺寸过小，则选择和预览窗口面积最接近的输出尺寸
                updateCameraPreviewWithVideoMode();
            }
        });
```

拍照模式的最优输出尺寸选择

```java
    private void updateCameraPreviewWithImageMode(){
        previewSize = outputSizes.get(0);
        previewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());
        createPreviewSession();
    }
```

录像模式下最优输出尺寸的选择

```java
    private void updateCameraPreviewWithVideoMode(){
        List<Size> sizes = new ArrayList<>();
        //计算预览窗口高宽比，高宽比，高宽比
        float ratio = ((float) previewView.getHeight / previewView.getWidth());
        //首先选取宽高比与预览窗口高宽比一致且最大的输出尺寸
        for (int i = 0; i < outputSizes.size(); i++){
            if (((float)outputSizes.get(i).getWidth()) / outputSizes.get(i).getHeight() == ratio){
                sizes.add(outputSizes.get(i));
            }
        }
        if (sizes.size() > 0){
            previewSize = Collections.max(sizes, new CompareSizesByArea());
            previewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());
            createPreviewSession();
            return;
        }
        //如果不存在宽高比与预览窗口高宽比一致的输出尺寸，则选择与其宽高比最接近的输出尺寸
        sizes.clear();
        float detRatioMin = Float.MAX_VALUE;
        for (int i = 0; i < outputSizes.size(); i++){
            Size size = outputSizes.get(i);
            float curRatio = ((float)size.getWidth()) / size.getHeight();
            if (Math.abs(curRatio - ratio) < detRatioMin){
                detRatioMin = curRatio;
                previewSize = size;
            }
        }
        if (previewSize.getWidth() * previewSize.getHeight() > PREVIEW_SIZE_MIN){
            previewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());
            createPreviewSession();
        }
        //如果宽高比最接近的输出尺寸太小，则选择与预览窗口面积最接近的输出尺寸
        long area = previewView.getWidth() * previewView.getHeight();
        long detAreaMin = Long.MAX_VALUE;
        for (int i = 0; i < outputSizes.size(); i++){
            Size size = outputSizes.get(i);
            long curArea = size.getWidth() * size.getHeight();
            if (Math.abs(curArea - area) < detAreaMin){
                detAreaMin = curArea;
                previewSize = size;
            }
        }
        previewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());
        createPreviewSession();
    }
```

### 4. 总结

本文针对不同的使用场景，给出简单的最优预览输出尺寸的选择方法，当然还有更好，兼容性更强的方法，这里提供一个简单的思路。