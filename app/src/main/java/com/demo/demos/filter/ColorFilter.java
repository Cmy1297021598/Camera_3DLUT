package com.demo.demos.filter;

import com.demo.demos.R;
import com.demo.demos.utils.CubeUtils;
import com.demo.demos.utils.GLUtil;

import static android.opengl.GLES30.*;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.FloatBuffer;

/**
 * Created by wangyt on 2019/5/27
 */
public class ColorFilter extends BaseFilter {

    public static final String UNIFORM_COLOR_FLAG  = "colorFlag";
    public static final String UNIFORM_TEXTURE_LUT = "textureLUT";
    public static final String UNIFORM_CUBE_LUT    = "sizeFlag";

    public static final String UNIFORM_tex2DLut = "tex2DLUT";
    public static final String UNIFORM_LUT_SIZE = "lut_size";
    public static final String UNIFORM_LUT_QSZ  = "lut_qsz";
    public static final String UNIFORM_LUT_sizeBack   = "sizeBackDiv";
    public static final String UNIFORM_LUT_allSizeDiv = "allSizeDiv";

    public static int COLOR_FLAG = 0;
    public static int CUBE_SIZE = 0;
    public static int COLOR_FLAG_USE_LUT   = 6;
    public static int COLOR_FLAG_USE_LUT2D = 11;

    public int hColorFlag;
    public int hCubeSize;
    public int hTextureLUT, hTex2DLut, hlutsize, hlutqsz, hsizeBack, hallSizeDiv;
    private int LUTTextureId, LutTex2DId, Lut_Size, Lut_QSZ;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSurfaceCreated() {
        super.onSurfaceCreated();
//      LUTTextureId = GLUtil.loadTextureFromRes(R.drawable.amatorka);

        String fL_cube = "/sdcard/3dlut.cube";
        //String fL_cube = "/data/local/tmp/3D_LUT_4701_33point.cube";

        LUTTextureId = GLUtil.loadTextureFromRes(CubeUtils.encode(fL_cube));

        //int lutsize[] = new int[1];
        //FloatBuffer fbuf = CubeUtils.encode2(lutsize,fL_cube);
        //LutTex2DId = GLUtil.loadTextureFromRes2D(fbuf,lutsize[0]);

        int lut_size[] = new int[1];
        FloatBuffer fbuf = CubeUtils.encode2(lut_size, fL_cube);
        Lut_Size = lut_size[0];

        int lut_qsz[] = new int[1];
        LutTex2DId = GLUtil.loadTextureFromRes2D2(lut_qsz, fbuf, lut_size[0]);
        Lut_QSZ    = lut_qsz[0];
    }

    @Override
    public int initProgram() {
        return GLUtil.createAndLinkProgram(R.raw.texture_vertex_shader, R.raw.texture_color_fragtment_shader);
    }

    @Override
    public void initAttribLocations() {
        super.initAttribLocations();

        hColorFlag = glGetUniformLocation(program, UNIFORM_COLOR_FLAG);
        hCubeSize = glGetUniformLocation(program,UNIFORM_CUBE_LUT);
        hTextureLUT = glGetUniformLocation(program, UNIFORM_TEXTURE_LUT);

        hTex2DLut = glGetUniformLocation(program, UNIFORM_tex2DLut);
        hlutsize  = glGetUniformLocation(program, UNIFORM_LUT_SIZE);
        hlutqsz  = glGetUniformLocation(program, UNIFORM_LUT_QSZ);

        hsizeBack  = glGetUniformLocation(program, UNIFORM_LUT_sizeBack);
        hallSizeDiv  = glGetUniformLocation(program, UNIFORM_LUT_allSizeDiv);

    }

    @Override
    public void setExtend() {
        super.setExtend();
        glUniform1i(hColorFlag, COLOR_FLAG);
        glUniform1i(hCubeSize,  CUBE_SIZE);
    }

    @Override
    public void bindTexture() {
        super.bindTexture();
        if (COLOR_FLAG == COLOR_FLAG_USE_LUT){
            glActiveTexture(GL_TEXTURE0 + 1);
            glBindTexture(GL_TEXTURE_2D, LUTTextureId);
            glUniform1i(hTextureLUT, 1);

        }
        else if(COLOR_FLAG == COLOR_FLAG_USE_LUT2D){
            glActiveTexture(GL_TEXTURE0 + 1);
            glBindTexture(GL_TEXTURE_2D, LutTex2DId);
            glUniform1i(hTex2DLut, 1);

            glUniform1f(hlutsize,Lut_Size);
            glUniform1f(hlutqsz,Lut_QSZ);
            glUniform1f(hsizeBack,1.0f / Lut_QSZ);
            glUniform1f(hallSizeDiv,1.0f /(Lut_Size * Lut_QSZ));

        }
    }
}
