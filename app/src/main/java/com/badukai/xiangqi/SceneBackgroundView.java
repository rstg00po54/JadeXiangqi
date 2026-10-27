package com.badukai.xiangqi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.io.IOException;
import java.io.InputStream;

/**
 * Animated ornamental backdrop, kept behind every interactive control.
 * Leaves, firelight and drifting dust are lightweight Canvas primitives rather
 * than a large looping video. The animator is paused when the View is hidden.
 */
public final class SceneBackgroundView extends View {
    private static final String HOME_IMAGE =
        "ui_tearoom_x300pro/backgrounds/tea_room_lobby_background_1260x2800.png";
    private static final String GAME_IMAGE =
        "ui/backgrounds/scene_background_1080x1920.png";
    private static Bitmap homeImage,gameImage;
    private final boolean home;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF();
    private final Rect source=new Rect();
    private Bitmap image;
    private ValueAnimator animator;
    private boolean motionEnabled=true;
    private float time=0f;
    private long lastFrame=0;

    public SceneBackgroundView(Context context,boolean home){
        super(context);
        this.home=home;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        loadImage();
    }
    private static Bitmap decode(Context context,String path){
        try(InputStream in=context.getAssets().open(path)){
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.RGB_565;
            return BitmapFactory.decodeStream(in,null,options);
        }catch(IOException error){return null;}
    }
    private void loadImage(){
        synchronized(SceneBackgroundView.class){
            if(home){
                if(homeImage==null)homeImage=decode(getContext(),HOME_IMAGE);
                image=homeImage;
            }else{
                if(gameImage==null)gameImage=decode(getContext(),GAME_IMAGE);
                image=gameImage;
            }
        }
    }
    public void setMotionEnabled(boolean enabled){
        motionEnabled=enabled;
        if(enabled)startAnimation();
        else stopAnimation();
        invalidate();
    }
    private void startAnimation(){
        if(!motionEnabled||getWindowVisibility()!=VISIBLE||!isAttachedToWindow()||animator!=null)return;
        lastFrame=0;
        animator=ValueAnimator.ofFloat(0f,1f);
        animator.setDuration(24000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value->{
            long now=SystemClock.uptimeMillis();
            if(now-lastFrame>=32L){
                lastFrame=now;
                time=(float)value.getAnimatedValue();
                postInvalidateOnAnimation();
            }
        });
        animator.start();
    }
    private void stopAnimation(){
        if(animator!=null){animator.cancel();animator=null;}
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        startAnimation();
    }
    @Override protected void onDetachedFromWindow(){
        stopAnimation();
        super.onDetachedFromWindow();
    }
    @Override protected void onWindowVisibilityChanged(int visibility){
        super.onWindowVisibilityChanged(visibility);
        if(visibility==VISIBLE)startAnimation();else stopAnimation();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();
        if(w==0||h==0)return;
        if(image!=null&&!image.isRecycled()){
            // Center crop: do not stretch the 1260×2800 design on other phones.
            float scale=Math.max(w/image.getWidth(),h/image.getHeight());
            float cropW=w/scale,cropH=h/scale;
            float drift=motionEnabled?(float)Math.sin(time*Math.PI*2f)*Math.min(4f,cropW*.004f):0f;
            float left=Math.max(0f,Math.min(image.getWidth()-cropW,(image.getWidth()-cropW)*.5f+drift));
            float top=(image.getHeight()-cropH)*.5f;
            source.set(Math.round(left),Math.round(top),
                    Math.round(left+cropW),Math.round(top+cropH));
            destination.set(0f,0f,w,h);
            paint.setShader(null);paint.setColor(Color.WHITE);paint.setAlpha(255);
            canvas.drawBitmap(image,source,destination,paint);
        }else{
            canvas.drawColor(home?Color.rgb(126,100,65):Color.rgb(14,58,44));
        }
        float cycle=time*2f*(float)Math.PI;
        // Warm lantern/torch glows pulse gently, remaining behind the UI.
        if(!home){
            glow(canvas,w*.19f,h*.075f,w*.078f,cycle,0.1f);
            glow(canvas,w*.80f,h*.075f,w*.078f,cycle,1.8f);
            glow(canvas,w*.09f,h*.94f,w*.05f,cycle,2.7f);
        }else{
            glow(canvas,w*.16f,h*.25f,w*.055f,cycle,0.7f);
            glow(canvas,w*.84f,h*.87f,w*.050f,cycle,2.1f);
        }
        // 14 slow-floating dust motes (slightly brighter near the lanterns).
        for(int i=0;i<14;i++){
            float phase=(i*0.618034f+time*(0.24f+(i%3)*.07f))%1f;
            float x=((i*0.371f+(float)Math.sin(cycle+i*2.6f)*.022f)%1f+1f)%1f*w;
            float y=(1f-phase)*h;
            float radius=w*(.0018f+(i%4)*.0008f);
            float shimmer=.6f+.4f*(float)Math.sin(cycle*2+i);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int)(40+42*shimmer),255,222,141));
            canvas.drawCircle(x,y,radius*2.2f,paint);
            paint.setColor(Color.argb((int)(65+65*shimmer),255,235,170));
            canvas.drawCircle(x,y,radius,paint);
        }
        // Leaf silhouettes float down in front of the painted scenery, not the controls.
        for(int i=0;i<9;i++){
            float drop=(time*(.65f+(i%3)*.15f)+i*.173f)%1f;
            float x=(.08f+(i*.294f)% .86f)*w+
                    (float)Math.sin(cycle*1.3f+i*1.7f)*w*.028f;
            float y=(drop*1.15f-.08f)*h;
            float rx=w*(.008f+(i%3)*.002f),ry=rx*.48f;
            canvas.save();
            canvas.rotate((float)Math.sin(cycle+i)*38f+i*19f,x,y);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(65+(i%4)*18,
                    home?119:109,home?154:153,home?68:91));
            canvas.drawOval(x-rx,y-ry,x+rx,y+ry,paint);
            canvas.restore();
        }
    }
    private void glow(Canvas canvas,float cx,float cy,float radius,float cycle,float offset){
        float pulsing=.65f+.35f*(float)Math.sin(cycle*2.2f+offset);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int)(13*pulsing),255,180,60));
        canvas.drawCircle(cx,cy,radius*2.3f,paint);
        paint.setColor(Color.argb((int)(23*pulsing),255,199,86));
        canvas.drawCircle(cx,cy,radius*1.25f,paint);
        paint.setColor(Color.argb((int)(37*pulsing),255,221,139));
        canvas.drawCircle(cx,cy,radius*.52f,paint);
    }
}
