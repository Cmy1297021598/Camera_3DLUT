package com.demo.demos.filter;

import android.opengl.GLES11Ext;

import com.demo.demos.R;
import com.demo.demos.utils.GLUtil;

import static android.opengl.GLES30.*;

/**
 * Created by wangyt on 2019/5/24
 */
public class OesFilter extends BaseFilter{

    public OesFilter() {
        super();
    }

    @Override
    public int initProgram() {
        return GLUtil.createAndLinkProgram(R.raw.texture_vertex_shader, R.raw.texture_oes_fragtment_shader);
    }

    @Override
    public void bindTexture() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId()[0]);
        glUniform1i(hTexture, 0);
    }
}
