package edu.dartmouth.cs.audiorecorder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;
import edu.dartmouth.cs.mltoolkit.processing.AudioFeatureExtraction;
import edu.dartmouth.cs.mltoolkit.processing.AudioInference;

@TargetApi(3)
public class RehearsalAudioRecorder extends Activity {
	/**
	 * INITIALIZING : recorder is initializing; READY : recorder has been
	 * initialized, recorder not yet started RECORDING : recording ERROR :
	 * reconstruction needed STOPPED: reset needed
	 */
	public enum State {
		INITIALIZING, READY, RECORDING, ERROR, STOPPED
	};
	public static int ones;
	public static int zeros=1;
	public static double percent = (double)ones/((double)ones+(double)zeros)*100;
	public static final boolean RECORDING_UNCOMPRESSED = true;
	public static final boolean RECORDING_COMPRESSED = false;
	private static final String TAG = "RehearsalAudioRecorder";

	// Recorder used for uncompressed recording
	private AudioRecord aRecorder = null;

	// Output file path
	private String fPath = null;

	// Recorder state; see State
	private State state;

	// File writer
	private DataOutputStream mDataOutput;

	// Number of channels, sample rate, sample size(size in bits), buffer size,
	// audio source, sample size(see AudioFormat)
	private short nChannels;
	private int sRate;
	private short bSamples;
	private int bufferSize;
	private int aSource;
	private int aFormat;
	private int aChannelConfig;

	private boolean mWriteToFile;

	private int frameSize;
	private int windowSize;

	// = new double[af.getFrame_feature_size()];
	//double [] audioWindowFeature;// = new double[af.getWindow_feature_size()];

	// Number of frames written to file on each output(only in uncompressed
	// mode)
	private int framePeriod;

	// Buffer for output(only in uncompressed mode)
	private short[] buffer;

	// Number of bytes written to file after header(only in uncompressed mode)
	// after stop() is called, this size is written to the header/data chunk in
	// the wave file
	private int payloadSize;
	
	private static HashMap<Double, Double> stressMap = new HashMap<Double, Double>();
	
	private AudioInference audioInf;
	private BlockingQueue<AudioData> mAudioQueue;
	private AudioRawDataPool mAudioRawDataPool;
	private AudioDataPool mAudioDataPool;
	private Thread mAudioProcessingThread1;
	private Thread mAudioProcessingThread2;

	/**
	 * 
	 * Returns the state of the recorder in a RehearsalAudioRecord.State typed
	 * object. Useful, as no exceptions are thrown.
	 * 
	 * @return recorder state
	 */
	public State getState() {
		return state;
	}

	/*
	 * 
	 * Method used for recording.
	 */
	private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
		@Override
		public void onPeriodicNotification(AudioRecord recorder) {
			int numRead = aRecorder.read(buffer, 0, buffer.length); // Fill
			// buffer
			if (numRead != AudioRecord.ERROR_INVALID_OPERATION && numRead != AudioRecord.ERROR_BAD_VALUE) {
				if (mWriteToFile) {
					try {
						// Write buffer to file
						for (int i = 0; i < numRead; ++i) {
							mDataOutput.writeShort(Short.reverseBytes(buffer[i]));
						}
						mDataOutput.flush();
					} catch (IOException e) {
						Log.e(TAG, "Error occured in updateListener, recording is aborted");
						stop();
						return;
					}
				}
				AudioData audioData = mAudioDataPool.borrowObject();
				audioData.setValues(buffer, numRead);
				boolean successfullyInserted = mAudioQueue.offer(audioData);
				if (!successfullyInserted) {
					mAudioRawDataPool.returnObject(buffer);
					mAudioDataPool.returnObject(audioData);
				}
				buffer = mAudioRawDataPool.borrowObject();
				payloadSize += numRead;
			} else {
				Log.e(TAG, "Error occured in updateListener, recording is aborted");
				stop();
			}
		}

		@Override
		public void onMarkerReached(AudioRecord recorder) {
			// NOT USED
		}
	};

	public RehearsalAudioRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat) {
		this(audioSource, sampleRate, channelConfig, audioFormat, false);
	}

	/**
	 * 
	 * 
	 * Default constructor
	 * 
	 * Instantiates a new recorder, in case of compressed recording the
	 * parameters can be left as 0. In case of errors, no exception is thrown,
	 * but the state is set to ERROR
	 * 
	 */
	public RehearsalAudioRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat,
			boolean writeToFile) {
		mWriteToFile = writeToFile;
		aChannelConfig = channelConfig;
		try {
			if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
				bSamples = 16;
			} else {
				bSamples = 8;
			}

			if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
				nChannels = 1;
			} else {
				nChannels = 2;
			}

			aSource = audioSource;
			sRate = sampleRate;
			aFormat = audioFormat;

			if (sampleRate < 11000) {
				// 40 256 frame
				frameSize = 256;
				windowSize = 40;
				framePeriod = frameSize * windowSize;
			} else if (sampleRate < 22050) {
				framePeriod = 2048;
			} else if (sampleRate < 44100) {
				framePeriod = 4096;
			} else {
				framePeriod = 8192;
			}
			bufferSize = (framePeriod * 2 * bSamples * nChannels) / Short.SIZE;
			
			
			/*
			 * Check to make sure buffer size is not smaller than the smallest
			 * allowed one
			 */
			if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
				bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2;
				// Set frame period and timer interval accordingly
				framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
				Log.w(TAG, "Increasing buffer size to " + Integer.toString(bufferSize));
			}
			mAudioDataPool = new AudioDataPool();
			mAudioRawDataPool = new AudioRawDataPool(bufferSize);
			mAudioQueue = new ArrayBlockingQueue<RehearsalAudioRecorder.AudioData>(100);

			aRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
			if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
				throw new Exception("AudioRecord initialization failed");
			aRecorder.setRecordPositionUpdateListener(updateListener);
			aRecorder.setPositionNotificationPeriod(framePeriod);
			fPath = null;
			state = State.INITIALIZING;
		} catch (Exception e) {
			if (e.getMessage() != null) {
				Log.e(TAG, e.getMessage());
			} else {
				Log.e(TAG, "Unknown error occured while initializing recording");
			}
			state = State.ERROR;
		}
	}

	public static HashMap<Double, Double> insertStressLevel(double date) {
		stressMap.put(date,percent);
		return null;
	}
	
	/**
	 * Sets output file path, call directly after construction/reset.
	 * 
	 * @param output
	 *            file path
	 * 
	 */
	public void setOutputFile(String argPath) {
		try {
			if (state == State.INITIALIZING) {
				fPath = argPath;
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				Log.e(TAG, e.getMessage());
			} else {
				Log.e(TAG, "Unknown error occured while setting output path");
			}
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * Prepares the recorder for recording, in case the recorder is not in the
	 * INITIALIZING state and the file path was not set the recorder is set to
	 * the ERROR state, which makes a reconstruction necessary. In case
	 * uncompressed recording is toggled, the header of the wave file is
	 * written. In case of an exception, the state is changed to ERROR
	 * 
	 */
	public void prepare() {
		try {
			if (state == State.INITIALIZING) {
				if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED)) {
					// write file header
					if (mWriteToFile && fPath != null) {
						mDataOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fPath)));
						// Set file length to 0, to prevent unexpected behavior
						// in case the file already existed
						mDataOutput.writeBytes("RIFF");
						mDataOutput.writeInt(0); // Final file size not known
						// yet,
						// write 0
						mDataOutput.writeBytes("WAVE");
						mDataOutput.writeBytes("fmt ");
						/* Sub-chunk size, 16 for PCM */
						mDataOutput.writeInt(Integer.reverseBytes(16));
						/* AudioFormat, 1 for PCM */
						mDataOutput.writeShort(Short.reverseBytes((short) 1));
						/* Number of channels, 1 formono, 2 for stereo */
						mDataOutput.writeShort(Short.reverseBytes(nChannels));
						// Sample rate
						mDataOutput.writeInt(Integer.reverseBytes(sRate));
						// Byte rate
						mDataOutput.writeInt(Integer.reverseBytes(sRate * bSamples * nChannels / 8));
						// Block align
						mDataOutput.writeShort(Short.reverseBytes((short) (nChannels * bSamples / 8)));
						// Bits per sample
						mDataOutput.writeShort(Short.reverseBytes(bSamples));
						mDataOutput.writeBytes("data");
						// Data chunk size not known yet, write 0
						mDataOutput.writeInt(0);
					}

					// buffer = new short[bufferSize];
					buffer = mAudioRawDataPool.borrowObject();
					state = State.READY;
				} else {
					Log.e(TAG, "prepare() method called on uninitialized recorder");
					state = State.ERROR;
				}
			} else {
				Log.e(TAG, "prepare() method called on illegal state");
				release();
				state = State.ERROR;
			}
		} catch (Exception e) {
			if (e.getMessage() != null) {
				Log.e(TAG, e.getMessage());
			} else {
				Log.e(TAG, "Unknown error occured in prepare()");
			}
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 * Releases the resources associated with this class, and removes the
	 * unnecessary files, when necessary
	 * 
	 */
	public void release() {
		if (state == State.RECORDING) {
			stop();
		} else {
			if (state == State.READY) {
				try {
					if (mWriteToFile) {
						mDataOutput.close(); // Remove prepared file
					}
				} catch (IOException e) {
					Log.e(TAG, "I/O exception occured while closing output file");
				}
				if (mWriteToFile) {
					(new File(fPath)).delete();
				}
			}
		}

		if (aRecorder != null) {
			aRecorder.release();
		}
	}

	/**
	 * 
	 * 
	 * Resets the recorder to the INITIALIZING state, as if it was just created.
	 * In case the class was in RECORDING state, the recording is stopped. In
	 * case of exceptions the class is set to the ERROR state.
	 * 
	 */
	public void reset() {
		try {
			if (state != State.ERROR) {
				release();
				fPath = null; // Reset file path
				aRecorder = new AudioRecord(aSource, sRate, aChannelConfig, aFormat, bufferSize);
				aRecorder.setRecordPositionUpdateListener(updateListener);
				aRecorder.setPositionNotificationPeriod(framePeriod);
				state = State.INITIALIZING;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 * Starts the recording, and sets the state to RECORDING. Call after
	 * prepare().
	 * 
	 */
	public void start() {
		if (state == State.READY) {
			mAudioProcessingThread1 = new Thread(new AudioProcessing("stresstoolkit"), "Audio processing");
			mAudioProcessingThread1.start();
			mAudioProcessingThread2 = new Thread(new AudioProcessing("stresstoolkit"), "Audio processing2");
			mAudioProcessingThread2.start();
			payloadSize = 0;
			aRecorder.startRecording();
			aRecorder.read(buffer, 0, buffer.length);
			state = State.RECORDING;
		} else {
			Log.e(TAG, "start() called on illegal state");
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * Stops the recording, and sets the state to STOPPED. Only the first call
	 * to stop() has effects. In case of further usage, a reset is needed. Also
	 * finalizes the wave file in case of uncompressed recording.
	 * 
	 */
	public void stop() {
		if (state == State.STOPPED) {
			return;
		}
		if (state == State.RECORDING) {
			aRecorder.stop();
			mAudioProcessingThread1.interrupt();
			mAudioProcessingThread2.interrupt();
			try {
				mAudioProcessingThread1.join(2000);
				mAudioProcessingThread2.join(2000);
			} catch (InterruptedException e) {
				Log.e(TAG, "Unable to stop processing thread");
			}
			while (!mAudioQueue.isEmpty()) {
				AudioData data = mAudioQueue.poll();
				if (data != null) {
					mAudioRawDataPool.returnObject(data.mData);
					mAudioDataPool.returnObject(data);
				}
			}

			if (mWriteToFile) {
				try {
					mDataOutput.flush();
					mDataOutput.close();
					int sizeToWrite = payloadSize * 2;
					RandomAccessFile fWriter = new RandomAccessFile(fPath, "rw");
					fWriter.seek(4); // Write size to RIFF header
					fWriter.writeInt(Integer.reverseBytes(36 + sizeToWrite));

					fWriter.seek(40); // Write size to Subchunk2Size field
					fWriter.writeInt(Integer.reverseBytes(sizeToWrite));

					fWriter.close();
				} catch (IOException e) {
					Log.e(TAG, "I/O exception occured while closing output file");
					state = State.ERROR;
				}
			}
			state = State.STOPPED;
		} else {
			Log.e(TAG, "stop() called on illegal state");
			state = State.ERROR;
		}
	}

	private class AudioData {
		public short[] mData;
		public int mSize;

		public void setValues(short[] data, int size) {
			this.mData = data;
			this.mSize = size;
		}
	}

	private class AudioProcessing implements Runnable {

		private long mCount = 0;
		private AudioInference infer;		
		private AudioFeatureExtraction features;
		private	double[]	audioFrameFeature;

		public AudioProcessing(String lib){
			//features = new AudioFeatureExtraction(lib,frameSize, windowSize, 20, 8000);
			features = new AudioFeatureExtraction(frameSize, windowSize,24, 20, 8000);	
			audioFrameFeature = new double[features.getFrame_feature_size()];
		}

		@Override
		public void run() {
			AudioData audiodata = null;
			short[] data = new short[framePeriod];
			float[] rdata = new float[framePeriod];
			double[] fdata = new double[framePeriod];
			final int row = features.getWindow_length();
			final int col = features.getFrame_length();
			short[][] data_buffer = new short[row][col];
			double[][] fdata_buffer = new double[row][col];
			double[] rms = new double[row];
			double[] zcr = new double[row];
			int[] teager_index = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};//
			//int[] teager_index = new int[]{2,6,7,8,9,10,11,17}; 
			final int nmfcc = 20;
			double [][] tdata = new double[teager_index.length][framePeriod];
			double[][] tdata_buffer = new double[row][teager_index.length*col]; 
			double[] teagerFeature = new double[teager_index.length];
			double[] featureset = new double[teager_index.length + nmfcc-1 + 5]; 
			//features for voice detection
			double zcr_m,zcr_v,rms_m,rms_s,rms_threshold;
			ArrayList<Double> pitch = new ArrayList<Double>();
			ArrayList<double[]> featureList = new ArrayList<double[]>();
			int lefr;
			double rate = -1;
			int voicedFrameNum = 0;
			//features = new AudioFeatureExtraction(col, row, 20, 8000);
			//test only
			short[] test = new short[framePeriod];
			try{
				BufferedReader f = new BufferedReader(new FileReader(new File("/sdcard/test")));
				String[] d = f.readLine().split(",");
				for(int i = 0; i < framePeriod; i ++){
					test[i] = Short.parseShort(d[i]);
					//if(i<5) Log.d(TAG, String.format("data:%d",test[i]));
				}
				f.close();	
			}catch(IOException e){
				Log.d(TAG, "error" + e.getMessage());
			}



			while (true) {
				try {
					double time=0,time1=0,time2=0,time3=0,time4=0;
					audiodata = mAudioQueue.take();
					time = System.currentTimeMillis();
					Log.d(TAG, String.format("Percentage:%d, Zeros:%d, Ones:%d",(int)percent, zeros, ones));
					/* data to process is in data */
					data = audiodata.mData;
					/* data length is in dataSize */
					int dataSize = audiodata.mSize;
					if(dataSize < framePeriod) continue;
					//System.arraycopy(audiodata.mData, 0, data, 0, framePeriod);
					voicedFrameNum = 0;
					pitch.clear();
					featureList.clear();

					//setActivityText(String.format("dataSize %d shorts %d", dataSize, data.length));

					//sampling error

					//detecting sound
					if(features.rms(data) < 250) {
						setActivityText("Silence");
						time1 = System.currentTimeMillis();
						Log.d(TAG, "silence with rms:" + features.rms(data) + "time " + (time1-time)/1000);
						continue;
					}

					//detecting voice
					for(int i = 0; i < row ; i++){
						System.arraycopy(data, i*col, data_buffer[i], 0, col);
						rms[i] = features.rms(data_buffer[i]);
						zcr[i] = features.zcr(data_buffer[i]);
					}

					zcr_m = features.mean(zcr);
					rms_m = features.mean(rms);
					zcr_v = features.var(zcr,zcr_m);
					rms_s = Math.sqrt(features.var(rms, rms_m))/rms_m;
					rms_threshold = rms_m * 0.5;
					lefr = 0;

					for(double i:rms){
						if(i < rms_threshold) lefr++;						
					}

					if(AudioInference.tree(zcr_v,zcr_m,rms_s,lefr)==0){
						//setActivityText("noise");
						time2 = System.currentTimeMillis();
						Log.d(TAG, "noise" + "time " + (time2-time1)/1000);
						//continue;
					}

					//feature extraction;

					//filter data

					//test only
					//for(int i = 0; i < framePeriod; i++) data[i] = test[i];
					//Log.d(TAG, data + " " + Arrays.toString(data));
					////////////

					fdata[0] = data[0];
					for(int i = 1; i < framePeriod; i++){
						fdata[i] = data[i] - 0.97 * data[i-1];
					}


					for(int i = 0; i < row ; i++){
						System.arraycopy(fdata, i*col, fdata_buffer[i], 0, col);
						zcr[i] = features.zcr(fdata_buffer[i]);
						if(zcr[i] > 120) continue;
						//int voiced = features.getFrameFeatures(fdata_buffer[i], audioFrameFeature);

						//int voiced = features.getFrameFeatures(fdata_buffer[i], audioFrameFeature);
						int voiced = features.getFrameFeat(fdata_buffer[i], audioFrameFeature);

						if(voiced == 1)
						{
							pitch.add(audioFrameFeature[21]);
							voicedFrameNum++;
							if(voicedFrameNum == 1) 
							{
								for(int j = 0; j < framePeriod; j++){
									rdata[j] = data[j];
								}
								time2 = System.currentTimeMillis();
								features.conv(data,framePeriod,teager_index,tdata);
								//for(int j = 0; j < 1; j++){
								//	features.getConv(data, teager_index[j], tdata[j]);
								//}
								//Log.d(TAG,  voicedFrameNum + " " + (System.currentTimeMillis() - time2)/1000);
								features.teo(tdata,framePeriod,tdata_buffer);
								rate = features.getEnrate(rdata, framePeriod, 8000);
								//Log.d(TAG, String.format("speaking rate:%f",rate));
								//DEBUG
								//for(int k = 0; k <= 7; k++){
								//if(voicedFrameNum == 40) Log.d(TAG, voicedFrameNum + " " + tdata_buffer[k][39][253] + " " + tdata_buffer[k][39][254]+ " " + tdata_buffer[k][39][255] );
								//}
								//DEBUG

							}
							//if(voicedFrameNum == 1) Log.d(TAG, voicedFrameNum + " tdata_buffer" + Arrays.toString(tdata_buffer[0]));
							//features.getTeager(tdata_buffer[i],teager_index.length,col,teagerFeature);
							features.getTeo(tdata_buffer[i],teager_index.length,col,teagerFeature);
							System.arraycopy(teagerFeature, 0, featureset, 0, teager_index.length);
							System.arraycopy( audioFrameFeature, 0, featureset, teager_index.length, nmfcc+2);
							featureList.add(featureset.clone());
							//if(voicedFrameNum == 1) Log.d(TAG, voicedFrameNum + " teagerFeature " + Arrays.toString(teagerFeature));
							//if(voicedFrameNum == 40) Log.d(TAG, voicedFrameNum + " other feature" + Arrays.toString(audioFrameFeature));

						}
						//}
					}
					double[] pitchFeature = new double[2];
					features.var(pitch, pitchFeature);
					time3 = System.currentTimeMillis();
					Log.d(TAG,  "feature time " + (time3 - time2)/1000);		

					int temp=0;
					int c = 0;
					int s = 0;
					for(double[] f:featureList){
						Date date = new Date();
						Calendar cal = Calendar.getInstance();
						cal.setTime(date);
						int hour = cal.get(Calendar.HOUR);
						int month = cal.get(Calendar.MONTH);
						int day=cal.get(Calendar.DAY_OF_WEEK);
						int year=cal.get(Calendar.YEAR);
						double _date = (double)Integer.parseInt(""+day+month+year);
						
						f[teager_index.length+nmfcc+1] = pitchFeature[0]; 
						f[teager_index.length+nmfcc+2] = pitchFeature[1];
						f[teager_index.length+nmfcc+3] = rate;
						
						if (AudioInference.stressInference(f)==1) {
							ones=ones+1;
							percent=(double)ones/((double)ones+(double)zeros)*100;
						}else{
							zeros=zeros+1;
							percent=(double)ones/((double)ones+(double)zeros)*100;
						}
						int level=0;
						//int level=stressCal.getCalView((percent/100));
						//stressCal.changeCalView(level);
						
						if (percent<=0.10) {
							level= 0;
						} else if (percent>0.10 && percent <= .20) {
							level= 1;
						} else if (percent>0.20 && percent <= .30) {
							level= 2;		
						} else if (percent>0.30 && percent <= .40) {
							level= 3;							
						} else if (percent>0.40 && percent <= .50) {
							level= 4;						
						} else if (percent>0.50 && percent <= .60) {
							level= 5;							
						} else if (percent>0.60 && percent <= .70) {
							level= 6;							
						} else if (percent>0.70 && percent <= .80) {
							level= 7;							     
						} else if (percent>0.80 && percent <= .90) {
							level= 8;
						} else if (percent>0.90 && percent <= 1.0) {
							level= 9;
						}						
						switch(level) {							
						}
						
						s += AudioInference.stressInference(f);
						//Log.d(TAG,this + "voiced features " + c + " " + Arrays.toString(f));
						c++;
					}
					Log.d(TAG,this + "voiced features " + c + " " + s);// + " "+ Arrays.toString(featureList.get(0)));
					time4 = System.currentTimeMillis();
					//Log.d(TAG,this + "pitch features " + c + " " + Arrays.toString(pitch.toArray()));				
					Log.d(TAG, "Inf time " + (time4 - time3)/1000);
					Log.d(TAG, "total time " + (time4 - time)/1000);

					//test only
					//Arrays.sort(zcr);

					/* process here data */
					/* =========== send text to the main activity =========== */
					//mCount++;
					//if (mCount % 2 == 0) {
					if(s > c/2) setActivityText(String.format("Stressed"));
					else  setActivityText(String.format("Not Stressed"));
					//} else{
					//double time = System.currentTimeMillis();

					//features.getFrameFeatures(fdata_buffer[0], audioFrameFeature);

					//Log.d(TAG, "" + (System.currentTimeMillis() - time)/1000);
					//Log.d(TAG, "frame comes");
					//}
					/* ========================= end ======================== */

				} catch (InterruptedException e) {
					break;
				} finally {
					mAudioRawDataPool.returnObject(data);
					mAudioDataPool.returnObject(audiodata);
				}
			}
		}

	}

	private void setActivityText(final String text) {
		DecimalFormat df = new DecimalFormat("#.00");
		Handler handler = SensorlabRecorderActivity.getHandler();
		if (null != handler) {
			Message m = new Message();
			Bundle data = new Bundle();
			data.putString(AudioRecorderService.AUDIORECORDER_NEWTEXT_CONTENT, (text+": "+df.format(percent)+"%"));
			m.setData(data);
			handler.sendMessage(m);
		}
	}

	public static HashMap<Double, Double> getStressMap() {
		return stressMap;
	}

	public static void setStressMap(HashMap<Double, Double> stressMap) {
		RehearsalAudioRecorder.stressMap = stressMap;
	}

	public class AudioDataFactory extends BasePoolableObjectFactory {
		@Override
		public Object makeObject() {
			return new AudioData();
		}
	}

	public class AudioDataPool extends StackObjectPool {
		public AudioDataPool() {
			super(new AudioDataFactory());
		}

		@Override
		public AudioData borrowObject() {
			try {
				return (AudioData) super.borrowObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void returnObject(Object obj) {
			try {
				super.returnObject(obj);
			} catch (Exception e) {
			}
		}

		@Override
		public void close() {
			try {
				super.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public class AudioRawDataFactory extends BasePoolableObjectFactory {

		public int mFactoryBufferSize;

		public AudioRawDataFactory(int size) {
			mFactoryBufferSize = size;
		}

		@Override
		public Object makeObject() {
			return new short[mFactoryBufferSize];
		}
	}

	public class AudioRawDataPool extends StackObjectPool {
		public AudioRawDataPool(int size) {
			super(new AudioRawDataFactory(size));
		}

		@Override
		public short[] borrowObject() {
			try {
				return (short[]) super.borrowObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void returnObject(Object obj) {
			try {
				super.returnObject(obj);
			} catch (Exception e) {
			}
		}

		@Override
		public void close() {
			try {
				super.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
