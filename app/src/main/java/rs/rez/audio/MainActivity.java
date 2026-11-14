// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff101411,CARD=0xff1a201b,TEXT=0xfff1f5eb,MUTED=0xffa0ad9f,ACCENT=0xffbcf776;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(), waveWorker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Future<?> waveJob;
    private AudioEngine.Source source;
    private String originalName="",lastName="";
    private Uri lastUri;
    private long startUs,endUs;
    private boolean busy=false,updating=false,ready=false,previewPending=false,dead=false;
    private File draft;
    private LosslessAudio.Result draftResult;
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private LinearLayout root,editor,lastCard;
    private TextView fileName,fileMeta,status,selection,playPosition;
    private EditText startField,endField;
    private Button pick,play,cut;
    private RangeView range;
    private final Runnable ticker=new Runnable(){public void run(){
        if(player==null||!ready)return;
        try {if(player.isPlaying()) {
            long t=player.getCurrentPosition()*1000L;
            if(t>=endUs){pause();return;}
            range.setPlayhead(t);playPosition.setText(TimeRange.format(t));handler.postDelayed(this,25);
        }}catch(IllegalStateException ignored){}
    }};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        buildUi();
        if(state!=null && state.getString("file")!=null) {
            File f=new File(state.getString("file"));String name=state.getString("name", getString(R.string.fallback_audio));
            long a=state.getLong("start"),b=state.getLong("end");
            if(f.isFile())loadCached(f,name,a,b);
            String saved=state.getString("last");if(saved!=null){lastUri=Uri.parse(saved);lastName=state.getString("lastName", getString(R.string.fallback_clip));showLast();}
        } else {
            cleanupCache();handleIntent(getIntent());
        }
    }
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=column();root.setPadding(dp(16),dp(16),dp(16),dp(26));scroll.addView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(dp(16),dp(16)+insets.getSystemWindowInsetTop(),dp(16),dp(26)+insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout header=row();TextView logo=text(getString(R.string.brand_wordmark),42,TEXT);logo.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL));header.addView(logo,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=text(getString(R.string.brand_badge),10,ACCENT);badge.setLetterSpacing(.16f);header.addView(badge);root.addView(header);
        addText(root,getString(R.string.tagline),23,TEXT,10);
        addText(root,getString(R.string.intro),14,MUTED,6);
        LinearLayout importCard=card();marginTop(importCard,24);root.addView(importCard);
        addText(importCard,getString(R.string.import_step),11,ACCENT,0);
        addText(importCard,getString(R.string.import_title),18,TEXT,10);
        addText(importCard,getString(R.string.supported_formats),12,MUTED,7);
        pick=button(getString(R.string.open_audio),true);marginTop(pick,16);importCard.addView(pick);pick.setOnClickListener(v->pickAudio());
        TextView privacy=text(getString(R.string.privacy),11,MUTED);privacy.setGravity(Gravity.CENTER);marginTop(privacy,10);root.addView(privacy);
        status=text(getString(R.string.ready),12,MUTED);marginTop(status,16);root.addView(status);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        editor=card();marginTop(editor,16);root.addView(editor);editor.setVisibility(View.GONE);
        addText(editor,getString(R.string.editor_step),11,ACCENT,0);
        fileName=text("",19,TEXT);fileName.setMaxLines(2);fileName.setEllipsize(TextUtils.TruncateAt.END);marginTop(fileName,12);editor.addView(fileName);
        fileMeta=text("",12,MUTED);marginTop(fileMeta,6);editor.addView(fileMeta);
        range=new RangeView(this);editor.addView(range,new LinearLayout.LayoutParams(-1,dp(176)));range.listener=(a,b)->{pause();startUs=a;endUs=b;updateFields();};
        LinearLayout zoomControls=controlGroup();marginTop(zoomControls,8);
        Button zoomOut=button(getString(R.string.zoom_out),false),zoomReset=button(getString(R.string.zoom_percent,100),false),zoomIn=button(getString(R.string.zoom_in),false);
        zoomOut.setContentDescription(getString(R.string.zoom_out_description));zoomIn.setContentDescription(getString(R.string.zoom_in_description));
        zoomReset.setContentDescription(getString(R.string.zoom_reset_description));
        addControl(zoomControls,zoomOut);
        addControl(zoomControls,zoomReset);
        addControl(zoomControls,zoomIn);editor.addView(zoomControls);
        zoomIn.setOnClickListener(v->{range.zoom(2f);zoomReset.setText(getString(R.string.zoom_percent,range.zoomPercent()));});
        zoomOut.setOnClickListener(v->{range.zoom(.5f);zoomReset.setText(getString(R.string.zoom_percent,range.zoomPercent()));});
        zoomReset.setOnClickListener(v->{range.resetZoom();zoomReset.setText(getString(R.string.zoom_percent,100));});
        addText(editor,getString(R.string.wave_help),11,MUTED,6);
        LinearLayout times=column();LinearLayout a=column(),b=column();times.addView(a,new LinearLayout.LayoutParams(-1,-2));Space gap=new Space(this);times.addView(gap,new LinearLayout.LayoutParams(1,dp(8)));times.addView(b,new LinearLayout.LayoutParams(-1,-2));
        addText(a,getString(R.string.start_label),10,MUTED,0);addText(b,getString(R.string.end_label),10,MUTED,0);
        startField=timeField(getString(R.string.start_description));endField=timeField(getString(R.string.end_description));startField.setId(R.id.range_start);endField.setId(R.id.range_end);a.addView(startField);b.addView(endField);editor.addView(times);
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void onTextChanged(CharSequence s,int st,int before,int count){if(!updating){pause();readRange(false);}}public void afterTextChanged(Editable e){}};
        startField.addTextChangedListener(watcher);endField.addTextChangedListener(watcher);
        LinearLayout nudges=controlGroup();marginTop(nudges,8);
        Button left=button(getString(R.string.nudge_start),false),right=button(getString(R.string.nudge_end),false);
        left.setTextSize(11);right.setTextSize(11);addControl(nudges,left);addControl(nudges,right);editor.addView(nudges);
        left.setOnClickListener(v->{if(readRange(true)){pause();startUs=Math.max(0,startUs-100000);updateFields();}});
        right.setOnClickListener(v->{if(readRange(true)){pause();endUs=Math.min(source.durationUs,endUs+100000);updateFields();}});
        selection=text("",13,ACCENT);selection.setId(R.id.selection);selection.setGravity(Gravity.CENTER);marginTop(selection,14);editor.addView(selection);
        LinearLayout transport=column();marginTop(transport,10);play=button(getString(R.string.play),false);transport.addView(play,new LinearLayout.LayoutParams(-1,-2));play.setOnClickListener(v->preview());
        playPosition=text(TimeRange.format(0),12,MUTED);playPosition.setTypeface(Typeface.MONOSPACE);playPosition.setPadding(dp(12),0,0,0);transport.addView(playPosition);editor.addView(transport);
        cut=button(getString(R.string.trim),true);marginTop(cut,14);editor.addView(cut);cut.setOnClickListener(v->makeCut());
        addText(editor,getString(R.string.trim_help),11,MUTED,12);
        lastCard=card();marginTop(lastCard,16);root.addView(lastCard);lastCard.setVisibility(View.GONE);
        LinearLayout online=card();marginTop(online,20);root.addView(online);
        addText(online,getString(R.string.youtube_heading),11,ACCENT,0);
        addText(online,getString(R.string.youtube_subtitle),16,TEXT,9);
        addText(online,getString(R.string.youtube_help),12,MUTED,7);
        Button yt=button(getString(R.string.youtube_button),false);marginTop(yt,12);online.addView(yt);yt.setOnClickListener(v->youtubeDialog());
        TextView footer=text(getString(R.string.footer,getString(R.string.app_version)),10,MUTED);footer.setGravity(Gravity.CENTER);marginTop(footer,24);root.addView(footer);
        setContentView(scroll);
    }
    private void pickAudio(){
        if(busy)return;pause();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivityForResult(i,10);}catch(ActivityNotFoundException e){error(getString(R.string.picker_unavailable));}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==10&&result==RESULT_OK&&data!=null&&data.getData()!=null)importAudio(data.getData());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){
        if(i==null||busy)return;Uri u=null;
        if(Intent.ACTION_SEND.equals(i.getAction()))u=i.getParcelableExtra(Intent.EXTRA_STREAM);
        else if(Intent.ACTION_VIEW.equals(i.getAction()))u=i.getData();
        if(u!=null&&"content".equals(u.getScheme()))importAudio(u);
    }
    private String displayName(Uri uri){
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}return getString(R.string.fallback_audio);
    }
    private void importAudio(Uri uri){
        if(busy)return;String name=displayName(uri);setBusy(true,getString(R.string.loading,name));cancelWave();pause();
        worker.execute(()->{
            File file=null;
            try {
                file=File.createTempFile("source-",".audio",getCacheDir());
                try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new BufferedOutputStream(new FileOutputStream(file))){
                    if(in==null)throw new IOException(getString(R.string.file_unavailable));byte[] buffer=new byte[65536];long bytes=0;int n;
                    while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException(getString(R.string.import_interrupted));bytes+=n;if(bytes>2_000_000_000L||getCacheDir().getUsableSpace()<5_000_000L)throw new IOException(getString(R.string.import_space));out.write(buffer,0,n);}
                }
                AudioEngine.Source loaded=AudioEngine.inspect(file);if(loaded.durationUs<=0)throw new IOException(getString(R.string.no_duration));
                runOnUiThread(()->{if(dead){loaded.file.delete();return;}acceptSource(loaded,name,0,Math.min(30_000_000,loaded.durationUs));});
            }catch(Exception e){if(file!=null)file.delete();runOnUiThread(()->{if(!dead){setBusy(false,getString(R.string.import_failed));error(message(e));}});}
        });
    }
    private void loadCached(File f,String name,long a,long b){
        setBusy(true,getString(R.string.restoring));worker.execute(()->{try{AudioEngine.Source s=AudioEngine.inspect(f);runOnUiThread(()->{if(!dead)acceptSource(s,name,a,Math.min(b,s.durationUs));});}catch(Exception e){runOnUiThread(()->{if(!dead)setBusy(false,getString(R.string.reopen));});}});
    }
    private void acceptSource(AudioEngine.Source s,String name,long a,long b){
        releasePlayer();if(source!=null&&!source.file.equals(s.file))source.file.delete();source=s;originalName=name;
        startUs=a;endUs=b>a?b:s.durationUs;editor.setVisibility(View.VISIBLE);fileName.setText(name);
        fileMeta.setText(getString(R.string.file_metadata,s.codec,s.sampleRate/1000f,s.channels,TimeRange.format(s.durationUs)));
        range.setAudio(s.durationUs);updateFields();setBusy(false,getString(R.string.audio_ready));preparePlayer();
        waveJob=waveWorker.submit(()->{float[] peaks=AudioEngine.waveform(s);runOnUiThread(()->{if(!dead&&source==s)range.setPeaks(peaks);});});
    }
    private void updateFields(){updating=true;startField.setText(TimeRange.format(startUs));endField.setText(TimeRange.format(endUs));updating=false;range.setRange(startUs,endUs);selection.setText(getString(R.string.selection,TimeRange.format(endUs-startUs)));}
    private boolean readRange(boolean show){
        if(source==null)return false;
        try{long a=TimeRange.parse(startField.getText().toString()),b=TimeRange.parse(endField.getText().toString());new TimeRange(a,b);
            if(b>source.durationUs+1000||a>=source.durationUs)throw new IllegalArgumentException(getString(R.string.range_bounds));
            startUs=a;endUs=Math.min(b,source.durationUs);range.setRange(startUs,endUs);selection.setText(getString(R.string.selection,TimeRange.format(endUs-startUs)));startField.setError(null);endField.setError(null);return true;
        }catch(IllegalArgumentException e){if(show){endField.setError(message(e));toast(message(e));}return false;}
    }
    private void preparePlayer(){
        releasePlayer();if(source==null)return;
        MediaPlayer p=new MediaPlayer();player=p;
        try{p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());p.setDataSource(source.file.getAbsolutePath());
            p.setOnPreparedListener(m->{if(player==m){ready=true;play.setEnabled(!busy);}});
            p.setOnSeekCompleteListener(m->{if(player==m&&previewPending){previewPending=false;try{m.start();play.setText(getString(R.string.pause));handler.post(ticker);}catch(Exception e){pause();}}});
            p.setOnCompletionListener(m->pause());p.setOnErrorListener((m,what,extra)->{if(player==m){ready=false;previewPending=false;play.setText(getString(R.string.play));status.setText(getString(R.string.playback_unsupported));}return true;});p.prepareAsync();
        }catch(Exception e){releasePlayer();status.setText(getString(R.string.preview_unsupported));}
    }
    private void preview(){
        if(!readRange(true)||busy)return;
        if(player==null){preparePlayer();toast(getString(R.string.preview_preparing));return;}
        if(!ready){toast(getString(R.string.preview_preparing));return;}
        try{if(player.isPlaying()||previewPending){pause();return;}
            focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener(change->{if(change<0)pause();}).build();
            if(audioManager.requestAudioFocus(focusRequest)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){toast(getString(R.string.audio_busy));return;}
            previewPending=true;player.seekTo(startUs/1000,MediaPlayer.SEEK_CLOSEST);play.setText(getString(R.string.preparing));
        }catch(Exception e){pause();error(getString(R.string.preview_error,message(e)));}
    }
    private void pause(){
        previewPending=false;handler.removeCallbacks(ticker);if(player!=null&&ready)try{if(player.isPlaying())player.pause();}catch(Exception ignored){}
        if(focusRequest!=null){audioManager.abandonAudioFocusRequest(focusRequest);focusRequest=null;}
        if(play!=null)play.setText(getString(R.string.play));if(range!=null)range.setPlayhead(-1);
    }
    private void releasePlayer(){pause();ready=false;if(player!=null){player.release();player=null;}}
    private void makeCut(){
        if(busy||!readRange(true))return;pause();cancelWave();hideKeyboard();
        AudioEngine.Source selected=source;TimeRange chosen=new TimeRange(startUs,endUs);setBusy(true,getString(R.string.trimming));
        worker.execute(()->{File output=null;try{output=File.createTempFile("cut-","."+selected.extension,getCacheDir());LosslessAudio.Result r=AudioEngine.trim(selected,chosen,output);File done=output;
            runOnUiThread(()->{if(dead){done.delete();return;}draft=done;draftResult=r;setBusy(false,getString(R.string.clip_ready_status));saveDialog();});
        }catch(Exception e){if(output!=null)output.delete();runOnUiThread(()->{if(!dead){setBusy(false,getString(R.string.trim_failed));error(message(e));}});}});
    }
    private void saveDialog(){
        LinearLayout box=column();box.setPadding(dp(24),dp(8),dp(24),0);
        addText(box,getString(R.string.actual_range,TimeRange.format(draftResult.startUs),TimeRange.format(draftResult.endUs)),12,MUTED,0);
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextColor(TEXT);name.setTextSize(17);name.setHint(getString(R.string.clip_name));name.setSelectAllOnFocus(true);
        String stem=originalName.replaceFirst("\\.[^.]+$","");name.setText(getString(R.string.default_clip_name,stem));box.addView(name);addText(box,getString(R.string.format_preserved,source.extension.toUpperCase(Locale.ROOT)),12,MUTED,7);
        CheckBox ringtone=new CheckBox(this);ringtone.setText(getString(R.string.set_ringtone_now));ringtone.setTextColor(TEXT);ringtone.setTextSize(13);marginTop(ringtone,12);box.addView(ringtone);
        AlertDialog dialog=new AlertDialog.Builder(this).setCustomTitle(dialogTitle(R.string.clip_ready_title)).setView(scrollable(box)).setNegativeButton(getString(R.string.cancel),(d,w)->discardDraft()).setPositiveButton(getString(R.string.save),null).create();
        dialog.setOnCancelListener(d->discardDraft());dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{String filename=TimeRange.safeName(name.getText().toString(),source.extension);dialog.dismiss();saveDraft(filename,ringtone.isChecked());}catch(IllegalArgumentException e){name.setError(message(e));}}));showDialog(dialog);
    }
    private void discardDraft(){if(draft!=null){draft.delete();draft=null;}status.setText(getString(R.string.clip_discarded));}
    private void saveDraft(String name,boolean asRingtone){
        File file=draft;draft=null;String mime=source.mime;long duration=draftResult.endUs-draftResult.startUs;
        setBusy(true,getString(R.string.saving,name));
        worker.execute(()->{
            Uri uri=null;
            try{
                ContentValues cv=new ContentValues();cv.put(MediaStore.Audio.Media.DISPLAY_NAME,name);cv.put(MediaStore.Audio.Media.TITLE,name);cv.put(MediaStore.Audio.Media.MIME_TYPE,mime);
                cv.put(MediaStore.Audio.Media.RELATIVE_PATH,(asRingtone?Environment.DIRECTORY_RINGTONES:Environment.DIRECTORY_MUSIC)+"/Rez");cv.put(MediaStore.Audio.Media.IS_RINGTONE,asRingtone?1:0);cv.put(MediaStore.Audio.Media.IS_MUSIC,asRingtone?0:1);cv.put(MediaStore.Audio.Media.DURATION,duration/1000);cv.put(MediaStore.Audio.Media.IS_PENDING,1);
                uri=getContentResolver().insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),cv);if(uri==null)throw new IOException(getString(R.string.create_failed));
                try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IOException(getString(R.string.write_unavailable));copy(in,out);}
                ContentValues done=new ContentValues();done.put(MediaStore.Audio.Media.IS_PENDING,0);getContentResolver().update(uri,done,null,null);Uri saved=uri;
                runOnUiThread(()->{if(dead)return;lastUri=saved;lastName=name;showLast();setBusy(false,getString(R.string.saved_location,asRingtone?"Ringtones":"Music"));if(asRingtone)requestRingtone(saved);});
            }catch(Exception e){if(uri!=null)try{getContentResolver().delete(uri,null,null);}catch(Exception ignored){}
                // Keep the cut for retry while the activity is alive.
                runOnUiThread(()->{if(dead){file.delete();return;}draft=file;setBusy(false,getString(R.string.save_failed_status));showDialog(new AlertDialog.Builder(this).setCustomTitle(dialogTitle(R.string.save_failed)).setMessage(message(e)).setPositiveButton(getString(R.string.retry),(d,w)->saveDialog()).setNegativeButton(getString(R.string.cancel),(d,w)->discardDraft()).setOnCancelListener(d->discardDraft()).create());});return;
            }
            file.delete();
        });
    }
    private void showLast(){
        lastCard.removeAllViews();lastCard.setVisibility(View.VISIBLE);addText(lastCard,getString(R.string.saved),11,ACCENT,0);addText(lastCard,lastName,17,TEXT,10);
        LinearLayout actions=controlGroup();marginTop(actions,10);Button listen=button(getString(R.string.open),false),share=button(getString(R.string.share),false);addControl(actions,listen);addControl(actions,share);lastCard.addView(actions);
        listen.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(lastUri,getContentResolver().getType(lastUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);}catch(Exception e){error(getString(R.string.open_unsupported));}});
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND).setType(getContentResolver().getType(lastUri)).putExtra(Intent.EXTRA_STREAM,lastUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri(getString(R.string.fallback_clip),lastUri));try{startActivity(Intent.createChooser(i,getString(R.string.share_clip)));}catch(Exception e){error(getString(R.string.share_unavailable));}});
        LinearLayout more=controlGroup();Button rename=button(getString(R.string.rename),false),ring=button(getString(R.string.set_ringtone),false);rename.setTextSize(12);ring.setTextSize(12);addControl(more,rename);addControl(more,ring);lastCard.addView(more);rename.setOnClickListener(v->renameLast());ring.setOnClickListener(v->requestRingtone(lastUri));
    }
    private void renameLast(){
        EditText name=new EditText(this);name.setText(lastName);name.setSingleLine(true);name.setSelectAllOnFocus(true);name.setPadding(dp(24),dp(12),dp(24),dp(12));
        AlertDialog d=new AlertDialog.Builder(this).setCustomTitle(dialogTitle(R.string.new_name)).setView(name).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.rename),null).create();d.setOnShowListener(w->d.getButton(-1).setOnClickListener(v->{try{String ext=lastName.substring(lastName.lastIndexOf('.')+1);String n=TimeRange.safeName(name.getText().toString(),ext);ContentValues cv=new ContentValues();cv.put(MediaStore.Audio.Media.DISPLAY_NAME,n);cv.put(MediaStore.Audio.Media.TITLE,n);if(getContentResolver().update(lastUri,cv,null,null)==0)throw new IOException(getString(R.string.clip_unavailable));lastName=n;showLast();d.dismiss();}catch(Exception e){name.setError(message(e));}}));showDialog(d);
    }
    private void requestRingtone(Uri uri){
        if(uri==null)return;pause();
        if(!Settings.System.canWrite(this)){
            showDialog(new AlertDialog.Builder(this).setCustomTitle(dialogTitle(R.string.ringtone_permission_title)).setMessage(getString(R.string.ringtone_permission_help)).setNegativeButton(getString(R.string.later),null).setPositiveButton(getString(R.string.open_settings),(d,w)->{
                getPreferences(0).edit().putString("pendingRingtone",uri.toString()).apply();
                try{startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){getPreferences(0).edit().remove("pendingRingtone").apply();error(getString(R.string.settings_help));}
            }).create());return;
        }
        applyRingtone(uri);
    }
    private void applyRingtone(Uri uri){
        try{ContentValues cv=new ContentValues();cv.put(MediaStore.Audio.Media.IS_RINGTONE,1);getContentResolver().update(uri,cv,null,null);RingtoneManager.setActualDefaultRingtoneUri(this,RingtoneManager.TYPE_RINGTONE,uri);toast(getString(R.string.ringtone_set));status.setText(getString(R.string.ringtone_ready));}
        catch(Exception e){error(getString(R.string.ringtone_failed));}
    }
    @Override protected void onResume(){super.onResume();String p=getPreferences(0).getString("pendingRingtone",null);if(p!=null){getPreferences(0).edit().remove("pendingRingtone").apply();if(Settings.System.canWrite(this))applyRingtone(Uri.parse(p));else toast(getString(R.string.ringtone_denied));}}
    private void youtubeDialog(){
        LinearLayout box=column();box.setPadding(dp(24),dp(4),dp(24),0);
        addText(box,getString(R.string.youtube_dialog_help),14,MUTED,0);
        EditText link=new EditText(this);link.setSingleLine(true);link.setHint(getString(R.string.youtube_hint));link.setTextSize(13);link.setTextColor(TEXT);link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);box.addView(link);
        addText(box,getString(R.string.youtube_legal),11,MUTED,12);
        AlertDialog d=new AlertDialog.Builder(this).setCustomTitle(dialogTitle(R.string.youtube_title)).setView(scrollable(box)).setNegativeButton(getString(R.string.close),null).setNeutralButton(getString(R.string.install_seal),(a,b)->openWeb("https://github.com/JunkFood02/Seal/releases/latest")).setPositiveButton(getString(R.string.send_seal),null).create();
        d.setOnShowListener(w->d.getButton(-1).setOnClickListener(v->{
            String value=link.getText().toString().trim();Uri uri=Uri.parse(value);String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||!(host.equalsIgnoreCase("youtu.be")||host.equalsIgnoreCase("youtube.com")||host.toLowerCase(Locale.ROOT).endsWith(".youtube.com"))){link.setError(getString(R.string.invalid_youtube));return;}
            Intent i=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,value).setPackage("com.junkfood.seal");
            try{startActivity(i);d.dismiss();}catch(ActivityNotFoundException e){toast(getString(R.string.seal_missing));}
        }));showDialog(d);
    }
    private void openWeb(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){error(getString(R.string.browser_unavailable));}}
    private void setBusy(boolean value,String message){busy=value;status.setText(message);pick.setEnabled(!value);if(cut!=null)cut.setEnabled(!value);if(play!=null)play.setEnabled(!value);if(range!=null)range.setEnabled(!value);if(startField!=null)startField.setEnabled(!value);if(endField!=null)endField.setEnabled(!value);}
    private void cancelWave(){if(waveJob!=null){waveJob.cancel(true);waveJob=null;}}
    private void cleanupCache(){File[] files=getCacheDir().listFiles();if(files!=null)for(File f:files)if((f.getName().startsWith("source-")||f.getName().startsWith("cut-"))&&System.currentTimeMillis()-f.lastModified()>86400000L)f.delete();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);if(source!=null){b.putString("file",source.file.getAbsolutePath());b.putString("name",originalName);b.putLong("start",startUs);b.putLong("end",endUs);}if(lastUri!=null){b.putString("last",lastUri.toString());b.putString("lastName",lastName);}}
    @Override protected void onStop(){pause();super.onStop();}
    @Override protected void onDestroy(){dead=true;cancelWave();worker.shutdownNow();waveWorker.shutdownNow();releasePlayer();handler.removeCallbacksAndMessages(null);if(draft!=null)draft.delete();super.onDestroy();}
    private TextView dialogTitle(int id){TextView title=text(getString(id),20,TEXT);title.setPadding(dp(24),dp(20),dp(24),dp(12));return title;}
    private void showDialog(AlertDialog dialog){dialog.show();
        if(!compactControls())return;
        Button positive=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if(positive!=null&&positive.getParent() instanceof LinearLayout){
            LinearLayout panel=(LinearLayout)positive.getParent();panel.setOrientation(LinearLayout.VERTICAL);
            for(int which:new int[]{AlertDialog.BUTTON_NEUTRAL,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_POSITIVE}){
                Button action=dialog.getButton(which);if(action==null)continue;
                action.setSingleLine(false);action.setMaxLines(Integer.MAX_VALUE);action.setMinHeight(dp(48));
                action.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
            }
        }
    }
    private ScrollView scrollable(View content){ScrollView scroll=new ScrollView(this);scroll.addView(content);return scroll;}
    private void hideKeyboard(){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);root.clearFocus();}
    private void error(String m){showDialog(new AlertDialog.Builder(this).setCustomTitle(dialogTitle(R.string.app_name)).setMessage(m).setPositiveButton(getString(R.string.ok),null).create());}
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
    private String message(Exception e){return LocalizedErrors.message(this,e);}
    private void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException(getString(R.string.save_interrupted));out.write(b,0,n);}}
    private int dp(float x){return Math.round(getResources().getDisplayMetrics().density*x);}
    private boolean compactControls(){Configuration c=getResources().getConfiguration();return c.screenWidthDp<360||c.fontScale>1.3f;}
    private LinearLayout controlGroup(){return compactControls()?column():row();}
    private void addControl(LinearLayout group,View control){group.addView(control,group.getOrientation()==LinearLayout.VERTICAL?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(shape(CARD,20));return l;}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setFontFeatureSettings("kern");return t;}
    private void addText(LinearLayout parent,String s,int size,int color,int top){TextView t=text(s,size,color);marginTop(t,top);parent.addView(t);}
    private void marginTop(View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);v.setLayoutParams(p);}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",0));b.setTextColor(primary?BG:TEXT);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?ACCENT:0xff29312a));b.setBackground(shape(primary?ACCENT:0xff29312a,14));b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(52));b.setSingleLine(false);b.setMaxLines(Integer.MAX_VALUE);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setStateListAnimator(null);return b;}
    private EditText timeField(String label){EditText e=new EditText(this);e.setMinWidth(0);e.setTextColor(TEXT);e.setTextSize(16);e.setSingleLine(true);e.setTypeface(Typeface.MONOSPACE);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);e.setContentDescription(getString(R.string.time_description,label));e.setSelectAllOnFocus(true);e.setPadding(dp(8),dp(10),dp(8),dp(10));e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);e.setOnEditorActionListener((v,action,event)->{readRange(true);hideKeyboard();return true;});return e;}
}
