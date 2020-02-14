package com.hunglv.testvideofilter.render;

import android.content.Context;
import android.opengl.GLES20;

import cn.ezandroid.ezfilter.extra.IAdjustable;
import cn.ezandroid.ezfilter.extra.MultiBitmapInputRender;

public class BlendOverlayRender extends MultiBitmapInputRender implements IAdjustable {

    private final String UNIFORM_MIX = "amount";
    private int mMixHandle;
    private float mMix = 0.6f;

    public BlendOverlayRender(Context context, int id) {
        super(context, new int[]{id});
    }
    @Override
    protected String getFragmentShader() {
        return "precision highp float;\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform sampler2D inputImageTexture2;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform float amount;\n" +
                "\n" +
                "void main()\n" +
                "{\n    " +
                "    lowp vec4 c1 = texture2D(inputImageTexture, textureCoordinate);\n    " +
                "    lowp vec4 c2 = texture2D(inputImageTexture2, vec2(textureCoordinate.x, 1.0 - textureCoordinate.y));\n" +
                "    c2.a = c2.a * amount;\n" +
                "\n" +
                "    lowp vec4 outputColor;\n" +
                " \n" +
                "    outputColor.r = c2.r + c1.r * c1.a * (1.0 - c2.a);\n" +
                "\n" +
                "    outputColor.g = c2.g + c1.g * c1.a * (1.0 - c2.a);\n" +
                " \n" +
                "    outputColor.b = c2.b + c1.b * c1.a * (1.0 - c2.a);\n" +
                "\n" +
                "    outputColor.a = c2.a + c1.a * (1.0 - c2.a);\n" +
                "\n" +
                "    gl_FragColor = outputColor;\n}";
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

    @Override
    public float getProgress() {
        return 0;
    }
}
