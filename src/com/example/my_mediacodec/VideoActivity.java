package com.example.my_mediacodec;

import android.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
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
	private static final String TAG = "MYMEDIACODEC";
    private static final String DEBUG_OUT_FILE_NAME_BASE = "/sdcard/my_decoded.";
	
    // parameters for the decoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    
	private static final String PATH = "/sdcard/test.h264";

	private static final boolean VERBOSE = true;

	private static final long TIMEOUT_USEC = 10000;
	private static final boolean DEBUG_SAVE_FILE = true;

	List <Integer> nalu_list = DataExtractor.getNaluList();
	byte[] bytes = DataExtractor.getBytes();
	Surface mSurface = null;
	
    // size of a frame, in pixels
    private int mWidth = -1;
    private int mHeight = -1;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_item);
		DataExtractor de = new DataExtractor(PATH);
		de.run();	
		
		try {
			BufferToSurfaceWrapper.runTest(this);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.out.println("Test thread exception");
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		while(DataExtractor.prepared) {
			for(int count = 0; count<nalu_list.size(); count++)
				System.out.println("Count: " + count + "  Value: " + nalu_list.get(count).toString());
		}
		
	
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mSurface = holder.getSurface();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	 /** Wraps testEncodeDecodeVideoFromBuffer(true) */
    private static class BufferToSurfaceWrapper implements Runnable {
        private Throwable mThrowable;
        private VideoActivity mTest;
        
        private BufferToSurfaceWrapper(VideoActivity test) {
            mTest = test;
        }
        
		@Override
		public void run() {
			// TODO Auto-generated method stub
			do{
				;
			}while(DataExtractor.prepared == false);
			System.out.println("Data prepared - ok");
			mTest.decodeFromBuffer();
		}
    	
		/**
         * Entry point.
         */
        public static void runTest(VideoActivity obj) throws Throwable {
            BufferToSurfaceWrapper wrapper = new BufferToSurfaceWrapper(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }
    /**
     * Sets the desired frame size and bit rate.
     */
    private void setParameters(int width, int height) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
    }
	   
    public void decodeFromBuffer() {
    	setParameters(320, 240);
    	
		long rawSize = 0;
		
		// Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
		// stream, not a .mp4 file, so not all players will know what to do with it.
		FileOutputStream outputStream = null;
		FileOutputStream outputStream_out = null;
		
        if (DEBUG_SAVE_FILE) {
            String fileName_out = DEBUG_OUT_FILE_NAME_BASE + mWidth + "x" + mHeight + ".yuv";
            try {
                outputStream_out = new FileOutputStream(fileName_out);
                Log.d(TAG, "decoded output will be saved as " + fileName_out);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug decode output file " + fileName_out);
                throw new RuntimeException(ioe);
            }
        }

    	ByteBuffer[] decoderInputBuffers = null;
    	ByteBuffer[] decoderOutputBuffers = null;
    	
    	MediaFormat decoderOutputFormat = null;
    	MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    	
    	MediaCodec decoder = MediaCodec.createDecoderByType(MIME_TYPE);
    	MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
    	decoder.configure(format, null, null, 0);
        decoder.start();
        decoderInputBuffers = decoder.getInputBuffers();
        decoderOutputBuffers = decoder.getOutputBuffers();
        int count = 2;
        int cur = 0;
        
        // Loop until the output side is done.
        boolean inputDone = false;
        boolean encoderDone = false;
        boolean outputDone = false;
        
        while(true) {
        	if(!inputDone) {
        		int size = 0; 
	    		int inputBufferIndex = decoder.dequeueInputBuffer(-1);
	    		
	        	if(count<nalu_list.size()) {
		        	size = nalu_list.get(count)-nalu_list.get(cur);
		        	int offset = nalu_list.get(cur);
		        	ByteBuffer bf = ByteBuffer.wrap(bytes, offset, size);
		        	byte []temp = new byte[1024];
		        	bf.get(temp, 0, size);
		        	System.out.println("Encoded buffer: ------");
		        	for(int i=0; i<size; i++)
		        	System.out.print("0x" + Integer.toHexString(temp[i]) + "  ");
		
		        	System.out.println("------");
		    		ByteBuffer inputBuffer = decoderInputBuffers[inputBufferIndex];
		    		inputBuffer.clear();
		        	inputBuffer.put(bf);
		        	decoder.queueInputBuffer(inputBufferIndex, 0, size, count, 0);
		        	
		        	cur = count;
		        	count++;
	        	} else {
	        		inputDone = true;
		        	decoder.queueInputBuffer(inputBufferIndex, 0, size, count, MediaCodec.BUFFER_FLAG_END_OF_STREAM);	        		
	        	}
        	}
        	
        	int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (VERBOSE) 
                	Log.d(TAG, "no output from decoder available");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // The storage associated with the direct ByteBuffer may already be unmapped,
                // so attempting to access data through the old output buffer array could
                // lead to a native crash.
                if (VERBOSE) 
                	Log.d(TAG, "decoder output buffers changed");
                decoderOutputBuffers = decoder.getOutputBuffers();
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // this happens before the first frame is returned
                decoderOutputFormat = decoder.getOutputFormat();
                if (VERBOSE) 
                	Log.d(TAG, "decoder output format changed: " + decoderOutputFormat);
            } else if (decoderStatus < 0) {
                //fail("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
            } else {  // decoderStatus >= 0
                ByteBuffer outputFrame = decoderOutputBuffers[decoderStatus];

                outputFrame.position(info.offset);
                outputFrame.limit(info.offset + info.size);
                rawSize += info.size;
                
                if (outputStream_out != null) {
                    byte[] data = new byte[info.size];
                    outputFrame.get(data);
                    outputFrame.position(info.offset);
                    try {
                        outputStream_out.write(data);
                    } catch (IOException ioe) {
                        Log.w(TAG, "failed writing debug data to file");
                        throw new RuntimeException(ioe);
                    }
                    Log.w(TAG, "successful writing debug data to file");
                }
                
                if (info.size == 0) {
                    if (VERBOSE) Log.d(TAG, "got empty frame");     
                }
                decoder.releaseOutputBuffer(decoderStatus, false);
            }
        }
    }
}
