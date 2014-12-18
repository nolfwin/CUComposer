import java.io.*;
import java.net.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.sound.sampled.*;


/*************************************************************************
  *  Compilation:  javac StdAudio.java
  *  Execution:    java StdAudio
  *  
  *  Simple library for reading, writing, and manipulationg .wav files.
  *
  *  Limitations
  *  -----------
  *    - Does not seem to work properly when reading .wav files from a .jar file.
  *    - Assumes the audio is monaural, and 44,100 samples per second.
  * 
  * A revision of StdAudio by Robert Sedgewick and Kevin Wayne
  * of Computer Science, Princeton. 
  *
  *************************************************************************/
public class SoundManager {
    private final static int FPS = 11025;   // samples per second
    
    // This declaration makes it impossible to use a new expression new StdAudio().
    private SoundManager() { }
    public static void main(String[] args){
    	String fileDestination = "C:/test.wav";
		float[] audio = MatlabMethod.wavRead(fileDestination);
		double[] audioDouble = convertFloatsToDoubles(audio);
		play(audioDouble);
    }
    public static void floatPlay(float[] audioFloats){
		double[] audioDouble = convertFloatsToDoubles(audioFloats);
		play(audioDouble);
    }
    /** = an array of the values in file filename, 
      with values scaled to be between -1 and 1. */
    public static double[] convertFloatsToDoubles(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }
    public static double[] read(String filename) {
        byte[] data= readByte(filename);
        int N= data.length;
        double[] d= new double[N/2];
        for (int i= 0; i < N/2; i++) {
            d[i]= ((short) (((data[2*i+1] & 0xFF) << 8) + (data[2*i] & 0xFF))) / 32768.0;
        }
        return d;
    }
    
    /** Play array d.
      Precondition. input is a .wav file, 44,100 samples
      per second, 16-bit audio, mono, signed PCM, little
      Endian, with array values between -1 and 1.
      */
    public static void play(double[] d) {
        AudioFormat format= new AudioFormat(FPS, 16, 1, true, false);
        byte[] data= new byte[2 * d.length];
        for (int i= 0; i < d.length; i++) {
            int temp= (int) (d[i] * 32768.0);
            data[2*i + 0]= (byte) temp;
            data[2*i + 1]= (byte) (temp >> 8);
        }
        play(format, data);
    }
    
    /** Save array d as a .wav or .au file in file named f.
      Precondition: f ends in .wav or .au.
      Information: The file has 44,100 samples per second
      and uses 16-bit audio, mono, signed PCM, little Endian
      */
    public static void save(double[] d, String f) {
        AudioFormat format= new AudioFormat(FPS, 16, 1, true, false);
        byte[] data= new byte[2 * d.length];
        for (int i= 0; i < d.length; i++) {
            int temp= (int) (d[i] * 32768.0);
            data[2*i + 0]= (byte) temp;
            data[2*i + 1]= (byte) (temp >> 8);
        }
        
        // Save the file
        try {
            ByteArrayInputStream bais= new ByteArrayInputStream(data);
            AudioInputStream ais= new AudioInputStream(bais, format, d.length);
            if (f.endsWith(".wav") || f.endsWith(".WAV")) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(f));
                System.out.println("file name: " + f);
            }
            else if (f.endsWith(".au") || f.endsWith(".AU")) {
                AudioSystem.write(ais, AudioFileFormat.Type.AU, new File(f));
            }
            else {
                throw new RuntimeException("File format not supported: " + f);
            }
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }
    
    /** play the sound represented by array d */
    private static void play(AudioFormat format, byte[] d) {
        SourceDataLine line= null;
        DataLine.Info info= new DataLine.Info(SourceDataLine.class, format);
        try {
            line= (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
        
        line.start();
        line.write(d, 0, d.length);
        line.drain();
        line.close();
    }
    
    /** = the audio format of file f */
    private static AudioFormat format(String f) {
        AudioInputStream ais= ais(f);
        AudioFormat format= ais.getFormat();
        return format;
    }
    
    /** = audio input stream of file f */
    private static AudioInputStream ais(String f) {
        AudioInputStream ais= null;
        try {
            URL url= SoundManager.class.getResource(f);
            ais= AudioSystem.getAudioInputStream(url);
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
        return ais;
    }
    
    /** = data in file f as a byte array */
    private static byte[] readByte(String f) {
        AudioInputStream ais= ais(f);
        byte[] data= null;
        try {
            data= new byte[ais.available()];
            ais.read(data);
        }
        catch (IOException e) { 
            throw new RuntimeException("Could not read " + f);
        }
        
        return data;
    }
    
    /* = a double array (a sine wave) that plays the note at frequency
     hz for duration seconds scaled to volume amplitude
     */
    public static double[] note(double hz, double duration, double amplitude) {
        int N= (int) (FPS * duration);
        double[] tone= new double[N];
        for (int i= 0; i < N; i++)
            tone[i]= amplitude * Math.sin(2 * Math.PI * i * hz / FPS);
        return tone;
    }
    
    /** = Play a major scale */
    public static void playScale() {
        // scale increments
        int[] steps= { 0, 2, 4, 5, 7, 9, 11, 12 };
        for (int i= 0; i < steps.length; i++) {
            double hz= 440.0 * Math.pow(2, steps[i] / 12.0);
            SoundManager.play(note(hz, 1.0, 0.5));
        }
        
        // needed because of the way Java handles audio
        System.exit(0);
    }
    
    /** = Save a major scale in files note0.wav .. note7.wav. */
    public static void saveScale() {
        int[] steps= { 0, 2, 4, 5, 7, 9, 11, 12 };
        for (int i= 0; i < steps.length; i++) {
            double hz= 440.0 * Math.pow(2, steps[i] / 12.0);
            SoundManager.save(note(hz, 1.0, 0.5), "note" + i + ".wav");
        }
        System.out.println("Done saving");
    }
    
    
    /** Play audio file f */
    public static void playAudio(String f) {
        try{
            File soundFile= new File(f);
            AudioInputStream audioInputStream= AudioSystem.getAudioInputStream(soundFile);
            AudioFormat audioFormat= audioInputStream.getFormat();
            //System.out.println(audioFormat);
            
            DataLine.Info dataLineInfo=
                new DataLine.Info(SourceDataLine.class, audioFormat);
            
            SourceDataLine sourceDataLine= (SourceDataLine)AudioSystem.getLine(dataLineInfo);
            
            run(audioFormat, audioInputStream, sourceDataLine);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    /** Play the music given by its three parameters. */
    private static void run(AudioFormat audioFormat,
                            AudioInputStream audioInputStream,
                            SourceDataLine sourceDataLine) {
        byte tempBuffer[] = new byte[10000];
        
        try {
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
            
            int cnt= audioInputStream.read(tempBuffer,0,tempBuffer.length);
            // Loop until the input read method returns -1 for empty stream
            while (cnt != -1){
                if(cnt > 0){
                    // Write data to the internal buffer of the data line,
                    // where it will be delivered to the speaker.
                    sourceDataLine.write(tempBuffer, 0, cnt);
                    
                    cnt= audioInputStream.read(tempBuffer,0,tempBuffer.length);
                }
            }
            //Wait for internal buffer of the data line to empty.
            sourceDataLine.drain();
            sourceDataLine.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    
    
    
}
