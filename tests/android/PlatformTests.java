// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

/** Run on a connected Android 10+ device. Does not change the system ringtone. */
public final class PlatformTests extends Instrumentation {
    private int assertions;
    private final StringBuilder report=new StringBuilder();
    private void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_OK;
        try{
            for(String name:new String[]{"cbr.mp3","vbr.mp3","pcm16.wav","audio.aac","audio.m4a","vorbis.ogg","opus.ogg"})testExport(name);
            File flac=asset("audio.flac");boolean rejected=false;try{AudioEngine.inspect(flac);}catch(IOException expected){rejected=true;}check(rejected,"FLAC is rejected honestly");
            testMediaStoreAndUi();
            testLocalizationAndLayout();
            result.putString("stream",report+"\nPASS "+assertions+" Android assertions\n");
        }catch(Throwable e){code=Activity.RESULT_CANCELED;result.putString("stream",report+"\nFAIL "+e+"\n"+android.util.Log.getStackTraceString(e));}
        finish(code,result);
    }
    private File asset(String name)throws IOException{
        File f=new File(getTargetContext().getCacheDir(),"test-"+name);
        try(InputStream in=getContext().getAssets().open(name);OutputStream out=new FileOutputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return f;
    }
    private void testExport(String name)throws Exception{
        File input=asset(name);AudioEngine.Source s=AudioEngine.inspect(input);File output=new File(getTargetContext().getCacheDir(),"test-cut."+s.extension);
        LosslessAudio.Result cut=AudioEngine.trim(s,new TimeRange(1_235_000,3_789_000),output);
        check(output.length()>0,name+" nonempty");check(Math.abs((cut.endUs-cut.startUs)-2_554_000)<150000,name+" boundaries");
        ArrayList<String> original=hashes(input),exported=hashes(output);
        check(!exported.isEmpty(),name+" extractor reads export");
        if(s.direct!=null && s.direct.kind==LosslessAudio.Kind.WAV) {
            LosslessAudio.Info out=LosslessAudio.inspect(output);
            check(Arrays.equals(s.direct.wavFormat,out.wavFormat),name+" unchanged PCM format");
        } else check(Collections.indexOfSubList(original,exported)>=0,name+" unchanged encoded packets");
        MediaPlayer p=new MediaPlayer();try{p.setDataSource(output.getAbsolutePath());p.prepare();check(p.getDuration()>2300&&p.getDuration()<2800,name+" Android playback duration");}finally{p.release();}
        float[] peaks=AudioEngine.waveform(s);float max=0;for(float v:peaks)max=Math.max(max,v);check(max>0,name+" actual waveform decodes");
        output.delete();input.delete();report.append("PASS Android export + playback + waveform: ").append(name).append('\n');
    }
    private ArrayList<String> hashes(File f)throws Exception{
        MediaExtractor ex=new MediaExtractor();ArrayList<String> result=new ArrayList<>();
        try{ex.setDataSource(f.getAbsolutePath());int track=-1;for(int i=0;i<ex.getTrackCount();i++)if(ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith("audio/")){track=i;break;}check(track>=0,"audio track exists");ex.selectTrack(track);
            ByteBuffer b=ByteBuffer.allocate(1024*1024);while(true){b.clear();int n=ex.readSampleData(b,0);if(n<0)break;MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] packet=new byte[n];b.position(0);b.get(packet);result.add(Arrays.toString(digest.digest(packet)));ex.advance();}
        }finally{ex.release();}return result;
    }
    private void testMediaStoreAndUi()throws Exception{
        Context context=getTargetContext();Uri uri=null;MainActivity activity=null;
        try{
            ContentValues cv=new ContentValues();cv.put(MediaStore.Audio.Media.DISPLAY_NAME,"Rez-test-original.mp3");cv.put(MediaStore.Audio.Media.MIME_TYPE,"audio/mpeg");cv.put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/Rez-tests");cv.put(MediaStore.Audio.Media.IS_PENDING,1);
            uri=context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cv);check(uri!=null,"MediaStore insert");
            try(InputStream in=getContext().getAssets().open("cbr.mp3");OutputStream out=context.getContentResolver().openOutputStream(uri)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            cv.clear();cv.put(MediaStore.Audio.Media.IS_PENDING,0);context.getContentResolver().update(uri,cv,null,null);
            Intent intent=new Intent(context,MainActivity.class).setAction(Intent.ACTION_VIEW).setDataAndType(uri,"audio/mpeg").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity=(MainActivity)startActivitySync(intent);waitForIdleSync();
            java.lang.reflect.Field source=MainActivity.class.getDeclaredField("source");source.setAccessible(true);
            boolean[] loaded={false};MainActivity a=activity;
            for(int i=0;i<100;i++){runOnMainSync(()->{try{loaded[0]=source.get(a)!=null;}catch(Exception e){throw new RuntimeException(e);}});if(loaded[0])break;Thread.sleep(100);}
            check(loaded[0],"SAF content URI import into UI");
            runOnMainSync(()->{
                View content=a.findViewById(android.R.id.content);
                check(findText(content,"Rez-test-original.mp3")!=null,"filename visible");
                EditText start=a.findViewById(R.id.range_start),end=a.findViewById(R.id.range_end);check(start!=null&&end!=null,"accessible range fields");
                start.setText("00:01.235");end.setText("00:03.789");
                TextView selection=a.findViewById(R.id.selection);check(selection.getText().toString().equals(a.getString(R.string.selection,"00:02.554")),"selection updates from typed values");
            });
            cv.clear();cv.put(MediaStore.Audio.Media.DISPLAY_NAME,"Rez-test-renamed.mp3");check(context.getContentResolver().update(uri,cv,null,null)==1,"MediaStore rename");
            report.append("PASS Android MediaStore + content import + editor + range fields\n");
        }finally{if(activity!=null){MainActivity a=activity;runOnMainSync(a::finish);}if(uri!=null)context.getContentResolver().delete(uri,null,null);}
    }
    private TextView findText(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=findText(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;}
    /** Uses isolated configuration contexts; never changes system locale/settings. */
    private void testLocalizationAndLayout() throws Exception {
        Context target=getTargetContext();
        Context english=localizedContext(target,"en",1f);
        for(String tag:new String[]{"en","bs","de","it","es","de-AT","es-MX","fr"}) {
            Context localized=localizedContext(target,tag,1f);
            for(java.lang.reflect.Field field:R.string.class.getFields()) {
                int id=field.getInt(null);String value=localized.getString(id);
                check(!value.isEmpty(),tag+" "+field.getName()+" nonempty");
                java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("%(\\d+)\\$[-+0-9.]*([sdf])").matcher(value);
                Object[] args=new Object[4];
                while(matcher.find()) {
                    int i=Integer.parseInt(matcher.group(1))-1;
                    String type=matcher.group(2);
                    if(type.equals("d"))args[i]=Integer.valueOf(2);else if(type.equals("f"))args[i]=Float.valueOf(44.1f);else args[i]="test";
                }
                check(!localized.getString(id,args).isEmpty(),tag+" "+field.getName()+" formats");
            }
            if(tag.equals("fr"))check(localized.getString(R.string.tagline).equals(english.getString(R.string.tagline)),"unsupported locale falls back to English");
            else if(!tag.equals("en"))check(!localized.getString(R.string.tagline).equals(english.getString(R.string.tagline)),tag+" translation selected");
            Context bs=localizedContext(target,"bs",1f);
            for(int id:new int[]{R.string.error_empty,R.string.error_name,R.string.error_end_before_start,R.string.error_platform_trim,R.string.error_time_format})
                check(LocalizedErrors.message(localized,new IOException(bs.getString(id))).equals(localized.getString(id)),tag+" diagnostic translation");
            check(LocalizedErrors.message(localized,new IOException(bs.getString(R.string.error_frame,"123"))).equals(localized.getString(R.string.error_frame,"123")),tag+" frame position translation");
            check(LocalizedErrors.message(localized,new IOException(bs.getString(R.string.error_codec,"audio/flac"))).equals(localized.getString(R.string.error_codec,"audio/flac")),tag+" codec translation");
            check(LocalizedErrors.message(localized,new IOException("untrusted provider diagnostic")).equals(localized.getString(R.string.error_generic)),tag+" unknown diagnostic fallback");
        }
        MainActivity activity=(MainActivity)startActivitySync(new Intent(target,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        try {
            runOnMainSync(()->{
                android.content.res.Resources resources=activity.getResources();
                android.content.res.Configuration saved=new android.content.res.Configuration(resources.getConfiguration());
                android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();metrics.setTo(resources.getDisplayMetrics());
                try {
                    java.lang.reflect.Method build=MainActivity.class.getDeclaredMethod("buildUi");build.setAccessible(true);
                    java.lang.reflect.Method last=MainActivity.class.getDeclaredMethod("showLast");last.setAccessible(true);
                    java.lang.reflect.Field editor=MainActivity.class.getDeclaredField("editor");editor.setAccessible(true);
                    java.lang.reflect.Field name=MainActivity.class.getDeclaredField("lastName");name.setAccessible(true);
                    for(String tag:new String[]{"en","bs","de","it","es"})for(float scale:new float[]{1f,1.5f,2f}) {
                        android.content.res.Configuration config=new android.content.res.Configuration(saved);
                        config.setLocales(new android.os.LocaleList(Locale.forLanguageTag(tag)));config.fontScale=scale;config.screenWidthDp=320;
                        android.util.DisplayMetrics testMetrics=new android.util.DisplayMetrics();testMetrics.setTo(metrics);testMetrics.scaledDensity=metrics.density*scale;
                        resources.updateConfiguration(config,testMetrics);
                        build.invoke(activity);((View)editor.get(activity)).setVisibility(View.VISIBLE);
                        name.set(activity,activity.getString(R.string.fallback_clip));last.invoke(activity);
                        View content=activity.findViewById(android.R.id.content);
                        int width=Math.round(320*metrics.density),height=Math.round(568*metrics.density);
                        content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                        content.layout(0,0,width,height);
                        checkButtons(content,tag+" font "+scale);
                    }
                } catch(Exception error) {throw new RuntimeException(error);}
                finally {resources.updateConfiguration(saved,metrics);}
            });
        } finally {runOnMainSync(activity::finish);}
        report.append("PASS localization resources, diagnostics, fallback, and 320dp controls in five languages at font scales 1/1.5/2\n");
    }
    private Context localizedContext(Context context,String tag,float scale) {
        android.content.res.Configuration config=new android.content.res.Configuration(context.getResources().getConfiguration());
        config.setLocales(new android.os.LocaleList(Locale.forLanguageTag(tag)));config.fontScale=scale;
        return context.createConfigurationContext(config);
    }
    private void checkButtons(View view,String label) {
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof Button) {
            Button button=(Button)view;android.text.Layout layout=button.getLayout();
            check(layout!=null,label+" button layout");
            if(layout!=null&&layout.getLineCount()>0) {
                int line=layout.getLineCount()-1;
                check(layout.getEllipsisCount(line)==0,label+" no ellipsis: "+button.getText());
                check(layout.getLineEnd(line)==button.getText().length(),label+" complete text: "+button.getText());
                check(layout.getHeight()<=button.getHeight()-button.getCompoundPaddingTop()-button.getCompoundPaddingBottom(),label+" button height: "+button.getText());
            }
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checkButtons(((ViewGroup)view).getChildAt(i),label);
    }
}
