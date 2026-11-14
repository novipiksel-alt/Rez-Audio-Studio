// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pure Java packet/sample copying. No decoder or encoder is involved. */
public final class LosslessAudio {
    public enum Kind { MP3, WAV, ADTS }
    public static final class Frame {
        public final long offset, startUs, endUs;
        public final int size;
        Frame(long offset, int size, long startUs, long endUs) {
            this.offset=offset; this.size=size; this.startUs=startUs; this.endUs=endUs;
        }
    }
    public static final class Info {
        public Kind kind;
        public long durationUs, dataOffset, dataSize;
        public int sampleRate, blockAlign, channels;
        public byte[] wavFormat, mp3Index;
        public int indexMarker;
        public final List<Frame> frames = new ArrayList<>();
        public String extension() { return kind == Kind.MP3 ? "mp3" : kind == Kind.WAV ? "wav" : "aac"; }
        public String mime() { return kind == Kind.MP3 ? "audio/mpeg" : kind == Kind.WAV ? "audio/wav" : "audio/aac"; }
    }
    public static final class Result {
        public final long startUs, endUs, bytes;
        public Result(long startUs,long endUs,long bytes) { this.startUs=startUs;this.endUs=endUs;this.bytes=bytes; }
    }
    public static Info inspect(File input) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(input,"r")) {
            if (f.length()<12) throw new IOException("Fajl je prazan ili oštećen.");
            byte[] magic=new byte[12]; f.readFully(magic);
            if (ascii(magic,0,4).equals("RIFF") && ascii(magic,8,4).equals("WAVE")) return wav(f);
            return packets(f);
        }
    }
    private static Info wav(RandomAccessFile f) throws IOException {
        Info info=new Info();info.kind=Kind.WAV;
        f.seek(12);
        while (f.getFilePointer()+8<=f.length()) {
            byte[] tag=new byte[4]; f.readFully(tag); long size=u32(f); long pos=f.getFilePointer();
            if (size>f.length()-pos) throw new IOException("Nepotpun WAV blok.");
            String id=ascii(tag,0,4);
            if (id.equals("fmt ")) {
                if(size<16||size>65536) throw new IOException("Neispravan WAV format.");
                info.wavFormat=new byte[(int)size];f.readFully(info.wavFormat);
            } else if(id.equals("data") && info.dataSize==0) { info.dataOffset=pos;info.dataSize=size; }
            f.seek(pos+size+(size&1));
        }
        if(info.wavFormat==null || info.dataSize==0) throw new IOException("WAV nema audio podatke.");
        byte[] b=info.wavFormat; int format=u16(b,0);
        if(format==65534) {
            if (b.length<40 || u16(b,16)<22) throw new IOException("Nepotpun WAVE extensible format.");
            byte[] guidTail={0,0,0,0,16,0,(byte)128,0,0,(byte)170,0,56,(byte)155,113};
            for(int i=0;i<14;i++) if(b[26+i]!=guidTail[i]) throw new IOException("Nepodržan WAV kodek.");
            format=u16(b,24);
        }
        if(format!=1 && format!=3) throw new IOException("Podržani su PCM i float WAV; komprimovani WAV nije podržan.");
        info.channels=u16(b,2);info.sampleRate=(int)le32(b,4);info.blockAlign=u16(b,12);
        int bits=u16(b,14);
        if(info.sampleRate<=0||info.channels<=0||bits==0||info.blockAlign!=info.channels*((bits+7)/8)||info.dataSize%info.blockAlign!=0)
            throw new IOException("Neispravan WAV raspored uzoraka.");
        info.durationUs=(info.dataSize/info.blockAlign)*1_000_000L/info.sampleRate;
        return info;
    }
    private static Info packets(RandomAccessFile f) throws IOException {
        Info info=new Info();long pos=0, limit=audioEnd(f), samples=0; int rate=0; boolean found=false;
        f.seek(0);byte[] first=new byte[10];f.readFully(first);
        if(ascii(first,0,3).equals("ID3")) {
            for(int i=6;i<10;i++) if((first[i]&128)!=0) throw new IOException("Neispravan ID3 tag.");
            pos=10+((first[6]&127)<<21)+((first[7]&127)<<14)+((first[8]&127)<<7)+(first[9]&127);
            if(first[3]==4 && (first[5]&16)!=0) pos+=10;
            if(pos>limit) throw new IOException("Nepotpun ID3 tag.");
        }
        long scanLimit=Math.min(limit,pos+65536);
        while(pos+4<=limit) {
            f.seek(pos); int h=f.readInt(); int[] mp=mp3(h); int[] aa=adts(f,pos,h);
            int[] header=info.kind==Kind.MP3?mp:info.kind==Kind.ADTS?aa:mp!=null?mp:aa;
            if(header==null) {
                if(!found && pos<scanLimit) {pos++;continue;}
                // Known trailing tags are metadata, never emitted into the cut.
                f.seek(pos);byte[] tail=new byte[(int)Math.min(8,limit-pos)];f.readFully(tail);
                if(ascii(tail,0,Math.min(3,tail.length)).equals("TAG") || (tail.length==8 && ascii(tail,0,8).equals("APETAGEX"))) break;
                throw new IOException("Oštećen ili nepodržan audio frejm na poziciji "+pos+".");
            }
            if(pos+header[0]>limit) throw new IOException("Posljednji audio frejm je nepotpun.");
            if(!found) {info.kind=mp!=null?Kind.MP3:Kind.ADTS;rate=header[1];info.channels=header[3];}
            if(rate!=header[1]) throw new IOException("Promjenljiva frekvencija uzorkovanja nije podržana.");
            // Xing/Info/VBRI are index frames, not audio. Carrying their old frame
            // counts into a short cut makes players report the original duration.
            if (!found && info.kind==Kind.MP3) {
                int marker=indexMarker(f,pos,h,header[0]);
                if(marker>=0) {
                    // Keep the neutral index frame, rebuild its duration on export.
                    f.seek(pos+marker);int tag=f.readInt();
                    if(tag!=0x56425249 && marker+16<=header[0]) {info.mp3Index=new byte[header[0]];f.seek(pos);f.readFully(info.mp3Index);info.indexMarker=marker;}
                    pos+=header[0]; continue;
                }
            }
            long start=samples*1_000_000L/rate;samples+=header[2];
            info.frames.add(new Frame(pos,header[0],start,samples*1_000_000L/rate));
            found=true;pos+=header[0];
        }
        if(info.frames.isEmpty()) throw new IOException("Nije MP3, PCM WAV ili ADTS AAC.");
        info.sampleRate=rate;info.durationUs=samples*1_000_000L/rate;return info;
    }
    private static long audioEnd(RandomAccessFile f)throws IOException {
        long end=f.length();
        if(end>=128){f.seek(end-128);if((f.readInt()>>>8)==0x544147)end-=128;}
        if(end>=32){
            f.seek(end-32);byte[] tag=new byte[8];f.readFully(tag);
            if(ascii(tag,0,8).equals("APETAGEX")) {
                u32(f);long size=u32(f);
                if(size<32||size>end)throw new IOException("Neispravan APE tag.");
                end-=size;
                if(end>=32){f.seek(end-32);f.readFully(tag);if(ascii(tag,0,8).equals("APETAGEX"))end-=32;}
            }
        }
        return end;
    }
    private static int indexMarker(RandomAccessFile f,long pos,int h,int size)throws IOException {
        int version=(h>>>19)&3;boolean mono=((h>>>6)&3)==3;
        int side=version==3?(mono?17:32):(mono?9:17);
        int crc=(h&0x10000)==0?2:0;
        int[] offsets={4+crc+side,36};
        for(int offset:offsets)if(offset+4<=size){f.seek(pos+offset);int marker=f.readInt();if(marker==0x58696e67||marker==0x496e666f||marker==0x56425249)return offset;}
        return -1;
    }
    private static int[] mp3(int h) {
        if((h&0xffe00000)!=0xffe00000) return null;
        int version=(h>>>19)&3, layer=(h>>>17)&3, br=(h>>>12)&15, sr=(h>>>10)&3;
        if(version==1||layer!=1||br==0||br==15||sr==3) return null;
        int[] rates={44100,48000,32000}; int rate=rates[sr]/(version==3?1:version==2?2:4);
        int[] high={0,32,40,48,56,64,80,96,112,128,160,192,224,256,320};
        int[] low={0,8,16,24,32,40,48,56,64,80,96,112,128,144,160};
        int size=(version==3?144:72)*(version==3?high[br]:low[br])*1000/rate+((h>>>9)&1);
        return new int[]{size,rate,version==3?1152:576,((h>>>6)&3)==3?1:2};
    }
    private static int[] adts(RandomAccessFile f,long pos,int h) throws IOException {
        if((h&0xfff60000)!=0xfff00000 || pos+7>f.length()) return null;
        int freq=(h>>>10)&15;int[] rates={96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};
        if(freq>=rates.length) return null;
        f.seek(pos+4);int b4=f.readUnsignedByte(),b5=f.readUnsignedByte(),b6=f.readUnsignedByte();
        int size=((h&3)<<11)|(b4<<3)|(b5>>>5);int channels=(((h>>>8)&1)<<2)|((h>>>6)&3);
        if(size<((h&0x10000)!=0?7:9)) return null;
        return new int[]{size,rates[freq],1024*((b6&3)+1),channels};
    }
    public static Result trim(File input,Info info,TimeRange range,File output) throws IOException {
        if(input.getCanonicalPath().equals(output.getCanonicalPath())) throw new IOException("Izvorni fajl se ne smije prepisati.");
        if(range.startUs>=info.durationUs || range.endUs>info.durationUs+1000) throw new IOException("Izabrano vrijeme prelazi trajanje fajla.");
        try(RandomAccessFile f=new RandomAccessFile(input,"r"); OutputStream out=new BufferedOutputStream(new FileOutputStream(output))) {
            long start,end,bytes=0;
            if(info.kind==Kind.WAV) {
                long a=range.startUs*info.sampleRate/1_000_000L, b=Math.min(info.dataSize/info.blockAlign,(range.endUs*info.sampleRate+999999)/1_000_000L);
                long length=(b-a)*info.blockAlign;
                if(length<=0) throw new IOException("Isječak je kraći od jednog uzorka.");
                int fmt=info.wavFormat.length;long total=4+8+fmt+(fmt&1)+8+length+(length&1);
                if(total>0xffffffffL) throw new IOException("Isječak prelazi WAV ograničenje od 4 GB.");
                writeAscii(out,"RIFF");write32(out,total);writeAscii(out,"WAVEfmt ");write32(out,fmt);out.write(info.wavFormat);if((fmt&1)!=0)out.write(0);
                writeAscii(out,"data");write32(out,length);copy(f,out,info.dataOffset+a*info.blockAlign,length);if((length&1)!=0)out.write(0);
                start=a*1_000_000L/info.sampleRate;end=b*1_000_000L/info.sampleRate;bytes=total+8;
            } else {
                start=-1;end=-1;
                int count=0;long payloadBytes=0;
                for(Frame frame:info.frames)if(frame.endUs>range.startUs && frame.startUs<range.endUs){count++;payloadBytes+=frame.size;}
                if(info.mp3Index!=null && count>0) {
                    byte[] index=Arrays.copyOf(info.mp3Index,info.mp3Index.length);
                    Arrays.fill(index,info.indexMarker,index.length,(byte)0);
                    int m=info.indexMarker;
                    byte[] tag="Xing".getBytes(StandardCharsets.US_ASCII);System.arraycopy(tag,0,index,m,4);
                    putBe32(index,m+4,3); // frame count + byte count; omit stale TOC and LAME delay/padding
                    putBe32(index,m+8,count);putBe32(index,m+12,payloadBytes+index.length);
                    out.write(index);bytes+=index.length;
                }
                for(Frame frame:info.frames) {
                    if(frame.endUs<=range.startUs)continue;if(frame.startUs>=range.endUs)break;
                    if(start<0)start=frame.startUs;end=frame.endUs;
                    copy(f,out,frame.offset,frame.size);bytes+=frame.size;
                }
                if(bytes==0) throw new IOException("Nema audio frejmova u ovom rasponu.");
            }
            return new Result(start,end,bytes);
        } catch(IOException|RuntimeException e) {output.delete();throw e;}
    }
    private static void copy(RandomAccessFile f,OutputStream out,long pos,long length)throws IOException {
        f.seek(pos);byte[] b=new byte[65536];while(length>0){int n=f.read(b,0,(int)Math.min(length,b.length));if(n<0)throw new EOFException();out.write(b,0,n);length-=n;}
    }
    private static String ascii(byte[] b,int off,int len){return new String(b,off,len,StandardCharsets.US_ASCII);}
    private static int u16(byte[] b,int off){return (b[off]&255)|((b[off+1]&255)<<8);}
    private static long le32(byte[] b,int off){return (b[off]&255L)|((b[off+1]&255L)<<8)|((b[off+2]&255L)<<16)|((b[off+3]&255L)<<24);}
    private static long u32(RandomAccessFile f)throws IOException {byte[] b=new byte[4];f.readFully(b);return le32(b,0);}
    private static void putBe32(byte[] b,int at,long value){for(int i=0;i<4;i++)b[at+i]=(byte)(value>>>(24-i*8));}
    private static void writeAscii(OutputStream out,String s)throws IOException{out.write(s.getBytes(StandardCharsets.US_ASCII));}
    private static void write32(OutputStream out,long v)throws IOException{for(int i=0;i<4;i++)out.write((int)(v>>>(8*i))&255);}
}
