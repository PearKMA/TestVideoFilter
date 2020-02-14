package com.hunglv.testvideofilter.render;

import android.content.Context;
import android.opengl.GLES20;

import cn.ezandroid.ezfilter.extra.IAdjustable;
import cn.ezandroid.ezfilter.extra.MultiBitmapInputRender;

/**
 * LookupRender
 *
 * @author like
 * @date 2017-05-16
 */
public class LookupRender extends MultiBitmapInputRender implements IAdjustable {

    private final String UNIFORM_MIX = "u_mix";
    private int mMixHandle;
    private float mMix = 1f;

    public LookupRender(Context context, int id) {
        super(context, new int[]{id});
    }

    @Override
    protected String getFragmentShader() {
        return "precision highp float;\n"+
                "\n"+
                "varying vec2 textureCoordinate;\n"+
                "\n"+
                "uniform sampler2D inputImageTexture;\n"+
                "uniform sampler2D inputImageTexture2;\n"+
                "\n"+
                "uniform mediump float u_mix;\n"+
                "\n"+
                "const float minLimit = 0.01;\n"+
                "const float maxLimit = 0.99;\n"+
                "\n"+
                "const float size = 8.0;\n"+
                "const float aa = 1.0 / size;\n"+
                "const float bb = 0.5 / 1728.0;\n"+
                "\n"+
                "void main()\n"+
                "{\n    "+
                "vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n  "+
                "\n    "+
                "float blueColor = clamp(textureColor.b, minLimit, maxLimit) * (size * + size - 1.0);\n    "+
                "\n    "+
                "vec2 quad1;\n    "+
                "quad1.y = floor(floor(blueColor) * aa);\n    "+
                "quad1.x = floor(blueColor) - (quad1.y * size);\n    "+
                "\n    "+
                "vec2 quad2;\n    "+
                "quad2.y = floor(ceil(blueColor) * aa);\n    "+
                "quad2.x = ceil(blueColor) - (quad2.y * size);\n    "+
                "\n    "+
                "vec2 texPos1;\n    "+
                "texPos1.x = (quad1.x * aa) + bb + ((aa - bb * 2.0) * clamp(textureColor.r, minLimit, maxLimit));\n  "+
                "texPos1.y = (quad1.y * aa) + bb + ((aa - bb * 2.0) * clamp(textureColor.g, minLimit, maxLimit));\n "+
                "\n    "+
                "vec2 texPos2;\n    "+
                "texPos2.x = (quad2.x * aa) + bb + ((aa - bb * 2.0) * clamp(textureColor.r, minLimit, maxLimit));\n   "+
                "texPos2.y = (quad2.y * aa) + bb + ((aa - bb * 2.0) * clamp(textureColor.g, minLimit, maxLimit));\n   "+
                "\n    "+
                "\n    "+
                "vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n    "+
                "vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n    "+
                "\n    "+
                "vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n    "+
                "vec4 fragColor = vec4(newColor.rgb, textureColor.w);\n    "+
                "gl_FragColor = mix(textureColor, fragColor, u_mix);\n"+
                "}";
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        mMixHandle = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_MIX);
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        GLES20.glUniform1f(mMixHandle, mMix);
    }

    @Override
    public void adjust(float progress) {
        mMix = progress;
    }
}