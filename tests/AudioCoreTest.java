// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class AudioCoreTest {
    static int assertions;
    static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    interface Operation {void run()throws Exception;}
    static void rejects(Operation operation,String message)throws Exception{boolean rejected=false;try{operation.run();}catch(IOException|IllegalArgumentException e){rejected=true;}check(rejected,message);}
    public static void main(String[] args)throws Exception{
        File dir=new File(args[0]);
        check(TimeRange.parse("01:02.345")==62_345_000,"minutes parse");
        check(TimeRange.parse("1:02:03,004")==3_723_004_000L,"hours comma parse");
        check(TimeRange.parse("2.5")==2_500_000,"seconds parse");
        check(TimeRange.format(62_345_000).equals("01:02.345"),"format");
        for(String s:new String[]{"","-1","NaN","Infinity","1:60","1::2","1e3",".5","1:01:60","1.2:03","9.1234567"})rejects(()->TimeRange.parse(s),"reject time "+s);
        rejects(()->new TimeRange(100,100),"empty selection");rejects(()->new TimeRange(-1,100),"negative selection");
        check(TimeRange.safeName("song.mp3","mp3").equals("song.mp3"),"no duplicate extension");
        check(TimeRange.safeName(" ../a:b ","wav").equals(".._a_b.wav"),"sanitize filename");
        rejects(()->TimeRange.safeName(" ","mp3"),"empty filename");
        for(String name:new String[]{"cbr.mp3","vbr.mp3","mpeg2.mp3","mpeg25.mp3","pcm16.wav","pcm24.wav","float.wav","odd.wav","audio.aac"}){
            File input=new File(dir,name),output=new File(dir,"cut-"+name);byte[] before=Files.readAllBytes(input.toPath());
            LosslessAudio.Info info=LosslessAudio.inspect(input);
            long a=1_235_000,b=3_789_000;
            LosslessAudio.Result result=LosslessAudio.trim(input,info,new TimeRange(a,b),output);
            byte[] after=Files.readAllBytes(output.toPath());
            check(Arrays.equals(before,Files.readAllBytes(input.toPath())),name+" original preserved");
            check(result.bytes==after.length,name+" byte count");
            LosslessAudio.Info cut=LosslessAudio.inspect(output);
            check(Math.abs(cut.durationUs-(result.endUs-result.startUs))<100,name+" output duration");
            if(info.kind==LosslessAudio.Kind.WAV){
                check(result.startUs<=a&&a-result.startUs<100,name+" sample-aligned start");check(result.endUs>=b&&result.endUs-b<100,name+" sample-aligned end");
                check(Arrays.equals(info.wavFormat,cut.wavFormat),name+" format bit-identical");
                long sample=a*info.sampleRate/1_000_000;
                byte[] original=Arrays.copyOfRange(before,(int)(info.dataOffset+sample*info.blockAlign),(int)(info.dataOffset+sample*info.blockAlign+cut.dataSize));
                byte[] exported=Arrays.copyOfRange(after,(int)cut.dataOffset,(int)(cut.dataOffset+cut.dataSize));
                check(Arrays.equals(original,exported),name+" PCM samples bit-identical");
            }else{
                check(result.startUs<=a&&result.endUs>=b,name+" frame-aligned boundaries");
                ByteArrayOutputStream expected=new ByteArrayOutputStream();int frames=0;
                for(LosslessAudio.Frame f:info.frames)if(f.endUs>a&&f.startUs<b){expected.write(before,(int)f.offset,f.size);frames++;}
                ByteArrayOutputStream actual=new ByteArrayOutputStream();
                for(LosslessAudio.Frame f:cut.frames)actual.write(after,(int)f.offset,f.size);
                check(Arrays.equals(expected.toByteArray(),actual.toByteArray()),name+" all audio packets bit-identical");
                check(cut.frames.size()==frames,name+" frame count");
                check(a-result.startUs<60_000&&result.endUs-b<60_000,name+" frame accuracy");
            }
            rejects(()->LosslessAudio.trim(input,info,new TimeRange(info.durationUs+1,info.durationUs+100000),new File(dir,"invalid.out")),name+" out-of-range rejected");
            rejects(()->LosslessAudio.trim(input,info,new TimeRange(0,100000),input),name+" overwrite blocked");
            File full=new File(dir,"full-"+name);LosslessAudio.trim(input,info,new TimeRange(0,info.durationUs),full);
            check(LosslessAudio.inspect(full).durationUs==info.durationUs,name+" full range");
            File shortCut=new File(dir,"tiny-"+name);LosslessAudio.trim(input,info,new TimeRange(200000,200001),shortCut);check(shortCut.length()>0,name+" tiny valid selection");
            System.out.println("PASS "+name+" ["+info.kind+", "+info.sampleRate+" Hz] "+TimeRange.format(result.startUs)+"–"+TimeRange.format(result.endUs));
        }
        File bad=new File(dir,"bad.bin");Files.write(bad.toPath(),new byte[128]);rejects(()->LosslessAudio.inspect(bad),"invalid format rejected");
        File truncated=new File(dir,"truncated.mp3");byte[] mp3=Files.readAllBytes(new File(dir,"cbr.mp3").toPath());Files.write(truncated.toPath(),Arrays.copyOf(mp3,mp3.length-5));rejects(()->LosslessAudio.inspect(truncated),"incomplete MP3 rejected");
        File brokenWav=new File(dir,"truncated.wav");byte[] wav=Files.readAllBytes(new File(dir,"pcm16.wav").toPath());Files.write(brokenWav.toPath(),Arrays.copyOf(wav,wav.length-5));rejects(()->LosslessAudio.inspect(brokenWav),"incomplete WAV rejected");
        // ID3v2.4 syncsafe size and footer handling.
        byte[] raw=Files.readAllBytes(new File(dir,"full-cbr.mp3").toPath());
        ByteArrayOutputStream tagged=new ByteArrayOutputStream();tagged.write(new byte[]{'I','D','3',4,0,16,0,0,0,4});tagged.write(new byte[14]);tagged.write(raw);
        File id3=new File(dir,"id3-footer.mp3");Files.write(id3.toPath(),tagged.toByteArray());
        check(LosslessAudio.inspect(id3).durationUs==LosslessAudio.inspect(new File(dir,"full-cbr.mp3")).durationUs,"ID3 footer ignored");
        ByteArrayOutputStream apeBytes=new ByteArrayOutputStream();apeBytes.write(raw);
        byte[] footer=new byte[32];System.arraycopy("APETAGEX".getBytes("US-ASCII"),0,footer,0,8);footer[8]=(byte)0xd0;footer[9]=7;footer[12]=32;
        apeBytes.write(footer);byte[] id3v1=new byte[128];id3v1[0]='T';id3v1[1]='A';id3v1[2]='G';apeBytes.write(id3v1);
        File ape=new File(dir,"ape-id3v1.mp3");Files.write(ape.toPath(),apeBytes.toByteArray());
        check(LosslessAudio.inspect(ape).durationUs==LosslessAudio.inspect(new File(dir,"full-cbr.mp3")).durationUs,"APE and ID3v1 tags ignored");
        System.out.println("PASS "+assertions+" assertions");
    }
}
