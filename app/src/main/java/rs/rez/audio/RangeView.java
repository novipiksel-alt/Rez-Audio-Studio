// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;

final class RangeView extends View {
    interface Listener {void changed(long start,long end);}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private long duration=1,start=0,end=1,playhead=-1;
    private float[] peaks=new float[160];
    private int drag;
    private long viewportStart=0, viewportEnd=1;
    private float touchX;
    private long panStart;
    private boolean panning;
    private float pinchDistance;
    private long pinchCenter;
    private static final long MIN_WINDOW_US=250_000L;
    Listener listener;
    RangeView(Context c) {super(c);setFocusable(true);setContentDescription(c.getString(R.string.range_description));}
    void setAudio(long d){duration=Math.max(1,d);viewportStart=0;viewportEnd=duration;start=0;end=Math.min(d,30_000_000L);playhead=-1;peaks=new float[160];invalidate();}
    void setRange(long s,long e){start=s;end=e;invalidate();}
    void setPeaks(float[] v){peaks=v;invalidate();}
    void setPlayhead(long v){playhead=v;invalidate();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private float x(long t){return dp(20)+(getWidth()-dp(40))*(t-viewportStart)/(float)Math.max(1,viewportEnd-viewportStart);}
    private long timeAt(float px){float f=Math.max(0f,Math.min(1f,(px-dp(20))/Math.max(1f,getWidth()-dp(40))));return viewportStart+(long)(f*(viewportEnd-viewportStart));}
    int zoomPercent(){return (int)Math.round(100.0*duration/Math.max(1,viewportEnd-viewportStart));}
    void zoom(float factor){zoomAround(factor,(viewportStart+viewportEnd)/2);}
    private void zoomAround(float factor,long center){
        long old=Math.max(1,viewportEnd-viewportStart);
        long window=Math.max(Math.min(duration,MIN_WINDOW_US),Math.min(duration,Math.round(old/factor)));
        double ratio=(center-viewportStart)/(double)old;
        long next=center-Math.round(window*ratio);
        viewportStart=Math.max(0,Math.min(duration-window,next));viewportEnd=viewportStart+window;invalidate();
    }
    void resetZoom(){viewportStart=0;viewportEnd=duration;invalidate();}
    void pan(long delta){long span=viewportEnd-viewportStart;viewportStart=Math.max(0,Math.min(duration-span,viewportStart+delta));viewportEnd=viewportStart+span;invalidate();}
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);float top=dp(24),bottom=getHeight()-dp(38),mid=(top+bottom)/2;float a=x(start),b=x(end);
        c.save();c.clipRect(dp(19),top-dp(8),getWidth()-dp(19),bottom+dp(8));
        p.setColor(Color.rgb(32,42,34));c.drawRoundRect(a,top,b,bottom,dp(8),dp(8),p);
        float span=getWidth()-dp(40),step=span/peaks.length;
        for(int i=0;i<peaks.length;i++){
            float xx=x((long)((i+.5)*duration/peaks.length));if(xx<dp(18)||xx>getWidth()-dp(18))continue;float h=dp(3)+peaks[i]*(bottom-top)*.43f;
            p.setColor(xx>=a&&xx<=b?0xffbcf776:0xff465149);p.setStrokeWidth(Math.max(dp(1.5f),step*.55f));p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(xx,mid-h,xx,mid+h,p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);p.setColor(0xffbcf776);p.setStrokeWidth(dp(2));c.drawLine(a,top,a,bottom,p);c.drawLine(b,top,b,bottom,p);
        c.drawRoundRect(a-dp(9),mid-dp(20),a+dp(9),mid+dp(20),dp(7),dp(7),p);c.drawRoundRect(b-dp(9),mid-dp(20),b+dp(9),mid+dp(20),dp(7),dp(7),p);
        p.setColor(0xff24351b);p.setStrokeWidth(dp(2));c.drawLine(a,mid-dp(7),a,mid+dp(7),p);c.drawLine(b,mid-dp(7),b,mid+dp(7),p);
        if(playhead>=0){p.setColor(0xfff2f6ee);p.setStrokeWidth(dp(2));c.drawLine(x(playhead),top-dp(5),x(playhead),bottom+dp(5),p);}
        c.restore();p.setColor(0xff929d92);p.setTextSize(dp(11));p.setTypeface(Typeface.MONOSPACE);p.setTextAlign(Paint.Align.LEFT);c.drawText(TimeRange.format(viewportStart),dp(15),getHeight()-dp(10),p);
        p.setTextAlign(Paint.Align.RIGHT);c.drawText(TimeRange.format(viewportEnd),getWidth()-dp(15),getHeight()-dp(10),p);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return false;
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_POINTER_DOWN&&e.getPointerCount()>=2){
            pinchDistance=Math.abs(e.getX(0)-e.getX(1));pinchCenter=timeAt((e.getX(0)+e.getX(1))/2f);drag=0;return true;
        }
        if(action==MotionEvent.ACTION_DOWN){
            touchX=e.getX();panStart=viewportStart;panning=false;
            float distanceStart=Math.abs(touchX-x(start)),distanceEnd=Math.abs(touchX-x(end));
            float threshold=dp(28);
            drag=Math.min(distanceStart,distanceEnd)<=threshold?(distanceStart<=distanceEnd?1:2):3;
            getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(action==MotionEvent.ACTION_MOVE){
            if(e.getPointerCount()>=2){
                float distance=Math.abs(e.getX(0)-e.getX(1));
                if(pinchDistance>dp(10)&&distance>dp(10))zoomAround(distance/pinchDistance,pinchCenter);
                pinchDistance=distance;pinchCenter=timeAt((e.getX(0)+e.getX(1))/2f);return true;
            }
            if(drag==3){
                long span=viewportEnd-viewportStart;
                long delta=(long)((touchX-e.getX())*span/Math.max(1f,getWidth()-dp(40)));
                long next=Math.max(0,Math.min(duration-span,panStart+delta));
                viewportStart=next;viewportEnd=next+span;panning=true;invalidate();return true;
            }
            if(drag==1||drag==2){
                long value=timeAt(e.getX()),minimum=Math.min(100_000,duration);
                if(drag==1)start=Math.max(0,Math.min(value,end-minimum));
                else end=Math.min(duration,Math.max(value,start+minimum));
                invalidate();if(listener!=null)listener.changed(start,end);return true;
            }
        }
        if(action==MotionEvent.ACTION_POINTER_UP){pinchDistance=0;return true;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            drag=0;pinchDistance=0;getParent().requestDisallowInterceptTouchEvent(false);
            if(action==MotionEvent.ACTION_UP&&!panning)performClick();return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
