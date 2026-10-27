package com.badukai.xiangqi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.List;

/** Responsive jade/ivory Chinese-chess board, implemented without bitmap resources. */
public final class BoardView extends View {
    public interface OnSquareClick { void onSquare(int square); }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private XiangqiGame game;
    private OnSquareClick listener;
    private int selected=-1, hintFrom=-1, hintTo=-1;
    private boolean allowMoves=true;
    private boolean flipped=false;
    private ValueAnimator animator;
    private XiangqiGame.Move moving;
    private boolean reversing=false;
    private float moveProgress=0f;
    private float x0,y0,spacing,sx,sy;
    private static boolean assetsLoaded=false;
    private static Bitmap boardTexture,selectionTexture;
    private static final Bitmap[] redTextures=new Bitmap[8],blackTextures=new Bitmap[8];
    private static final String ASSET_ROOT="";
    private static Bitmap bitmap(Context context,String name){
        try(InputStream input=context.getAssets().open(ASSET_ROOT+name)){
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inScaled=false;
            return BitmapFactory.decodeStream(input,null,options);
        }catch(IOException error){return null;}
    }
    private static synchronized void loadTextures(Context context){
        if(assetsLoaded)return;
        boardTexture=bitmap(context,"ui_tearoom_x300pro/boards/board_warm_stone_1024x1190.png");
        selectionTexture=bitmap(context,"ui_tearoom_x300pro/decorations/selection_ring_256.png");
        String[] names={"","general","advisor","elephant","horse","rook","cannon","soldier"};
        for(int type=1;type<=7;type++){
            redTextures[type]=bitmap(context,"xiangqi_pieces/red_"+names[type]+"_256.png");
            blackTextures[type]=bitmap(context,"xiangqi_pieces/black_"+names[type]+"_256.png");
        }
        assetsLoaded=true;
    }
    private static final int FOREST=Color.rgb(24,67,55);
    private static final int GOLD=Color.rgb(186,145,77);
    private static final int PAPER=Color.rgb(238,222,179);
    public BoardView(Context context) {super(context);loadTextures(context);}
    public void bind(XiangqiGame game) {this.game=game;invalidate();}
    public void onSquareClick(OnSquareClick cb){this.listener=cb;}
    public void setSelected(int sq) {this.selected=sq;invalidate();}
    public void setHint(int from,int to) {hintFrom=from;hintTo=to;invalidate();}
    public void setAllowMoves(boolean allow){allowMoves=allow;invalidate();}
    public void setFlipped(boolean flip){flipped=flip;invalidate();}

    /** Must be called after game position has been changed to the target position. */
    public void animateMove(XiangqiGame.Move move,boolean undo,Runnable onFinished){
        cancelMoveAnimation();
        if(move==null||game==null){if(onFinished!=null)onFinished.run();return;}
        moving=move;reversing=undo;moveProgress=0f;
        setAllowMoves(false);
        ValueAnimator current=ValueAnimator.ofFloat(0f,1f);
        animator=current;
        int distance=Math.abs(move.to/9-move.from/9)+Math.abs(move.to%9-move.from%9);
        current.setDuration(Math.min(340,190+distance*20));
        current.setInterpolator(new DecelerateInterpolator(1.5f));
        current.addUpdateListener(a->{
            if(animator==current){moveProgress=(float)a.getAnimatedValue();invalidate();}
        });
        current.addListener(new AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(Animator animation){
                if(animator!=current)return;
                animator=null;moving=null;moveProgress=0f;invalidate();
                if(onFinished!=null)onFinished.run();
            }
        });
        current.start();
    }

    public void cancelMoveAnimation(){
        ValueAnimator previous=animator;
        animator=null;moving=null;moveProgress=0f;
        if(previous!=null)previous.cancel();
        invalidate();
    }
    @Override protected void onDetachedFromWindow(){
        cancelMoveAnimation();
        super.onDetachedFromWindow();
    }
    private int displayRow(int sq){return flipped?9-sq/9:sq/9;}
    private int displayCol(int sq){return flipped?8-sq%9:sq%9;}
    private float px(int sq){return x(displayCol(sq));}
    private float py(int sq){return y(displayRow(sq));}
    public void clearMarkers(){selected=-1;hintFrom=-1;hintTo=-1;invalidate();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w,Math.round(w*1190f/1024f));
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        // Texture comes from a measured 1024×1190 board:
        // file lines: x=102..918, y=87..1078, giving exactly 9×10 crossings.
        x0=w*102f/1024f;y0=h*87f/1190f;
        sx=w*102f/1024f;sy=h*991f/(1190f*9f);
        spacing=Math.min(sx,sy);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setAlpha(255);
        p.setFilterBitmap(true);
        if(boardTexture!=null){
            c.drawBitmap(boardTexture,null,new RectF(0,0,w,h),p);
        }else{
            p.setColor(PAPER);
            c.drawRoundRect(new RectF(0,0,w,h),w*.035f,w*.035f,p);
            p.setColor(FOREST);p.setStrokeWidth(Math.max(1f,w*.0027f));
            p.setStyle(Paint.Style.STROKE);
            for(int r=0;r<10;r++)c.drawLine(x(0),y(r),x(8),y(r),p);
            for(int col=0;col<9;col++){
                if(col==0||col==8)c.drawLine(x(col),y(0),x(col),y(9),p);
                else {c.drawLine(x(col),y(0),x(col),y(4),p);c.drawLine(x(col),y(5),x(col),y(9),p);}
            }
            c.drawLine(x(3),y(0),x(5),y(2),p);c.drawLine(x(5),y(0),x(3),y(2),p);
            c.drawLine(x(3),y(7),x(5),y(9),p);c.drawLine(x(5),y(7),x(3),y(9),p);
            p.setStyle(Paint.Style.FILL);
        }
        if(game==null)return;
        if(!game.history.isEmpty()) {
            XiangqiGame.Move last=game.history.get(game.history.size()-1);
            highlight(c,last.from,Color.argb(110,42,125,102));
            highlight(c,last.to,Color.argb(145,42,125,102));
        }
        if(selected>=0){
            highlight(c,selected,Color.argb(90,244,173,58));
            if(selectionTexture!=null){
                float size=spacing*1.15f;
                p.setColor(Color.WHITE);p.setAlpha(225);
                c.drawBitmap(selectionTexture,null,new RectF(px(selected)-size*.5f,
                    py(selected)-size*.5f,px(selected)+size*.5f,py(selected)+size*.5f),p);
                p.setAlpha(255);
            }
        }
        if(hintFrom>=0)highlight(c,hintFrom,Color.argb(130,64,158,215));
        if(hintTo>=0)highlight(c,hintTo,Color.argb(175,64,158,215));
        if(selected>=0&&allowMoves) {
            List<XiangqiGame.Move> options=game.legalMoves();
            for(XiangqiGame.Move m:options)if(m.from==selected) {
                p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(170,40,150,97));
                c.drawCircle(px(m.to),py(m.to),spacing*.13f,p);
            }
        }
        for(int sq=0;sq<90;sq++){
            if(game.board[sq]==0)continue;
            if(moving!=null&&sq==(reversing?moving.from:moving.to))continue;
            if(moving!=null&&reversing&&sq==moving.to&&moving.captured!=0)continue;
            drawPiece(c,sq,game.board[sq]);
        }
        if(moving!=null){
            if(moving.captured!=0){
                int alpha=Math.round(255f*(reversing?moveProgress:1f-moveProgress));
                if(alpha>0)drawPieceAt(c,moving.captured,px(moving.to),py(moving.to),alpha);
            }
            int from=reversing?moving.to:moving.from;
            int to=reversing?moving.from:moving.to;
            float cx=px(from)+(px(to)-px(from))*moveProgress;
            float cy=py(from)+(py(to)-py(from))*moveProgress;
            drawPieceAt(c,moving.piece,cx,cy,255);
        }
    }
    private float x(int c){return x0+sx*c;}
    private float y(int r){return y0+sy*r;}
    private void highlight(Canvas c,int sq,int color){
        p.setStyle(Paint.Style.FILL);p.setColor(color);
        c.drawCircle(px(sq),py(sq),spacing*.43f,p);
    }
    private void drawPiece(Canvas c,int sq,int piece){
        drawPieceAt(c,piece,px(sq),py(sq),255);
    }
    private void drawPieceAt(Canvas c,int piece,float cx,float cy,int alpha){
        Bitmap texture=null;
        int type=Math.abs(piece);
        if(type>=1&&type<=7)texture=(piece>0?redTextures:blackTextures)[type];
        if(texture!=null){
            float size=spacing*1.14f;
            p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
            p.setAlpha(alpha);p.setFilterBitmap(true);
            c.drawBitmap(texture,null,new RectF(cx-size*.5f,cy-size*.5f,cx+size*.5f,cy+size*.5f),p);
            p.setAlpha(255);
            return;
        }
        float radius=spacing*.405f;
        int save=-1;
        if(alpha<255)save=c.saveLayerAlpha(cx-radius-spacing*.12f,cy-radius-spacing*.12f,
                cx+radius+spacing*.12f,cy+radius+spacing*.12f,alpha);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(90,12,42,32));
        c.drawCircle(cx+spacing*.034f,cy+spacing*.06f,radius,p);
        p.setColor(Color.rgb(247,237,207));c.drawCircle(cx,cy,radius,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(GOLD);p.setStrokeWidth(spacing*.053f);
        c.drawCircle(cx,cy,radius-spacing*.035f,p);
        p.setColor(Color.rgb(186,164,126));p.setStrokeWidth(spacing*.013f);
        c.drawCircle(cx,cy,radius-spacing*.11f,p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));
        p.setTextSize(spacing*.56f);p.setColor(piece>0?Color.rgb(170,49,47):FOREST);
        Paint.FontMetrics fm=p.getFontMetrics();
        c.drawText(XiangqiGame.pieceName(piece),cx,cy-(fm.ascent+fm.descent)/2,p);
        if(save!=-1)c.restoreToCount(save);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP)return true;
        if(!allowMoves||game==null||listener==null||moving!=null)return true;
        int col=Math.round((event.getX()-x0)/spacing),row=Math.round((event.getY()-y0)/spacing);
        if(col>=0&&col<9&&row>=0&&row<10) {
            float dx=event.getX()-x(col),dy=event.getY()-y(row);
            if(Math.hypot(dx,dy)<spacing*.56f)listener.onSquare((flipped?9-row:row)*9+(flipped?8-col:col));
        }
        performClick();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
