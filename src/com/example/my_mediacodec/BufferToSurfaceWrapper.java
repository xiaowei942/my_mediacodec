package com.example.my_mediacodec;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;


/** Wraps testEncodeDecodeVideoFromBuffer(true) */
class BufferToSurfaceWrapper implements Runnable {
	private static final String TAG = "MYMEDIACODEC";
    // parameters for the decoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final String DEBUG_OUT_FILE_NAME_BASE = "/sdcard/my_decoded.";
    private static final String DEBUG_OUT_FILE_NAME_BASE_IN = "/sdcard/my_encoded.";
	
    private static final long TIMEOUT_USEC = 10000;
    private static final boolean VERBOSE = true;
    private static final boolean OUTPUTBUFFERS = false;
    private static final boolean DEBUG_SAVE_FILE = true;

	long rawSize = 0;
	int decodedframes = 0;
	
    // size of a frame, in pixels
    static int mWidth = -1;
    static int mHeight = -1;
	
    Throwable mThrowable;
    private VideoActivity va;
	static Surface mSurface = null;

	static byte[] bytes = DataExtractor.getBytes();
	static List <Integer> nalu_list = DataExtractor.getNaluList();
	
    BufferToSurfaceWrapper(VideoActivity obj) {
        va = obj;
    	mSurface = obj.mSurface;
    	mWidth = obj.mWidth;
    	mHeight = obj.mHeight;
    }
    
	@Override
	public void run() {
		// TODO Auto-generated method stub
		do{
			;
		}while(DataExtractor.prepared == false);
		System.out.println("Data prepared - ok");
		decodeFromBuffer();
	}
    
    /**
     * Sets the desired frame size and bit rate.
     */
    static void setParameters(int width, int height) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
    }
    
    public void decodeFromBuffer() {
    	setParameters(mWidth, mHeight);

		FileOutputStream outputStream = null;
		FileOutputStream outputStream_out = null;
		
		// Save a copy to disk.
		if(mSurface == null) {
		/*	
            String fileName = DEBUG_OUT_FILE_NAME_BASE_IN + mWidth + "x" + mHeight + ".yuv";
            try {
                outputStream = new FileOutputStream(fileName);
                Log.d(TAG, "decoded output will be saved as " + fileName);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug encoded output file " + fileName);
                throw new RuntimeException(ioe);
            }
        */ 
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
    	
    	boolean decoderconfigured = false;
    	MediaFormat decoderOutputFormat = null;
    	MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    	
    	MediaCodec decoder = MediaCodec.createDecoderByType(MIME_TYPE);
        
        //Merge SPS and PPS to one buffer by set index
        int count = 2;
        int cur = 0;
        
        // Loop until the output side is done.
        boolean inputDone = false;
        boolean encoderDone = false;
        boolean outputDone = false;

		long startMs = System.currentTimeMillis();
		
        while(!outputDone) {
        	if(!inputDone) {
        		int size = 0; 
	    		int inputBufferIndex = 0;
	    		
	        	if(count < nalu_list.size()) {
	        		if(count < 2) {
	        			count++;
	        			continue;
	        		} else if(count == 2) {
	        			size = nalu_list.get(count)-nalu_list.get(cur);
			        	int offset = nalu_list.get(cur);
			        	ByteBuffer bf = ByteBuffer.wrap(bytes, offset, size);
			        	
			        	bf.position(0);
			        	bf.limit(size);
			        	
			        	if(OUTPUTBUFFERS) {
				        	byte []temp = new byte[1024];
				        	bf.get(temp, 0, size);		  
				        	System.out.println("Encoded buffer: " + count);
				        	System.out.println("------");
				        	for(int i=0; i<size; i++)
				        		System.out.print("0x" + Integer.toHexString(temp[i]) + "  ");
				        	System.out.println("\n------");
			        	}
			        	MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
			        	format.setByteBuffer("csd-0", bf);
			        	//format.setInteger("color-format", 19);
		            	decoder.configure(format, mSurface, null, 0);
		                decoder.start();
		                
		                decoderconfigured = true;
		                decoderInputBuffers = decoder.getInputBuffers();
		                decoderOutputBuffers = decoder.getOutputBuffers();
		                cur = count;
		                count++;
		                continue;
	        		} else {
		        		inputBufferIndex = decoder.dequeueInputBuffer(-1);
		        		if(inputBufferIndex >= 0) {
    			        	size = nalu_list.get(count)-nalu_list.get(cur);
    			        	int offset = nalu_list.get(cur);
    			        	ByteBuffer bf = ByteBuffer.wrap(bytes, offset, size);

    			        	bf.position(0);
    			        	bf.limit(size);

    			        	if(OUTPUTBUFFERS) {
    			        	byte []temp = new byte[1024];
    				        	bf.get(temp, 0, size);		  
    				        	System.out.println("Encoded buffer: " + count);
    				        	System.out.println("------");
    				        	for(int i=0; i<size; i++)
    				        		System.out.print("0x" + Integer.toHexString(temp[i]) + "  ");
    				        	System.out.println("------");
    			        	}

    			        	ByteBuffer inputBuffer = decoderInputBuffers[inputBufferIndex];
    			    		inputBuffer.clear();
    			        	inputBuffer.put(bytes, offset, size);
    			        	if(count == 3) {
    				        	decoder.queueInputBuffer(inputBufferIndex, 0, size, count*1000, 0);
    				        	if (VERBOSE) Log.d(TAG, "passed " + size + " bytes to decoder" + " with flags - " + 1);
    		        		} else {
    		        			decoder.queueInputBuffer(inputBufferIndex, 0, size, count*1000, 0);
    				        	if (VERBOSE) Log.d(TAG, "passed " + size + " bytes to decoder" + " with flags - " + 0);
    		        		}

    			        	cur = count;
    			        	count++;
		        		}
	        		}
	        	} else {
	        		inputDone = true;
	        		inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
	        		if( inputBufferIndex >= 0) {
    		        	decoder.queueInputBuffer(inputBufferIndex, 0, size, count*1000, MediaCodec.BUFFER_FLAG_END_OF_STREAM);	
    		        	
    		        	if (VERBOSE) Log.d(TAG, "passed " + size + " bytes to decoder" + " with flags - " + MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                + (encoderDone ? " (EOS)" : ""));
	        		}
	        	}	
        	}

        	if(decoderconfigured) {
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
	                
				    if(mSurface == null)
				    {
    	                //outputFrame.position(info.offset);
    	                //outputFrame.limit(info.offset + info.size);
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
					}    
					
	                if (info.size == 0) {
	                    if (VERBOSE) Log.d(TAG, "got empty frame");
	                } else {
	                    if (VERBOSE) Log.d(TAG, "decoded, checking frame " + decodedframes++);
	                }
					
	                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
	                    if (VERBOSE) Log.d(TAG, "output EOS");
	                    outputDone = true;
	                }
	                
	            /*    
	                long curr = info.presentationTimeUs/1000;
	                long off = System.currentTimeMillis() - startMs;
    				while ( true ) {//curr > off) {
    					try {
    						Thread.sleep(50);
    					} catch (InterruptedException e) {
    						e.printStackTrace();
    						break;
    					}
    				}
    			*/

					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
					
    				if(mSurface == null) {
    	                decoder.releaseOutputBuffer(decoderStatus, false /*render*/);
    				} else {
    					decoder.releaseOutputBuffer(decoderStatus, true /*render*/);
    				}
	            }
        	}
        }
        while(true) {
            System.out.println("Sleep");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
