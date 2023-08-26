# android-camera2相机开发【3】-解决相机预览图像拉伸问题

简单来说，预览图像拉伸问题是相机的输出尺寸和屏幕上预览窗口的宽高比不同引起的。所以可以根据选择的相机输出尺寸的宽高比调整预览窗口的宽高比，使两者一致，从而消除图像拉伸问题。

本篇文章在上篇文章的基础上，自定义可以设置宽高比的预览控件 `AutoFitTextureView` ，解决图像拉伸的问题。

### 1. 自定义 AutoFitTextureView

`AutoFitTextureView`  继承自 `TextureView` ，添加一个约束宽高比的方法，设定宽高比后请求系统重新计算布局，实现预览窗口大小的调整。

```java
public class AutoFitTextureView extends TextureView {

    private int ratioW = 0;
    private int ratioH = 0;

    public AutoFitTextureView(Context context) {
        super(context);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置宽高比
     * @param width
     * @param height
     */
    public void setAspectRation(int width, int height){
        if (width < 0 || height < 0){
            throw new IllegalArgumentException("width or height can not be negative.");
        }
        //相机输出尺寸宽高默认是横向的，屏幕是竖向时需要反转
        // （后续适配屏幕旋转时会有更好的方案，这里先这样）
        ratioW = height;
        ratioH = width;
        //请求重新布局
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (0 == ratioW || 0 == ratioH){
            //未设定宽高比，使用预览窗口默认宽高
            setMeasuredDimension(width, height);
        }else {
            //设定宽高比，调整预览窗口大小（调整后窗口大小不超过默认值）
            if (width < height * ratioW / ratioH){
                setMeasuredDimension(width, width * ratioH / ratioW);
            }else {
                setMeasuredDimension(height * ratioW / ratioH, height);
            }
        }

    }
}
```

### 2. `AutoFitTextureView`  的使用

1. 布局文件：将 `TextureView` 替换为 `AutoFitTextureView` 的类全路径即可。
2. 代码使用：在初始化界面及按钮点击事件更新 `previewSize` 后添加 `previewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());`

### 3. 总结

经过上述改造，切换相机输出尺寸时，预览窗口会自动调整大小，保持和输出尺寸有相同的宽高比，这样就不会再出现图像拉伸的问题。

但是，现在还有一个问题，就是如果手机未锁定方向，当手机横向放置的时候，预览窗口里的图像旋转了90度，而且布局也有问题，需要接着优化，后续文章实现。