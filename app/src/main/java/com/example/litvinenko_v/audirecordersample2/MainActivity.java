package com.example.litvinenko_v.audirecordersample2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.View;
import android.os.Environment;
import android.widget.Button;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

/////

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;//CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int bufferSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonHandlers();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    //short sData[] = new short[BufferElements2Rec];

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);//BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                //writeAudioDataToFile();
                writeAudioDataToWaveFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        String filePath =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.raw";
        String filePathWave =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.wav";
        //short sData[] = new short[BufferElements2Rec];

        byte bdata[] = new byte[bufferSize];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int read = 0;
        while (isRecording) {
            // gets the voice output from microphone to byte format
            read = recorder.read(bdata, 0, bufferSize);

            //if(AudioRecord.ERROR_INVALID_OPERATION != read){
                try {
                    os.write(bdata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            //}
            /*recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }
        try {
            os.close();
            copyWaveFile(filePath, filePathWave);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    startRecording();
                    break;
                }
                case R.id.btnStop: {
                    enableButtons(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            //System.out.println("File size: " + totalDataLen);
            System.out.println("File size: " + totalDataLen);
            //AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            int inputData = in.read();
            while(inputData != -1){
                //System.out.println("Data: " + (char)inputData);
                out.write(inputData);
                inputData = in.read();
            }
            System.out.println("Data write finished!");
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
    private void writeAudioDataToWaveFile() {
        // Write the output audio in byte

        String filePathWave =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.wav";
        //short sData[] = new short[BufferElements2Rec];

        byte bdata[] = new byte[bufferSize];

        //FileInputStream in = null;
        FileOutputStream outFileStream = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        String outFilename = filePathWave;
        try {
            outFileStream = new FileOutputStream(outFilename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            WriteWaveFileHeader(outFileStream, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Start Data writing!");
        int readCount = 0;
        while (isRecording) {
            // gets the voice output from microphone to byte format
            readCount =+ recorder.read(bdata, 0, bufferSize);

            //if(AudioRecord.ERROR_INVALID_OPERATION != read){
            try {
                outFileStream.write(bdata);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        try {
            //outFileStream
            totalAudioLen = outFileStream.getChannel().size()-36;
            //totalAudioLen = readCount;
            totalDataLen = totalAudioLen + 36;
            CorrectWaveFileHeader(outFilename, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            outFileStream.close();
            System.out.println("Data write finished!");
            //outFileStream.getChannel().
            //copyWaveFile(filePath, filePathWave);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CorrectWaveFileHeader(
            String filePath, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
        WriteOffset(randomAccessFile, 4, (byte) (totalDataLen & 0xff));
        WriteOffset(randomAccessFile, 5, (byte) ((totalDataLen >> 8) & 0xff));
        WriteOffset(randomAccessFile, 6, (byte) ((totalDataLen >> 16) & 0xff));
        WriteOffset(randomAccessFile, 7, (byte) ((totalDataLen >> 24) & 0xff));
        WriteOffset(randomAccessFile, 40, (byte) (totalAudioLen & 0xff));
        WriteOffset(randomAccessFile, 41, (byte) ((totalAudioLen >> 8) & 0xff));
        WriteOffset(randomAccessFile, 42, (byte) ((totalAudioLen >> 16) & 0xff));
        WriteOffset(randomAccessFile, 43, (byte) ((totalAudioLen >> 24) & 0xff));
        randomAccessFile.close();
    }

    private void WriteOffset(RandomAccessFile randomAccessFile, int offset, byte curByte) throws IOException {

        randomAccessFile.seek(offset);
        randomAccessFile.write(curByte);

    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
}
