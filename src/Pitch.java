import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class Pitch {
	static int count = 0;
	static String pitchAnswer = "";
	static String pitchBefore = "";
	static int lastDuration = 0;
	static int volume = 80;
	static int lastNote = -1;
	static float lastFreq = -1;
	static String pitchPropString="";
	static ArrayList<Integer> playNote = new ArrayList<Integer>();
	static ArrayList<Integer> playDuration = new ArrayList<Integer>();
	static ArrayList<Float> frequencyArray = new ArrayList<Float>();
	static String[] note = { "C0", "C#0", "D0", "D#0", "E0", "F0", "F#0", "G0",
			"G#0", "A0", "A#0", "B0", "C1", "C#1", "D1", "D#1", "E1", "F1",
			"F#1", "G1", "G#1", "A1", "A#1", "B1", "C2", "C#2", "D2", "D#2",
			"E2", "F2", "F#2", "G2", "G#2", "A2", "A#2", "B2", "C3", "C#3",
			"D3", "D#3", "E3", "F3", "F#3", "G3", "G#3", "A3", "A#3", "B3",
			"C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4",
			"A#4", "B4", "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5", "G5",
			"G#5", "A5", "A#5", "B5", "C6", "C#6", "D6", "D#6", "E6", "F6",
			"F#6", "G6", "G#6", "A6", "A#6", "B6", "C7", "C#7", "D7", "D#7",
			"E7", "F7", "F#7", "G7", "G#7", "A7", "A#7", "B7", "C8", "C#8",
			"D8", "D#8", "E8", "F8", "F#8", "G8", "G#8", "A8", "A#8", "B8" };
	static double[] freq = { 16.35, 17.32, 18.35, 19.45, 20.6, 21.83, 23.12,
			24.5, 25.96, 27.5, 29.14, 30.87, 32.7, 34.65, 36.71, 38.89, 41.2,
			43.65, 46.25, 49, 51.91, 55, 58.27, 61.74, 65.41, 69.3, 73.42,
			77.78, 82.41, 87.31, 92.5, 98, 103.83, 110, 116.54, 123.47, 130.81,
			138.59, 146.83, 155.56, 164.81, 174.61, 185, 196, 207.65, 220,
			233.08, 246.94, 261.63, 277.18, 293.66, 311.13, 329.63, 349.23,
			369.99, 392, 415.3, 440, 466.16, 493.88, 523.25, 554.37, 587.33,
			622.25, 659.25, 698.46, 739.99, 783.99, 830.61, 880, 932.33,
			987.77, 1046.5, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91,
			1479.98, 1567.98, 1661.22, 1760, 1864.66, 1975.53, 2093, 2217.46,
			2349.32, 2489.02, 2637.02, 2793.83, 2959.96, 3135.96, 3322.44,
			3520, 3729.31, 3951.07, 4186.01, 4434.92, 4698.63, 4978.03,
			5274.04, 5587.65, 5919.91, 6271.93, 6644.88, 7040, 7458.62, 7902.13 };
	static int bufferSize = 1024;

	static int[][] key = new int[12][7];
	
	public static void initKey(){
		key[0][0]=0;key[0][1]=2;key[0][2]=4;key[0][3]=5;key[0][4]=7;key[0][5]=9 ; key[0][6]=11;
		for(int i = 1 ; i < key.length;i++){
			for(int j = 0 ; j < key[0].length;j++){
				key[i][j] = (key[i-1][j]+1)%12;
			}
		}
		for(int i = 1 ; i<key.length;i++){
		    Arrays.sort(key[i]);
		}
	}
	
	public static Thread pitchEst(String directory)
			throws UnsupportedAudioFileException, IOException {
		initKey();
		AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(new File(
				directory), bufferSize, 0);
		playNote.clear();
		playDuration.clear();
		playNote.add(-1);
		frequencyArray.add((float) -1.0);
		Thread thread = dispatchPitchEst(dispatcher);
		return thread;
	}

	public static Thread pitchEst(float[] audioFloats)
			throws UnsupportedAudioFileException, IOException {
		initKey();
		AudioDispatcher dispatcher = AudioDispatcherFactory.fromFloatArray(
				audioFloats, 11025, 1024, 0);
		playNote.clear();
		playDuration.clear();
		playNote.add(-1);
		frequencyArray.add((float) -1.0);
		Thread thread = dispatchPitchEst(dispatcher);
		return thread;
	}

	public static Thread dispatchPitchEst(AudioDispatcher dispatcher) {
		final int BufferSize = bufferSize;
		final int sampFreq = 11025;

		dispatcher.addAudioProcessor(new PitchProcessor(
				PitchEstimationAlgorithm.FFT_YIN, sampFreq, BufferSize,
				new PitchDetectionHandler() {
					public void handlePitch(
							PitchDetectionResult pitchDetectionResult,
							AudioEvent audioEvent) {
						final float pitchInHz = pitchDetectionResult.getPitch();
						final float pitchProp = pitchDetectionResult
								.getProbability();
						String time = "";
						count++;
						DecimalFormat df = new DecimalFormat("#.00");
						time = " "
								+ df.format(((double) (count * BufferSize + 1))
										/ sampFreq);
						int noteAns;
						pitchPropString = pitchPropString +pitchProp+"\n";
						if (pitchProp > 0.95){
							noteAns = findNote(pitchInHz);
							
						}
						else{
							noteAns = lastNote;
						}
						
						System.out.println(pitchInHz + " " + noteAns);
						String noteP;
						if (noteAns > 0)
							noteP = note[noteAns];
						else
							noteP = "detect nothing";

						if (pitchInHz == -1) {
							// System.out.println(pitchInHz);
							String ans = time + " detect nothing";
							if (!pitchBefore.equals("detect nothing")) {

								int duration = (int) (Double.parseDouble(time) * 1000)
										- lastDuration;
								lastDuration = (int) (Double.parseDouble(time) * 1000);

								if (noteAns > 0)
									playNote.add(noteAns + 12);
								else
									playNote.add(-1);
								frequencyArray.add(pitchInHz);
								lastNote = -1;
								lastFreq=pitchInHz;
								playDuration.add(duration);
								pitchAnswer = pitchAnswer + "\n" + ans;
								pitchBefore = "detect nothing";
							}
						} else {
							String ans = time + " " + pitchInHz + " " + noteP/*
																			 * +
																			 * " "
																			 * +
																			 * percent
																			 * +
																			 * "%"
																			 */;

							 System.out.println(ans);
							if (!pitchBefore.equals(noteP)) {
								pitchAnswer = pitchAnswer + "\n" + ans;
								pitchBefore = noteP;

								int duration = (int) (Double.parseDouble(time) * 1000)
										- lastDuration;
								lastDuration = (int) (Double.parseDouble(time) * 1000);

								lastNote = noteAns;
								lastFreq=pitchInHz;
								frequencyArray.add(pitchInHz);
								if (noteAns > 0)
									playNote.add(noteAns + 12);
								else
									playNote.add(-1);
								playDuration.add(duration);
							}
						}
					}
				}));

		Thread thread = new Thread(dispatcher, "Audio Dispatcher");
		thread.start();
		while (thread.isAlive())
			;
		System.out.println("Pitch prop = "+pitchPropString);
		playDuration.add(1000);

		ArrayList<Integer> reallyPlayNote = new ArrayList<Integer>();
		ArrayList<Integer> reallyPlayduration = new ArrayList<Integer>();
		int currentSize = 0;
		if(playDuration.get(0)<0)playDuration.set(0,0);
		
		for (int i = 0; i < playNote.size(); i++) {
			System.out.println("note is " + playNote.get(i)
					+ " and the duration is " + playDuration.get(i)
					+ " and the frequency is " + frequencyArray.get(i));
		}
		
		
		for (int i = 0; i < playNote.size() - 1; i++) {
			if (i > 0)
				System.out.println("i = " + i + " " + playNote.get(i) + " vs "
						+ reallyPlayNote.get(currentSize));

			if (i == 0) {
				reallyPlayNote.add(playNote.get(0));
				reallyPlayduration.add(playDuration.get(0));
				System.out.println("add " + playNote.get(0) + " to index "
						+ currentSize);
			} else if ((Math.abs(playNote.get(i)
					- reallyPlayNote.get(currentSize)) < 2)) {
				int j = i;
				int currentDuration = reallyPlayduration.get(currentSize);
				int otherDuration = 0;
				for (; j < playNote.size(); j++) {
					if (playNote.get(j) == reallyPlayNote.get(currentSize)) {
						System.out.println("Current duration = "
								+ currentDuration + " Other duration = "
								+ otherDuration);
						currentDuration += playDuration.get(j);
						if (currentDuration > otherDuration)
							reallyPlayduration.set(currentSize,
									reallyPlayduration.get(currentSize)
											+ playDuration.get(i));
						else {
							if (i > 0)
								System.out.println("i = " + i + " "
										+ playNote.get(i) + " differ "
										+ playNote.get(i - 1));
							reallyPlayNote.add(playNote.get(i));
							reallyPlayduration.add(playDuration.get(i));
							currentSize++;
						}
						break;
					} else
						otherDuration += playDuration.get(j);
					if (j == playNote.size() - 1) {
						if (i > 0)
							System.out.println("i = " + i + " "
									+ playNote.get(i) + " differ "
									+ playNote.get(i - 1));
						reallyPlayNote.add(playNote.get(i));
						reallyPlayduration.add(playDuration.get(i));
						currentSize++;
					}
				}

				// playNote.set(i+1,playNote.get(i));
				// reallyPlayduration.set(currentSize,reallyPlayduration.get(currentSize)+playDuration.get(i));
			} else {

				reallyPlayNote.add(playNote.get(i));
				reallyPlayduration.add(playDuration.get(i));

				currentSize++;
				System.out.println("add " + playNote.get(i) + " to index "
						+ currentSize);
			}
		}
		System.out.println("---------------end---------------");
		for (int i = 0; i < reallyPlayNote.size(); i++) {
			// JOptionPane.showMessageDialog(null, "fu");
			System.out.println("true note is " + reallyPlayNote.get(i)
					+ " and the true duration is " + reallyPlayduration.get(i));
		}
		System.out.println("---------------true end---------------");

		for (int i = 0; i < reallyPlayNote.size(); i++) {
			int dur = reallyPlayduration.get(i);
			if (dur < 187) {
				//first entry
				if (i == 0 && reallyPlayNote.size() > 1) {
					if (reallyPlayduration.get(i + 1) > dur) {
						reallyPlayduration.set(i + 1,
								reallyPlayduration.get(i + 1) + dur);
						reallyPlayduration.set(i, 0);
					} else {
						int j = i + 2;
						while (j < reallyPlayNote.size()) {
							if (reallyPlayduration.get(j) > dur) {
								reallyPlayduration.set(j,
										reallyPlayduration.get(j) + dur);
								reallyPlayduration.set(i, 0);
								break;
							}
							j++;
						}
					}
				} 
				//last entry
				else if (i == reallyPlayNote.size() - 1) {
					if (reallyPlayduration.get(i - 1) > dur) {
						reallyPlayduration.set(i - 1,
								reallyPlayduration.get(i - 1) + dur);
						reallyPlayduration.set(i, 0);
					} else {
						int j = i - 2;
						while (j >= 0) {
							if (reallyPlayduration.get(j) > dur) {
								reallyPlayduration.set(j,
										reallyPlayduration.get(j) + dur);
								reallyPlayduration.set(i, 0);
								break;
							}
							j--;
						}
					}
				} 
				//in the middle of array
				else {
					int c1 = reallyPlayNote.get(i-1);
					int c2 = reallyPlayNote.get(i+1);
					int note = reallyPlayNote.get(i);
					if(Math.abs(note-c1)<Math.abs(note-c2)){
						if(dur<reallyPlayduration.get(i+1) && dur>reallyPlayduration.get(i-1) ){
							reallyPlayduration.set(i+1,dur+reallyPlayduration.get(i+1));
							reallyPlayduration.set(i,0);
						}
						else{
							reallyPlayduration.set(i-1,dur+reallyPlayduration.get(i-1));
							reallyPlayduration.set(i,0);
						}
					}
					else{
						if(dur<reallyPlayduration.get(i-1) && dur>reallyPlayduration.get(i+1) ){
							reallyPlayduration.set(i-1,dur+reallyPlayduration.get(i-1));
							reallyPlayduration.set(i,0);
						}
						else{
							reallyPlayduration.set(i+1,dur+reallyPlayduration.get(i+1));
							reallyPlayduration.set(i,0);
						}
					}

				}
			}
		}
		for(int i = reallyPlayNote.size()-1;i>=0;i--){
			if(reallyPlayduration.get(i)==0){
				reallyPlayNote.remove(i);
				reallyPlayduration.remove(i);
			}
		}
		for (int i = 0; i < reallyPlayNote.size(); i++) {
			// JOptionPane.showMessageDialog(null, "fu");
			System.out.println("really true  note is " + reallyPlayNote.get(i)
					+ " and the really true true duration is " + reallyPlayduration.get(i));
		}
		System.out.println("---------------really true end---------------");
		int musicKey =calculateKey(reallyPlayNote,reallyPlayduration);
		tuneMelody(reallyPlayNote,reallyPlayduration,musicKey);
	


		try {
			MainClass
					.playMusicArray(reallyPlayNote, reallyPlayduration, volume);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// JOptionPane.showMessageDialog(null, pitchAnswer);

		pitchAnswer = "";
		return thread;
	}
	public static void tuneMelody(ArrayList<Integer> noteList, ArrayList<Integer> durationList,int musicKey){
	    System.out.println(Arrays.deepToString(key));
	    System.out.println("Music Key is "+musicKey);
	 //   musicKey = 0;
		for(int i = 0;  i< noteList.size();i++){
			int multiplier = noteList.get(i)/12;
			int note = noteList.get(i)%12;
			int nearestLeft =-1;
			int nearestRight = 12;

			if (note==0){
					nearestLeft = (key[musicKey][6]==10) ? -2:-1;
			}
			else if(note==11){
				nearestRight = (key[musicKey][0]==1) ? 13:12;
			}
			
			for(int j = 0 ; j < key[0].length;j++){
				if(key[musicKey][j]<note){
					if(nearestLeft<key[musicKey][j])nearestLeft=key[musicKey][j];
				}
				else if(key[musicKey][j]>note){
					if(nearestRight>key[musicKey][j])nearestRight=key[musicKey][j];
				}
				else{
					nearestLeft=-100;
					break;
				}
			}
			if(nearestLeft!= -100){
				if(Math.abs(note-nearestLeft)>Math.abs(note-nearestRight)){
					noteList.set(i,12*multiplier+nearestRight);
				}
				else if(Math.abs(note-nearestLeft)<Math.abs(note-nearestRight)){
					noteList.set(i,12*multiplier+nearestLeft);
				}
				else{
					double a = Math.random();
					if(a>0.5) noteList.set(i,12*multiplier+nearestRight);
					else noteList.set(i,12*multiplier+nearestLeft);
				}
				
			}
		}
	}
	public static int calculateKey(ArrayList<Integer> noteList,ArrayList<Integer> durationList){
		int[] bucket = {0,0,0,0,0,0,0,0,0,0,0,0};
		for(int i = 0; i < noteList.size();i++){
			int note = noteList.get(i)%12;
			for(int j = 0 ; j < key.length ; j++){
				for(int k = 0 ; k < key[0].length; k++){
					if(note==key[j][k]) 
					{bucket[j]+=durationList.get(i);
					break;
					}
				}
			}
		}
		int maxIndex=0;
		for(int i =1;i<bucket.length;i++){
			if(bucket[maxIndex]<bucket[i])maxIndex=i;
		}
	    System.out.println(Arrays.toString(bucket));
		return maxIndex;
	}
	public static void main(String[] args) throws LineUnavailableException,
			UnsupportedAudioFileException, IOException, InterruptedException {
		// MatlabMethod.wavPlay("D:/testSound/D.wav");
		String fileDestination = "C:/test.wav";

		Thread thread = pitchEst(fileDestination);
		while (thread.isAlive())
			;
		System.out.println("Count = " + count);
		// pitchEst(MatlabMethod.wavRead(fileDestination));

	}

	public static void playNote(int note, int duration) {
	}

	public static void tuneNote(ArrayList<Integer> noteList,
			ArrayList<Integer> durationList) {

	}

	public static int findNote(double freq) {
		int noteAns = -2;
		// double percent = 0;
		double noteIndex = (Math.log((double) freq) - 2.794372868) / 0.0578;
		if (noteIndex - Math.floor(noteIndex) >= 0.5) {
			noteAns = (int) Math.floor(noteIndex) + 1;
			// percent = (noteIndex-Math.floor(noteIndex))*100;
		} else {
			noteAns = (int) Math.floor(noteIndex);
			// percent = 100-(noteIndex-Math.floor(noteIndex))*100;
		}
		return noteAns;
	}
}
