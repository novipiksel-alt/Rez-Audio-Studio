// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import android.media.*;
import java.io.*;
import java.nio.*;

final class AudioEngine {
    static final class Source {
        File file;
        LosslessAudio.Info direct;
        long durationUs;
        String extension, mime, codec;
        int sampleRate, channels, track=-1, muxFormat;
    }
    static Source inspect(File file) throws IOException {
        Source s=new Source();s.file=file;
        try {
            s.direct=LosslessAudio.inspect(file);s.durationUs=s.direct.durationUs;
            s.extension=s.direct.extension();s.mime=s.direct.mime();s.codec=s.extension.toUpperCase(java.util.Locale.ROOT);
            s.sampleRate=s.direct.sampleRate;s.channels=s.direct.channels;return s;
        } catch(IOException directError) {
            // Never fall back to platform remux for damaged MP3/WAV/ADTS.
            try(RandomAccessFile f=new RandomAccessFile(file,"r")) {
                int h=f.readInt();
                if(h==0x52494646 || (h>>>8)==0x494433 || (h&0xffe00000)==0xffe00000) throw directError;
            }
        }
        MediaExtractor ex=new MediaExtractor();
        try {
            ex.setDataSource(file.getAbsolutePath());
            for(int i=0;i<ex.getTrackCount();i++) {
                MediaFormat fmt=ex.getTrackFormat(i);String mime=fmt.getString(MediaFormat.KEY_MIME);
                if(mime==null||!mime.startsWith("audio/"))continue;
                if(mime.equals("audio/mp4a-latm")) {s.extension="m4a";s.mime="audio/mp4";s.codec="AAC";s.muxFormat=MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;}
                else if(mime.equals("audio/3gpp")||mime.equals("audio/amr-wb")) {s.extension="3gp";s.mime="audio/3gpp";s.codec="AMR";s.muxFormat=MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP;}
                else if(mime.equals("audio/opus")||mime.equals("audio/vorbis")) {s.extension="webm";s.mime="audio/webm";s.codec=mime.equals("audio/opus")?"Opus":"Vorbis";s.muxFormat=MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM;}
                else throw new IOException("Ovaj kodek ("+mime+") se ne može rezati bez ponovnog kodiranja u ovoj verziji. Podržani: MP3, PCM WAV, AAC/M4A, AMR, Opus i Vorbis. FLAC i ALAC trenutno nisu podržani.");
                s.track=i;s.durationUs=fmt.getLong(MediaFormat.KEY_DURATION);s.sampleRate=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);s.channels=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                return s;
            }
            throw new IOException("Fajl nema podržanu audio traku.");
        } catch(RuntimeException e) {throw new IOException("Fajl se ne može otvoriti. Izaberite ispravan audio fajl.",e);}
        finally {ex.release();}
    }
    static LosslessAudio.Result trim(Source s,TimeRange range,File output)throws IOException {
        if(s.direct!=null)return LosslessAudio.trim(s.file,s.direct,range,output);
        if(range.startUs>=s.durationUs||range.endUs>s.durationUs+1000)throw new IOException("Izabrani raspon prelazi trajanje fajla.");
        MediaExtractor ex=new MediaExtractor();MediaMuxer mux=null;boolean started=false,success=false;
        try {
            ex.setDataSource(s.file.getAbsolutePath());ex.selectTrack(s.track);
            MediaFormat format=ex.getTrackFormat(s.track);
            mux=new MediaMuxer(output.getAbsolutePath(),s.muxFormat);int track=mux.addTrack(format);mux.start();started=true;
            ex.seekTo(range.startUs,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            ByteBuffer data=ByteBuffer.allocateDirect(1024*1024);MediaCodec.BufferInfo bi=new MediaCodec.BufferInfo();
            long first=-1,last=-1;int count=0;
            while(ex.getSampleTime()>=0 && ex.getSampleTime()<range.endUs) {
                long t=ex.getSampleTime();
                if(t<range.startUs) {ex.advance();continue;}
                long size=ex.getSampleSize();if(size>data.capacity())throw new IOException("Audio paket je prevelik.");
                data.clear();int n=ex.readSampleData(data,0);if(n<0)break;
                if((ex.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Šifrovani audio nije podržan.");
                if(first<0)first=t;last=t;
                int flags=(ex.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_SYNC)!=0?MediaCodec.BUFFER_FLAG_KEY_FRAME:0;
                bi.set(0,n,t-first,flags);mux.writeSampleData(track,data,bi);count++;ex.advance();
            }
            if(count==0)throw new IOException("Izabrani raspon ne sadrži nijedan puni audio paket.");
            long next=ex.getSampleTime();long end=next>last?next:s.durationUs;
            mux.stop();started=false;success=true;
            return new LosslessAudio.Result(first,end,output.length());
        } catch(RuntimeException e) {throw new IOException("Sistem ne podržava rezanje ovog fajla bez kodiranja.",e);}
        finally {
            ex.release();if(mux!=null){if(started)try{mux.stop();}catch(Exception ignored){}try{mux.release();}catch(Exception ignored){}}
            if(!success)output.delete();
        }
    }
    /** Decode only for visualization, in bounded work. The export path never calls this. */
    static float[] waveform(Source s) {
        float[] peaks=new float[160]; MediaExtractor ex=new MediaExtractor();MediaCodec decoder=null;
        try {
            ex.setDataSource(s.file.getAbsolutePath());int track=-1;
            for(int i=0;i<ex.getTrackCount();i++)if(ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith("audio/")){track=i;break;}
            if(track<0)return peaks;
            MediaFormat fmt=ex.getTrackFormat(track);ex.selectTrack(track);
            decoder=MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));decoder.configure(fmt,null,null,0);decoder.start();
            MediaCodec.BufferInfo bi=new MediaCodec.BufferInfo();
            // Sparse real PCM windows across the entire recording, never an invented waveform.
            for(int bin=0;bin<peaks.length;bin++) {
                if(Thread.currentThread().isInterrupted())return peaks;
                ex.seekTo(s.durationUs*bin/peaks.length,MediaExtractor.SEEK_TO_CLOSEST_SYNC);decoder.flush();
                boolean got=false;int encoding=AudioFormat.ENCODING_PCM_16BIT;
                for(int attempt=0;attempt<24 && !got;attempt++) {
                    int idx=decoder.dequeueInputBuffer(3000);
                    if(idx>=0){ByteBuffer b=decoder.getInputBuffer(idx);int n=ex.readSampleData(b,0);if(n<0)decoder.queueInputBuffer(idx,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);else {decoder.queueInputBuffer(idx,0,n,ex.getSampleTime(),0);ex.advance();}}
                    int out=decoder.dequeueOutputBuffer(bi,3000);
                    if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat output=decoder.getOutputFormat();if(output.containsKey(MediaFormat.KEY_PCM_ENCODING))encoding=output.getInteger(MediaFormat.KEY_PCM_ENCODING);}
                    if(out>=0) {
                        ByteBuffer pcm=decoder.getOutputBuffer(out).order(ByteOrder.LITTLE_ENDIAN);pcm.position(bi.offset);pcm.limit(bi.offset+bi.size);
                        float peak=0;
                        if(encoding==AudioFormat.ENCODING_PCM_FLOAT) {while(pcm.remaining()>=4)peak=Math.max(peak,Math.abs(pcm.getFloat()));}
                        else {while(pcm.remaining()>=2)peak=Math.max(peak,Math.abs(pcm.getShort()/32768f));}
                        peaks[bin]=Math.min(1,peak);decoder.releaseOutputBuffer(out,false);got=bi.size>0;
                    }
                }
            }
        } catch(Exception ignored) { /* A flat timeline remains usable if this decoder cannot provide PCM. */ }
        finally {ex.release();if(decoder!=null){try{decoder.stop();}catch(Exception ignored){}decoder.release();}}
        return peaks;
    }
}
