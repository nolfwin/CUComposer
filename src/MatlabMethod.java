import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import be.tarsos.dsp.pitch.*;

/*
 * 1. wavRead - insert the file destination and it will return an array of float of the wav file
 * 2. wavPlay - insert the file destination and it will play the wav file
 * 3. convertToFloat - insert the byte Array and it will return float array (value is between -1 to 1)
 * 4. convertToByte - insert the float Array and it will return byte array 
 */
public class MatlabMethod {

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {

		String fileDestination = "C:/test.wav";
				
		float[] audio = wavRead(fileDestination);
		
		Pitch.pitchEst(fileDestination);
		
	/*	try {
			MainClass.analysis(fileDestination);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
/*		FastYin yin = new FastYin(44100,audio.length);
//		AMDF amdf = new AMDF(44100,audio.length);
//		Yin yin = new Yin(44100,audio.length);
		PitchDetectionResult result = yin.getPitch(audio);
		float a = result.getPitch();
		System.out.println();
		System.out.println("Freq = " + a); */
		System.exit(0);
	}
	public static float[] wavRead(String directory) {
		float[] audioFloats = new float[1];
		try {
			URL url = new File(directory).toURI().toURL();
			AudioInputStream audioInputStream = AudioSystem
					.getAudioInputStream(url);
			int read;
			byte[] buff = new byte[1024];

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				// the line below is for creating a wav file from recording
				while ((read = audioInputStream.read(buff)) > 0) {
					out.write(buff, 0, read);
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			byte[] audioBytes = out.toByteArray();

			ShortBuffer sbuf = ByteBuffer.wrap(audioBytes)
					.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			short[] audioShorts = new short[sbuf.capacity()];
			sbuf.get(audioShorts);
			audioFloats = new float[audioShorts.length];
			for (int i = 0; i < audioShorts.length; i++) {
				audioFloats[i] = ((float) audioShorts[i]) / 0x8000;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return audioFloats;
	}
	
	public static void wavPlay(final String directory) {
		new Thread() {
			@Override
			public void run() {
				AudioInputStream audioInputStream = null;
				URL url;
				try {
					url = new File(directory).toURI().toURL();
					audioInputStream = AudioSystem
							.getAudioInputStream(url);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SourceDataLine sourceDataLine = null;
				try {
					AudioFormat audioFormat = audioInputStream.getFormat();
					DataLine.Info info = new DataLine.Info(
							SourceDataLine.class, audioFormat);
					sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
					sourceDataLine.open(audioFormat);
				} catch (LineUnavailableException e) {
					e.printStackTrace();
					return;
				}

				sourceDataLine.start();
				byte[] data = new byte[524288];// 128Kb
				try {
					int bytesRead = 0;
					while (bytesRead != -1) {
						bytesRead = audioInputStream.read(data, 0, data.length);
						if (bytesRead >= 0)
							sourceDataLine.write(data, 0, bytesRead);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				} finally {
					sourceDataLine.drain();
					sourceDataLine.close();
				}
			}
		}.start();
	}
	public static float[] convertToFloats(byte[] audioBytes){
		ShortBuffer sbuf =
			    ByteBuffer.wrap(audioBytes).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
			short[] audioShorts = new short[sbuf.capacity()];
			sbuf.get(audioShorts);
			float[] audioFloats = new float[audioShorts.length];
			for (int i = 0; i < audioShorts.length; i++) {
			    audioFloats[i] = ((float)audioShorts[i])/0x8000;
			}
		return audioFloats;
	}
	public static byte[] convertToBytes(float[] audioFloats){

		short[] shortsArray = new short[audioFloats.length];
		for(int i = 0 ; i < shortsArray.length;i++){
			shortsArray[i] = (short)((audioFloats[i])*0x8000);
		}

	byte[] bytesArray = new byte[shortsArray.length * 2];
		ByteBuffer.wrap(bytesArray).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(shortsArray);
		return bytesArray;
	}
}
