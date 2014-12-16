/*
 *
 * Copyright (c) 1999 Sun Microsystems, Inc. All Rights Reserved.

 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free,
 * license to use, modify and redistribute this software in 
 * source and binary code form, provided that i) this copyright
 * notice and license appear on all copies of the software; and 
 * ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty
 * of any kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS
 * AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE 
 * HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR 
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT
 * WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, 
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
 * OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY
 * TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.

 This software is not designed or intended for use in on-line
 control of aircraft, air traffic, aircraft navigation or
 aircraft communications; or in the design, construction,
 operation or maintenance of any nuclear facility. Licensee 
 represents and warrants that it will not use or redistribute 
 the Software for such purposes.
 */

/*  The above copyright statement is included because this 
 * program uses several methods from the JavaSoundDemo
 * distributed by SUN. In some cases, the sound processing methods
 * unmodified or only slightly modified.
 * All other methods copyright Steve Potts, 2002
 */

import java.awt.BorderLayout;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;

/**
 * SimpleSoundCapture Example. This is a simple program to record sounds and
 * play them back. It uses some methods from the CapturePlayback program in the
 * JavaSoundDemo. For licensizing reasons the disclaimer above is included.
 * 
 * @author Steve Potts
 */
public class MainClass extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5757823624573194943L;
	String fileDestination = "C:/test.wav";
	final int bufSize = 16384;
	Capture capture = new Capture();
	Playback playback = new Playback();
	AudioInputStream audioInputStream;
	JButton playB, captB, saveB;
	JTextField textField;
	String errStr;
	double duration, seconds;
	boolean isPlay = false;
	File file;
	
	static int channelIndex = 0; // 0 is a piano, 9 is percussion, other channels are for other instruments
	
	public MainClass() {
		setLayout(new BorderLayout());
		EmptyBorder eb = new EmptyBorder(5, 5, 5, 5);
		SoftBevelBorder sbb = new SoftBevelBorder(SoftBevelBorder.LOWERED);
		setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));

		JPanel p2 = new JPanel();
		p2.setBorder(sbb);
		p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBorder(new EmptyBorder(10, 0, 5, 0));
		playB = addButton("Play", buttonsPanel, false);
		captB = addButton("Record", buttonsPanel, true);
		saveB = addButton("Save", buttonsPanel, true);
		saveB.setEnabled(false);
		p2.add(buttonsPanel);

		p1.add(p2);
		add(p1);
	}

	public void open() {
	}

	public void close() {
		if (playback.thread != null) {
			playB.doClick(0);
		}
		if (capture.thread != null) {
			captB.doClick(0);
		}
	}

	private JButton addButton(String name, JPanel p, boolean state) {
		JButton b = new JButton(name);
		b.addActionListener(this);
		b.setEnabled(state);
		p.add(b);
		return b;
	}

	public void actionPerformed(ActionEvent e) {
		Object obj = e.getSource();
		if (obj.equals(playB)) {
			if (playB.getText().startsWith("Play")) {
				isPlay = true;
				playback.start();
				captB.setEnabled(false);
				playB.setText("Stop");
			} else {
				isPlay = true;
				playback.stop();
				captB.setEnabled(true);
				playB.setText("Play");
			}
		} else if (obj.equals(saveB)) {
			isPlay = false;
			playback.start();
			captB.setEnabled(false);
		} else if (obj.equals(captB)) {
			if (captB.getText().startsWith("Record")) {
				capture.start();
				playB.setEnabled(false);
				saveB.setEnabled(false);
				captB.setText("Stop");
			} else {
				capture.stop();
				playB.setEnabled(true);
				saveB.setEnabled(true);
				captB.setText("Record");
			}

		}
	}

	/**
	 * Write data to the OutputChannel.
	 */
	public class Playback implements Runnable {

		SourceDataLine line;

		Thread thread;

		public void start() {
			errStr = null;
			thread = new Thread(this);
			thread.setName("Playback");
			thread.start();
		}

		public void stop() {
			thread = null;
		}

		private void shutDown(String message) {
			if ((errStr = message) != null) {
				System.err.println(errStr);
			}
			if (thread != null) {
				thread = null;
				captB.setEnabled(true);
				playB.setText("Play");
			}
		}

		public void run() {

			// make sure we have something to play
			if (audioInputStream == null) {
				shutDown("No loaded audio to play back");
				return;
			}
			// reset to the beginnning of the stream
			try {
				audioInputStream.reset();
			} catch (Exception e) {
				shutDown("Unable to reset the stream\n" + e);
				return;
			}

			// get an AudioInputStream of the desired format for playback

			AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
			float rate = 44100.0f;
			int channels = 1;
			int frameSize = 4;
			int sampleSize = 16;
			boolean bigEndian = true;

			AudioFormat format = new AudioFormat(encoding, rate, sampleSize,
					channels, (sampleSize / 8) * channels, rate, bigEndian);

			AudioInputStream playbackInputStream = AudioSystem
					.getAudioInputStream(format, audioInputStream);

			if (playbackInputStream == null) {
				shutDown("Unable to convert stream of format "
						+ audioInputStream + " to format " + format);
				return;
			}

			// define the required attributes for our line,
			// and make sure a compatible line is supported.

			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			if (!AudioSystem.isLineSupported(info)) {
				shutDown("Line matching " + info + " not supported.");
				return;
			}

			// get and open the source data line for playback.

			try {
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(format, bufSize);
			} catch (LineUnavailableException ex) {
				shutDown("Unable to open the line: " + ex);
				return;
			}

			if (isPlay) {
				// play back the captured audio data
				int frameSizeInBytes = format.getFrameSize();
				int bufferLengthInFrames = line.getBufferSize() / 8;
				int bufferLengthInBytes = bufferLengthInFrames
						* frameSizeInBytes;
				byte[] data = new byte[bufferLengthInBytes];
				int numBytesRead = 0;

				// start the source data line
				line.start();
				while (thread != null) {
					try {
						if ((numBytesRead = playbackInputStream.read(data)) == -1) {
							break;
						}
						int numBytesRemaining = numBytesRead;
						while (numBytesRemaining > 0) {
							numBytesRemaining -= line.write(data, 0,
									numBytesRemaining);
						}
					} catch (Exception e) {
						shutDown("Error during playback: " + e);
						break;
					}
				}
			} else {
				// save the wave file to the fileDestination , also analyse it using analysis method
				int read;
				byte[] buff = new byte[1024];

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				File f2 = new File(fileDestination);
				try {
					audioInputStream.reset();
					System.out.println(audioInputStream.getFrameLength());
					// the line below is for creating a wav file from recording
					// sound
					AudioSystem.write(audioInputStream,
							AudioFileFormat.Type.WAVE, f2);
					audioInputStream.reset();
					
					while ((read = audioInputStream.read(buff)) > 0) {
						out.write(buff, 0, read);
					}
					out.flush();
					audioInputStream.reset();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				byte[] audioBytes = out.toByteArray();
				float[] audioFloats = MatlabMethod.convertToFloats(audioBytes);			
				// analysis audioFloat here
				try {
					MainClass.analysis(fileDestination);
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			/*
			 * we reached the end of the stream. let the data play out, then
			 * stop and close the line.
			 */
			if (thread != null) {
				line.drain();
			}
			line.stop();
			line.close();
			line = null;
			shutDown(null);
		}
	} // End class Playback

	/**
	 * Reads data from the input channel and writes to the output stream
	 */
	class Capture implements Runnable {

		TargetDataLine line;

		Thread thread;

		public void start() {
			errStr = null;
			thread = new Thread(this);
			thread.setName("Capture");
			thread.start();
		}

		public void stop() {
			thread = null;
		}

		private void shutDown(String message) {
			if ((errStr = message) != null && thread != null) {
				thread = null;
				playB.setEnabled(true);
				captB.setText("Record");
				System.err.println(errStr);
			}
		}

		public void run() {

			duration = 0;
			audioInputStream = null;

			// define the required attributes for our line,
			// and make sure a compatible line is supported.

			AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
			float rate = 44100.0f;
			int channels = 1;
			int frameSize = 4;
			int sampleSize = 16;
			boolean bigEndian = true;

			AudioFormat format = new AudioFormat(encoding, rate, sampleSize,
					channels, (sampleSize / 8) * channels, rate, bigEndian);

			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				shutDown("Line matching " + info + " not supported.");
				return;
			}

			// get and open the target data line for capture.

			try {
				line = (TargetDataLine) AudioSystem.getLine(info);
				line.open(format, line.getBufferSize());
			} catch (LineUnavailableException ex) {
				shutDown("Unable to open the line: " + ex);
				return;
			} catch (SecurityException ex) {
				shutDown(ex.toString());
				// JavaSound.showInfoDialog();
				return;
			} catch (Exception ex) {
				shutDown(ex.toString());
				return;
			}

			// play back the captured audio data
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int frameSizeInBytes = format.getFrameSize();
			int bufferLengthInFrames = line.getBufferSize() / 8;
			int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
			byte[] data = new byte[bufferLengthInBytes];
			int numBytesRead;

			line.start();

			while (thread != null) {
				if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
					break;
				}
				out.write(data, 0, numBytesRead);
			}

			// we reached the end of the stream.
			// stop and close the line.
			line.stop();
			line.close();
			line = null;

			// stop and close the output stream
			try {
				out.flush();
				out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			// load bytes into the audio input stream for playback

			byte audioBytes[] = out.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
			audioInputStream = new AudioInputStream(bais, format,
					audioBytes.length / frameSizeInBytes);

			long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / format
					.getFrameRate());
			duration = milliseconds / 1000.0;

			try {
				audioInputStream.reset();
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}

		}
	} // End class Capture

	
	//Analyse the latest observed wav file here
	public static void analysis(float[] audioFloats) {
		System.out.println("\nAnalysing..");
	}
	public static void analysis(String filedestination) throws UnsupportedAudioFileException, IOException {
		System.out.println("\nAnalysing..");
		Pitch.count = 0;
		Pitch.pitchEst(filedestination);
	}

	public static void main(String s[]) throws InterruptedException {
	
		MainClass ssc = new MainClass();
		ssc.open();
		JFrame f = new JFrame("Capture/Playback");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add("Center", ssc);
		f.pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = 360;
		int h = 170;
		f.setLocation(screenSize.width / 2 - w / 2, screenSize.height / 2 - h
				/ 2);
		f.setSize(w, h);
		f.setVisible(true);
		System.out.println("System is Running");
		int[] note = {60,62,64,67,67};
		int[] duration = {200,200,200,200,200};
		int volume = 80;
	//	playMusicArray(note, duration, volume);
	}
	public static void playMusicArray(ArrayList<Integer> note,ArrayList<Integer> duration,int volume) throws InterruptedException{

		 Synthesizer synth = null;
		 MidiChannel channel;
		 Instrument[] instr;

		
		try {
			synth = MidiSystem.getSynthesizer();
			synth.open();
		} catch (MidiUnavailableException e) {
			System.out.println("Warning! Midi file is not found.");
		}     

		channel = synth.getChannels()[channelIndex]; //Percussion is always played on Midi channel 9
		instr = synth.getDefaultSoundbank().getInstruments();
		synth.loadInstrument(instr[0]);
		channel.programChange(channelIndex);
		for(int i = 0 ; i < note.size(); i++ ){
			int interestNote = note.get(i);
			if(interestNote>0)channel.noteOn(interestNote,volume);
			else channel.allNotesOff();
			if(duration.get(i)>0)Thread.sleep(duration.get(i));
		}
			channel.allNotesOff();

			Thread.sleep( 1500 );

		 channel.allNotesOff();
	}
}