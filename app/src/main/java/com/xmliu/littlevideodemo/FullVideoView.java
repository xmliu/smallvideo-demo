package com.xmliu.littlevideodemo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class FullVideoView extends VideoView {

	public FullVideoView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public FullVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FullVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(0, widthMeasureSpec);
		int height = getDefaultSize(0, heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

}
