# android-camera2相机开发【9】-使用opengl实现LUT滤镜

上一篇文章简单的实现了几个常见的滤镜效果，针对每一种滤镜，在片段着色器中编写响应的计算逻辑。

但是，随便一个美颜app、短视频app的滤镜都太多了，总不能一个一个写代码去吧，况且好些滤镜特别接近，就让程序员看一眼，怎么写计算逻辑。。

其实，这种风格化的滤镜是通过把原图的像素颜色经过过处理，变成另一种颜色来实现的，所以简单的方法就是使用LUT方法，通过设计师提供的LUT文件来实现预定的滤镜效果。

具体LUT是什么，我这里就不再赘述了，网上很多文章，这里推荐一个 [lut](https://www.jianshu.com/p/f054464e1b40)

LUT实现滤镜的基本思路：

1. 准备LUT文件
2. 加载LUT文件加载到opengl纹理
3. 将纹理传递到到片段着色器中
4. 根据LUT，在片段着色器中对图像的颜色值进行映射，得到滤镜后的颜色进行输出

LUT文件可以自己制作，也可以从别人项目中拷贝。

LUT文件到opengl纹理

```java
    public static int loadTextureFromRes(int resId){
        //创建纹理对象
        int[] textureId = new int[1];
        //生成纹理：纹理数量、保存纹理的数组，数组偏移量
        glGenTextures(1, textureId,0);
        if(textureId[0] == 0){
            Log.e(TAG, "创建纹理对象失败");
        }
        //原尺寸加载位图资源（禁止缩放）
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
        if (bitmap == null){
            //删除纹理对象
            glDeleteTextures(1, textureId, 0);
            Log.e(TAG, "加载位图失败");
        }
        //绑定纹理到opengl
        glBindTexture(GL_TEXTURE_2D, textureId[0]);
        //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        //将位图加载到opengl中，并复制到当前绑定的纹理对象上
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        //释放bitmap资源（上面已经把bitmap的数据复制到纹理上了）
        bitmap.recycle();
        //解绑当前纹理，防止其他地方以外改变该纹理
        glBindTexture(GL_TEXTURE_2D, 0);
        //返回纹理对象
        return textureId[0];
    }
```

LUT纹理绑定到片段着色器

```java
            //amatorka 就是lut文件
            int LUTTextureId = GLUtil.loadTextureFromRes(R.drawable.amatorka);
            int hTextureLUT = glGetUniformLocation(program, "textureLUT");
            glActiveTexture(GL_TEXTURE0 + 1);
            glBindTexture(GL_TEXTURE_2D, LUTTextureId);
            glUniform1i(hTextureLUT, 1);
```

片段着色器代码

```glsl
#version 300 es
precision mediump float;

in vec2 v_texCoord;
out vec4 outColor;
uniform sampler2D s_texture;
uniform sampler2D textureLUT;

//查找表滤镜
vec4 lookupTable(vec4 color){
    float blueColor = color.b * 63.0;

    vec2 quad1;
    quad1.y = floor(floor(blueColor) / 8.0);
    quad1.x = floor(blueColor) - (quad1.y * 8.0);
    vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);

    vec2 texPos1;
    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.r);
    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.g);
    vec2 texPos2;
    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.r);
    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * color.g);
    vec4 newColor1 = texture(textureLUT, texPos1);
    vec4 newColor2 = texture(textureLUT, texPos2);
    vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
    return vec4(newColor.rgb, color.w);
}

void main(){
    vec4 tmpColor = texture(s_texture, v_texCoord);
    outColor = lookupTable(tmpColor);
}
```

效果图如下

|原图|lut滤镜|
|---|---|
|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-lut%E6%BB%A4%E9%95%9C/001.jpg)|![](https://wangyt-imgs.oss-cn-beijing.aliyuncs.com/blog/android-%E7%9B%B8%E6%9C%BA%E5%BC%80%E5%8F%91-lut%E6%BB%A4%E9%95%9C/002.jpg)|

[项目github地址](https://github.com/WangYantao/android-camera-demos)