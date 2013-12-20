package com.example.my_mediacodec;

import android.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


public class VideoActivity extends Activity implements SurfaceHolder.Callback{
	private static final String PATH = "/sdcard/test.h264";
	final boolean HASSURFACE = true;
 
	Surface mSurface = null;
	int mWidth = 704;
	int mHeight = 480;
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SurfaceView sv = new SurfaceView(this);
		sv.getHolder().addCallback(this);

		//setContentView(R.layout.activity_list_item);
		setContentView(sv);

		while(DataExtractor.prepared) {
			for(int count = 0; count<BufferToSurfaceWrapper.nalu_list.size(); count++)
				System.out.println("Count: " + count + "  Value: " + BufferToSurfaceWrapper.nalu_list.get(count).toString());
		}
		
		DataExtractor de = new DataExtractor(PATH);
		de.run();	
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		if(HASSURFACE) {
			mSurface = holder.getSurface();
		} else {
			mSurface = null;
		}
		
		try {
			runTest(this);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.out.println("Test thread exception");
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Entry point.
     */
    public static void runTest(VideoActivity obj) throws Throwable {
    	
        BufferToSurfaceWrapper wrapper = new BufferToSurfaceWrapper(obj);
        Thread th = new Thread(wrapper, "codec test");
        th.start();
        //th.join();
        if (wrapper.mThrowable != null) {
            throw wrapper.mThrowable;
        }
    }
}
