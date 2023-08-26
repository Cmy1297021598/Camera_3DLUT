# android-camera2相机开发【8】-使用opengl实现滤镜效果（1）

前一篇文章中，实现了 opengles 进行相机预览的功能，基本的流程如下：

1. 把相机的预览数据做成纹理，绑定到opengles对应的纹理单元上
2. 然后通过opengles 的内置函数 texture()，在片段着色器中根据纹理和纹理坐标进行插值计算
3. 直接将计算结果输出到颜色缓冲区，显示到屏幕的像素上。

给图像添加滤镜本质上就是图片处理，也就是对图片的像素进行计算，简单来说，图像处理的方法可以分为三类：

1. 点算子：当前像素的处理只和自身的像素值有关，和其他像素无关，比如灰度处理。
2. 邻域算子：当前像素的处理需要和相邻的一定范围内的像素有关，比如高斯模糊。
3. 全局算子：在全局上对所有像素进行统一变换，比如几何变换。

### 实现滤镜的思路

滤镜本质上就是对每个位置的颜色值进行调整，比如灰度效果，就是将彩色图像的各个颜色分量的值变成一样的。

对颜色值进行调整的时机应该是再上面步骤的2、3之间，这个时候已经拿到了颜色值，还没有输出到颜色缓冲区，这个时候我们对颜色值进行处理就可以实现滤镜效果。

用上一篇的片段着色器代码做个说明：

```
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 v_texCoord;
out vec4 outColor;
uniform samplerExternalOES s_texture;

void main(){
    //拿到颜色值
    vec4 tmpColor = texture(s_texture, v_texCoord);
    //对颜色值进行处理
    process(tmpColor);
    //将处理后的颜色值输出到颜色缓冲区
    outColor = tmpColor;
}
```

## 点算子实现的滤镜

### 灰度滤镜

灰度滤镜通过图像的灰度化算法进行实现。

在 RBG颜色模型中，让 R=G=B=grey， 即可将彩色图像转为灰度图像，其中 grey 叫做灰度值。

grey的计算方法有四种：分量法，最大值法，平均值法、加权平均法。

**分量法**

使用彩色图像的某个颜色分量的值作为灰度值。

* grey=R：R分量灰度图
* grey=G：G分量灰度图
* grey=B：B分量灰度图

**最大值法**

将彩色图像三个颜色分量中值最大的作为灰度值。

grey = max(R,G,B)

**平均值法**

将彩色图像的三个颜色分量值的平均值作为灰度值

grey = (R+G+B)/3

**加权平均法**

在 RGB 颜色模型中，人眼对G（绿色）的敏感度最高，对B（蓝色）的敏感的最低，所以对彩色图像的三个颜色分量做加权平均计算灰度值效果比较好。

grey = 0.3*R + 0.59*G + 0.11*B

下面在片段着色器中用加权平均法实现灰度滤镜。

```
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 v_texCoord;
out vec4 outColor;
uniform samplerExternalOES s_texture;

//灰度滤镜
void grey(inout vec4 color){
    float weightMean = color.r * 0.3 + color.g * 0.59 + color.b * 0.11;
    color.r = color.g = color.b = weightMean;
}

void main(){
    //拿到颜色值
    vec4 tmpColor = texture(s_texture, v_texCoord);
    //对颜色值进行处理
    grey(tmpColor);
    //将处理后的颜色值输出到颜色缓冲区
    outColor = tmpColor;
}
```

效果图
 
|原图|灰度滤镜|
|---|---|
|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/001.jpg)|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/002.jpg)|

### 黑白滤镜

黑白滤镜就是将图像进行二值化处理，彩色图像的颜色值经过处理之后，要么是 0（黑色），要么是255（白色）。

实际应用中使用的不多，从各大直播、美颜相机、短视频app上就能发现，基本上没有用黑白滤镜，因为不好看。

二值化方法主要有：全局二值化，局部二值化，局部自适应二值化。最影响效果的就是阈值的选取。

* 全局二值化是选定一个阈值，然后将大于该阈值的颜色值置为255，小于该阈值的颜色置为0。因为使用的全局阈值，所以会丧失很多细节。
* 局部二值化：为了弥补全局阈值化的缺陷，将图像分为N个窗口，每个窗口设定一个阈值，进行二值化操作，一般取该窗口颜色值的平均值。
* 局部自适应二值化：局部二值化的阈值选取方法仍然不能很好的将对应窗口的图像进行二值化，在此基础上，通过窗口颜色的平均值E、像素之间的差平方P、像素之间的均方根Q等能够表示窗口内局部特征的参数，设定计算公式计算阈值。

这里使用最简单的全局二值化做个示例

```
//黑白滤镜
void blackAndWhite(inout vec4 color){
    float threshold = 0.5;
    float mean = (color.r + color.g + color.b) / 3.0;
    color.r = color.g = color.b = mean >= threshold ? 1.0 : 0.0;
}
```

效果图

|原图|黑白滤镜|
|---|---|
|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/003.jpg)|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/004.jpg)|

### 反色滤镜

RGB 颜色值的范围是 [0,255]，反色滤镜的的原理就是将 255 与当前颜色的每个分量Rs,Gs,Bs值做差运算。

结果颜色为 (R,G,B) = (255 - Rs, 255 - Gs, 255 - Bs);

```
//反向滤镜
void reverse(inout vec4 color){
    color.r = 1.0 - color.r;
    color.g = 1.0 - color.g;
    color.b = 1.0 - color.b;
}
```

效果图

|原图|反色滤镜|
|---|---|
|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/005.jpg)|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/006.jpg)|

### 亮度滤镜

增加亮度有两种方法：

1. 再 rgb 颜色空间下，将各个颜色分量都加上一个值，可以达到图像亮度增加的目的，但是这种方式会导致图像一定程度上偏白。
2. 将颜色值从 rgb 颜色空间转换到 hsl 颜色空间上，因为 hsl 更适合视觉上的描述，色相、饱和度、亮度，调整 l（亮度分量），即可实现图像的亮度处理，然后将调整后的 hsl 值再转换到 rgb 颜色空间上进行输出。

[各颜色空间转换方法](http://www.easyrgb.com/en/math.php)

下面以第2中方式为例：

```
//rgb转hsl
vec3 rgb2hsl(vec3 color){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(color.bg, K.wz), vec4(color.gb, K.xy), step(color.b, color.g));
    vec4 q = mix(vec4(p.xyw, color.r), vec4(color.r, p.yzx), step(p.x, color.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

//hsla转rgb
vec3 hsl2rgb(vec3 color){
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(color.xxx + K.xyz) * 6.0 - K.www);
    return color.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), color.y);
}

//亮度
void light(inout vec4 color){
    vec3 hslColor = vec3(rgb2hsl(color.rgb));
    hslColor.z += 0.15;
    color = vec4(hsl2rgb(hslColor), color.a);
}
```

|原图|亮度滤镜|
|---|---|
|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/007.jpg)|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/008.jpg)|

### 色调分离

色调分离的原理简单来说就是根据图像的直方图，将图像分为阴影、中间、高光三个部分，在hsl颜色空间中调整每个部分的色相、饱和度。调整色相可以对图像进行色彩调整，调整饱和度可以使图像整体的颜色趋于一个整体的风格。

```
//色调分离
void saturate(inout vec4 color){
    //计算灰度值
    float grayValue = color.r * 0.3 + color.g * 0.59 + color.b * 0.11;
    //转换到hsl颜色空间
    vec3 hslColor = vec3(rgb2hsl(color.rgb));
    //根据灰度值区分阴影和高光，分别处理
    if(grayValue < 0.3){
        //添加蓝色
        if(hslColor.x < 0.68 || hslColor.x > 0.66){
            hslColor.x = 0.67;
        }
        //增加饱和度
        hslColor.y += 0.3;
    }else if(grayValue > 0.7){
        //添加黄色
        if(hslColor.x < 0.18 || hslColor.x > 0.16){
            hslColor.x = 0.17;
        }
        //降低饱和度
        hslColor.y -= 0.3;
    }
    color = vec4(hsl2rgb(hslColor), color.a);
}
```

|原图|色调分离|
|---|---|
|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/009.jpg)|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-%E6%BB%A4%E9%95%9C/010.jpg)|

[项目github地址](https://github.com/WangYantao/android-camera-demos)