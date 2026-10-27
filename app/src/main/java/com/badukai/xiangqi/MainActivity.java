package com.badukai.xiangqi;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Locale;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.FrameLayout;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/** First native-Android edition: jade UI, valid Xiangqi, CPU AI and spectator mode. */
public final class MainActivity extends Activity {
    private static final int JADE=Color.rgb(19,79,65), DARK=Color.rgb(16,59,49);
    private static final int GOLD=Color.rgb(200,157,84), BG=Color.rgb(243,246,238);
    private static final int MODE_HUMAN_AI=0, MODE_AI_AI=1, MODE_TWO_HUMANS=2;
    private static final int HOME=0, GAME=1, REPLAY=2, PUZZLE=3;
    private static final String PREFS="xiangqi_v1";
    private int screen=HOME,mode=MODE_HUMAN_AI,humanSide=XiangqiGame.RED,level=2,serial=0;
    private int selected=-1,hintFrom=-1,hintTo=-1,replayIndex=0,puzzleIndex=0,puzzleSelected=-1;
    private boolean thinking=false,hintThinking=false,fastAi=false,autoPlaying=false,animating=false,sceneMotion=true;
    private int timeControlMinutes=10,timedWinner=0;
    private long redRemainingMs=600000,blackRemainingMs=600000,clockStamp;
    private boolean foreground=true;
    private TextView redTimeView,blackTimeView;
    private final Runnable timerTick=new Runnable(){
        @Override public void run(){
            updateClocks();
            handler.postDelayed(this,250);
        }
    };
    private XiangqiGame game=new XiangqiGame();
    private FrameLayout root;
    private SceneBackgroundView sceneView;
    private Bitmap woodenTexture;
    private BoardView boardView;
    private BoardView replayBoard,puzzleBoard;
    private XiangqiGame puzzleGame;
    private boolean puzzlePassed=false;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        sceneMotion=getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("sceneMotion",true);
        load();home();
        clockStamp=SystemClock.elapsedRealtime();
        handler.postDelayed(timerTick,250);
    }
    @Override protected void onPause(){updateClocks();foreground=false;persist();super.onPause();}
    @Override protected void onResume(){super.onResume();foreground=true;clockStamp=SystemClock.elapsedRealtime();}
    @Override protected void onDestroy(){handler.removeCallbacks(timerTick);super.onDestroy();}
    private int dp(float n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private GradientDrawable bg(int color,int radius,int border) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(border!=0)d.setStroke(dp(1),border);return d;
    }
    private GradientDrawable gradient(int a,int b,int radius) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
        d.setCornerRadius(dp(radius));return d;
    }
    private TextView label(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);
        t.setFontFeatureSettings("kern");
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void gap(LinearLayout l,int height){View spacer=new View(this);l.addView(spacer,new LinearLayout.LayoutParams(1,dp(height)));}
    private void add(LinearLayout l,View v,int mBottom){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin=dp(mBottom);l.addView(v,p);
    }
    private TextView action(String title,int color,boolean light,Runnable run) {
        TextView t=label(title,14,Color.rgb(250,231,183),true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(6),dp(12),dp(6),dp(12));
        t.setBackground(bg(light?Color.argb(238,23,79,62):Color.argb(232,25,59,49),13,GOLD));
        t.setOnClickListener(v->run.run());
        return t;
    }
    private android.graphics.drawable.Drawable woodButton(){
        if(woodenTexture==null){
            try(InputStream in=getAssets().open(
                    "ui_tearoom_x300pro/buttons/button_blank_600x196.png")){
                woodenTexture=BitmapFactory.decodeStream(in);
            }catch(IOException ignored){}
        }
        if(woodenTexture!=null){
            BitmapDrawable d=new BitmapDrawable(getResources(),woodenTexture);
            d.setGravity(Gravity.FILL);
            return d;
        }
        return bg(Color.rgb(226,177,107),18,GOLD);
    }
    private void addAction(LinearLayout parent,String title,int color,boolean light,Runnable run){
        TextView a=action(title,color,light,run);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);
        p.setMargins(dp(3),dp(3),dp(3),dp(3));parent.addView(a,p);
    }
    private void shell(String title,String subtitle){
        // Background and touchable controls are separate layers.
        root=new FrameLayout(this);
        sceneView=new SceneBackgroundView(this,screen==HOME);
        sceneView.setMotionEnabled(sceneMotion);
        root.addView(sceneView,new FrameLayout.LayoutParams(-1,-1));
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);
        sc.setClipToPadding(false);
        sc.setBackgroundColor(Color.TRANSPARENT);
        sc.setPadding(dp(10),dp(4),dp(10),dp(6));
        root.addView(sc,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        LinearLayout body=col();
        body.setPadding(dp(2),dp(4),dp(2),dp(12));
        sc.addView(body);
        LinearLayout heading=col();
        heading.setPadding(dp(14),dp(11),dp(14),dp(10));
        heading.setBackground(bg(Color.argb(215,16,57,43),14,GOLD));
        add(heading,label(title,20,Color.rgb(251,223,168),true),1);
        add(heading,label(subtitle,11,Color.rgb(229,235,207),false),0);
        add(body,heading,7);
        content=body;
    }
    private LinearLayout content;
    private void block(String title,String desc,Runnable onClick){
        LinearLayout card=col();
        card.setPadding(dp(20),dp(13),dp(18),dp(14));
        card.setBackground(woodButton());
        add(card,label(title,19,Color.rgb(95,53,27),true),0);
        add(card,label(desc,11,Color.rgb(109,69,39),false),0);
        card.setOnClickListener(v->onClick.run());
        add(content,card,5);
    }
    private void cancelAnimations(){
        if(boardView!=null)boardView.cancelMoveAnimation();
        if(replayBoard!=null)replayBoard.cancelMoveAnimation();
        if(puzzleBoard!=null)puzzleBoard.cancelMoveAnimation();
        animating=false;
    }
    private void home(){
        ++serial;cancelAnimations();
        clockStamp=SystemClock.elapsedRealtime();screen=HOME;thinking=false;hintThinking=false;
        shell("玉弈象棋","竹窗茶室 · 人机对弈 · 棋谱复盘");
        // Leave part of the round garden window visible above the interactive entries.
        gap(content,110);
        LinearLayout banner=col();
        banner.setPadding(dp(18),dp(20),dp(18),dp(20));
        banner.setBackground(bg(Color.argb(215,20,70,53),18,GOLD));
        add(banner,label("♜  今日来一局",24,Color.rgb(246,225,171),true),6);
        add(banner,label("经典象棋 · 人机挑战 · 棋谱复盘",13,Color.WHITE,false),0);
        add(content,banner,12);
        add(content,label("对局时限",16,Color.rgb(254,232,175),true),5);
        LinearLayout timeRow=row();
        for(final int minutes:new int[]{5,10,15}) {
            addAction(timeRow,minutes+" 分钟",
                minutes==timeControlMinutes?JADE:Color.WHITE,
                minutes==timeControlMinutes,()->{timeControlMinutes=minutes;home();});
        }
        add(content,timeRow,10);
        add(content,label("开始对弈",18,Color.rgb(254,232,175),true),9);
        block("♟  快速人机 · 执红","红方先手 · "+timeControlMinutes+"分钟场 · 三档难度",()->newGame(MODE_HUMAN_AI,XiangqiGame.RED));
        block("♟  快速人机 · 执黑","黑方后手 · 棋盘自动翻转 · AI 先行",()->newGame(MODE_HUMAN_AI,XiangqiGame.BLACK));
        block("♜  双 AI 观战","自动走棋 · 调整速度 · 支持暂停",()->newGame(MODE_AI_AI,XiangqiGame.RED));
        block("♞  双人同屏","本地两人对弈 · "+timeControlMinutes+"分钟场",()->newGame(MODE_TWO_HUMANS,XiangqiGame.RED));
        block("♙  残局入门 · 一步将军","四个原创入门小关卡 · 车、炮、马、兵",()->startPuzzle(0));
        SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if(prefs.contains("moves")&&(!prefs.getString("moves","").isEmpty())) {
            block("↺  继续上次对局","自动保存了走子记录，可以接着下",()->{load();autoPlaying=mode==MODE_AI_AI;screen=GAME;renderGame();scheduleAi();});
        }
        add(content,label("单机版 · 四关入门训练 · Java CPU AI",12,Color.rgb(245,221,173),false),12);
    }
    private void newGame(int newMode,int newHumanSide){
        ++serial;cancelAnimations();game=new XiangqiGame();selected=-1;hintFrom=-1;hintTo=-1;
        mode=newMode;humanSide=newHumanSide;thinking=false;hintThinking=false;autoPlaying=newMode==MODE_AI_AI;
        timedWinner=0;redRemainingMs=blackRemainingMs=timeControlMinutes*60000L;clockStamp=SystemClock.elapsedRealtime();
        screen=GAME;persist();renderGame();scheduleAi();
    }
    private void persist(){
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("moves",game.exportMoves())
                .putInt("mode",mode).putInt("humanSide",humanSide).putInt("level",level)
                .putInt("clockMin",timeControlMinutes).putInt("timedWinner",timedWinner)
                .putLong("redTime",redRemainingMs).putLong("blackTime",blackRemainingMs).apply();
    }
    private void load(){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        mode=p.getInt("mode",MODE_HUMAN_AI);
        humanSide=p.getInt("humanSide",XiangqiGame.RED);
        level=p.getInt("level",2);
        timeControlMinutes=p.getInt("clockMin",10);
        redRemainingMs=p.getLong("redTime",timeControlMinutes*60000L);
        blackRemainingMs=p.getLong("blackTime",timeControlMinutes*60000L);
        timedWinner=p.getInt("timedWinner",0);
        game=new XiangqiGame();game.importMoves(p.getString("moves",""));
        clockStamp=SystemClock.elapsedRealtime();
    }
    private boolean isAiTurn(){return mode==MODE_AI_AI||(mode==MODE_HUMAN_AI&&game.turn!=humanSide);}
    private boolean isGameFinished(){return timedWinner!=0||game.isOver();}
    private String clockText(long timeMs){
        long seconds=Math.max(0,(timeMs+999)/1000);
        return String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60);
    }
    private void showClock(){
        if(redTimeView!=null)redTimeView.setText("红方  "+clockText(redRemainingMs));
        if(blackTimeView!=null)blackTimeView.setText("黑方  "+clockText(blackRemainingMs));
    }
    private void updateClocks(){
        long now=SystemClock.elapsedRealtime();
        long delta=Math.max(0,now-clockStamp);
        clockStamp=now;
        if(!foreground||screen!=GAME||animating||mode==MODE_AI_AI||timedWinner!=0||game.isOver())return;
        if(game.turn==XiangqiGame.RED)redRemainingMs=Math.max(0,redRemainingMs-delta);
        else blackRemainingMs=Math.max(0,blackRemainingMs-delta);
        showClock();
        if(redRemainingMs==0||blackRemainingMs==0){
            timedWinner=redRemainingMs==0?XiangqiGame.BLACK:XiangqiGame.RED;
            ++serial;thinking=false;persist();renderGame();
        }
    }
    private void renderGame(){
        screen=GAME;
        String modeText=mode==MODE_AI_AI?"AI × AI 自动对弈":mode==MODE_TWO_HUMANS?"双人同屏":(humanSide==XiangqiGame.RED?"你执红方":"你执黑方");
        shell("玉弈 · 对弈",modeText+"  |  内置 CPU AI");
        LinearLayout info=row();info.setPadding(dp(13),dp(12),dp(13),dp(12));
        info.setBackground(bg(Color.argb(219,18,63,48),15,GOLD));
        String turn=game.turn==XiangqiGame.RED?"红方":"黑方";
        String text=isGameFinished()?(( (timedWinner!=0?timedWinner:game.winner())==XiangqiGame.RED?"红方":"黑方")+"获胜"+(timedWinner!=0?" · 超时":"")):
            (turn+"行棋"+(game.inCheck(game.turn)?" · 将军！":"")+(thinking?" · AI 思考中…":""));
        TextView status=label(text,16,Color.rgb(245,225,177),true);info.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(label("第 "+(game.history.size()/2+1)+" 回合",12,Color.rgb(242,219,176),false));
        add(content,info,7);
        LinearLayout players=row();
        players.setPadding(dp(10),dp(11),dp(10),dp(11));
        players.setBackground(bg(Color.argb(211,16,64,49),14,GOLD));
        redTimeView=label("",16,Color.rgb(251,191,145),true);
        blackTimeView=label("",16,Color.rgb(243,224,167),true);
        redTimeView.setGravity(Gravity.CENTER);
        blackTimeView.setGravity(Gravity.CENTER);
        players.addView(redTimeView,new LinearLayout.LayoutParams(0,dp(32),1));
        players.addView(blackTimeView,new LinearLayout.LayoutParams(0,dp(32),1));
        showClock();
        add(content,players,10);
        boardView=new BoardView(this);boardView.bind(game);
        boardView.setFlipped(mode==MODE_HUMAN_AI&&humanSide==XiangqiGame.BLACK);
        boardView.setSelected(selected);
        boardView.setHint(hintFrom,hintTo);
        boardView.setAllowMoves(!thinking&&!animating&&!isGameFinished()&&mode!=MODE_AI_AI&&(!isAiTurn()));
        boardView.onSquareClick(this::squareTapped);
        add(content,boardView,10);
        LinearLayout row1=row();
        addAction(row1,"悔棋",Color.WHITE,false,this::undo);
        addAction(row1,"AI 提示",Color.WHITE,false,this::hint);
        addAction(row1,"棋谱复盘",Color.WHITE,false,this::replay);
        add(content,row1,4);
        LinearLayout row2=row();
        addAction(row2,sceneMotion?"动效：开":"动效：关",JADE,true,()->{
            sceneMotion=!sceneMotion;
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("sceneMotion",sceneMotion).apply();
            if(sceneView!=null)sceneView.setMotionEnabled(sceneMotion);
            renderGame();
        });
        addAction(row2,"棋力："+(level==1?"入门":level==2?"普通":"较强"),Color.WHITE,false,()->{
            if(animating)return;
            level=level==3?1:level+1;persist();renderGame();
        });
        addAction(row2,"回首页",Color.rgb(219,229,219),false,this::home);
        add(content,row2,4);
        if(mode==MODE_AI_AI){
            LinearLayout auto=row();
            addAction(auto,autoPlaying?"暂停观战":"继续观战",GOLD,true,()->{
                if(animating){autoPlaying=false;return;}
                if(autoPlaying) {autoPlaying=false;++serial;thinking=false;renderGame();}
                else {autoPlaying=true;renderGame();scheduleAi();}
            });
            addAction(auto,fastAi?"速度：快":"速度：慢",Color.WHITE,false,()->{fastAi=!fastAi;if(!animating)renderGame();});
            add(content,auto,4);
        }
        String note=hintThinking?"AI 正在分析…":hintFrom>=0?"提示：蓝色标记为建议走法。":"点己方棋子，再点目标位置即可落子。绿点表示合法位置。";
        add(content,label(note,12,Color.rgb(249,224,166),false),8);
    }
    private void squareTapped(int sq){
        if(animating||thinking||isGameFinished()||isAiTurn()||mode==MODE_AI_AI)return;
        if(selected<0){
            if(XiangqiGame.color(game.board[sq])==game.turn){selected=sq;boardView.setSelected(sq);}
            return;
        }
        if(selected==sq){selected=-1;boardView.setSelected(-1);return;}
        if(XiangqiGame.color(game.board[sq])==game.turn){selected=sq;boardView.setSelected(sq);return;}
        XiangqiGame.Move played=game.move(selected,sq);
        if(played==null){toast("这一步不符合象棋规则");return;}
        afterMove(played,false);
    }
    private void afterMove(XiangqiGame.Move played,boolean byAi){
        selected=-1;hintFrom=-1;hintTo=-1;persist();
        if(boardView==null){renderGame();scheduleAi();return;}
        animating=true;
        boardView.setSelected(-1);
        boardView.setHint(-1,-1);
        boardView.setAllowMoves(false);
        final int ticket=serial;
        boardView.animateMove(played,false,()->{
            if(ticket!=serial||screen!=GAME)return;
            animating=false;renderGame();
            if(mode==MODE_AI_AI&&game.history.size()>=300){
                toast("自动对弈已走满300步，暂时停止避免无限循环");return;
            }
            if(isGameFinished())return;
            if(mode==MODE_AI_AI&&byAi&&autoPlaying){
                int currentTicket=serial;
                handler.postDelayed(()->{
                    if(currentTicket==serial&&screen==GAME&&!thinking&&!animating)scheduleAi();
                },fastAi?80:460);
            }else scheduleAi();
        });
    }
    private void undo(){
        if(animating)return;
        ++serial;thinking=false;hintThinking=false;
        if(game.history.isEmpty()){toast("还没有可悔的棋");return;}
        selected=-1;hintFrom=-1;hintTo=-1;
        animating=true;
        boardView.setSelected(-1);
        boardView.setAllowMoves(false);
        undoOne();
    }
    private void undoOne(){
        XiangqiGame.Move last=game.history.get(game.history.size()-1);
        game.unmakeLast();
        int ticket=serial;
        boardView.animateMove(last,true,()->{
            if(ticket!=serial||screen!=GAME)return;
            if(mode==MODE_HUMAN_AI&&game.turn!=humanSide&&!game.history.isEmpty()){
                undoOne();return;
            }
            animating=false;persist();renderGame();
            if(mode==MODE_HUMAN_AI&&game.turn!=humanSide)scheduleAi();
        });
    }
    private void hint(){
        if(animating||hintThinking||thinking||isGameFinished()||isAiTurn()||mode==MODE_AI_AI)return;
        hintThinking=true;renderGame();
        final int ticket=++serial;
        final String moves=game.exportMoves();
        new Thread(()->{
            XiangqiGame search=new XiangqiGame();search.importMoves(moves);
            SimpleAi.Result result=new SimpleAi().best(search,Math.max(2,level),1200);
            runOnUiThread(()->{
                if(ticket!=serial||screen!=GAME)return;
                hintThinking=false;
                if(result.move!=null){hintFrom=result.move.from;hintTo=result.move.to;}
                renderGame();
                if(result.move!=null)toast("推荐："+XiangqiGame.moveText(result.move));
            });
        },"xiangqi-hint").start();
    }
    private void scheduleAi(){
        if(screen!=GAME||thinking||animating||isGameFinished()||!isAiTurn()||game.history.size()>=300||(mode==MODE_AI_AI&&!autoPlaying))return;
        thinking=true;renderGame();
        final int ticket=++serial;
        final String moves=game.exportMoves();
        new Thread(()->{
            XiangqiGame copy=new XiangqiGame();copy.importMoves(moves);
            SimpleAi.Result result=new SimpleAi().best(copy,level,level==3?2300:level==2?1100:400);
            runOnUiThread(()->{
                if(ticket!=serial||screen!=GAME||!thinking)return;
                thinking=false;
                if(result.move!=null){
                    XiangqiGame.Move played=game.move(result.move.from,result.move.to);
                    if(played!=null){
                        afterMove(played,true);
                        return;
                    }
                }
                renderGame();
            });
        },"xiangqi-ai").start();
    }
    private void replay(){if(animating)return;++serial;thinking=false;hintThinking=false;replayIndex=game.history.size();renderReplay();}
    private void renderReplay(){
        screen=REPLAY;
        shell("棋谱 · 复盘","逐手回放本局棋谱 · 红方先行");
        XiangqiGame snapshot=new XiangqiGame();
        for(int i=0;i<replayIndex;i++){
            XiangqiGame.Move m=game.history.get(i);
            snapshot.move(m.from,m.to);
        }
        add(content,label("第 "+replayIndex+" / "+game.history.size()+" 步",17,JADE,true),7);
        replayBoard=new BoardView(this);replayBoard.bind(snapshot);
        replayBoard.setFlipped(mode==MODE_HUMAN_AI&&humanSide==XiangqiGame.BLACK);
        replayBoard.setAllowMoves(false);
        add(content,replayBoard,9);
        LinearLayout controls=row();
        addAction(controls,"◀ 上一步",Color.WHITE,false,()->stepReplay(-1));
        addAction(controls,"下一步 ▶",Color.WHITE,false,()->stepReplay(1));
        add(content,controls,5);
        LinearLayout links=row();
        addAction(links,"返回对弈",JADE,true,()->{renderGame();scheduleAi();});
        addAction(links,"复制棋谱",Color.WHITE,false,this::copyMoves);
        add(content,links,10);
        add(content,label("本局走棋记录（简明坐标记谱）",14,DARK,true),7);
        if(game.history.isEmpty())add(content,label("暂无棋谱，先下一步吧。",13,Color.GRAY,false),8);
        for(int i=0;i<game.history.size();i++){
            XiangqiGame.Move m=game.history.get(i);
            String txt=(i+1)+". "+(m.piece>0?"红  ":"黑  ")+XiangqiGame.moveText(m);
            add(content,label(txt,12,Color.rgb(78,93,83),false),5);
        }
    }
    private void stepReplay(int dir){
        if(animating||replayBoard==null)return;
        int next=replayIndex+dir;
        if(next<0||next>game.history.size())return;
        XiangqiGame.Move move=game.history.get(dir>0?replayIndex:next);
        replayIndex=next;
        XiangqiGame snapshot=new XiangqiGame();
        for(int i=0;i<replayIndex;i++){
            XiangqiGame.Move replayMove=game.history.get(i);
            snapshot.move(replayMove.from,replayMove.to);
        }
        animating=true;
        replayBoard.bind(snapshot);
        final int ticket=serial;
        replayBoard.animateMove(move,dir<0,()->{
            if(screen!=REPLAY||ticket!=serial)return;
            animating=false;renderReplay();
        });
    }
    private void startPuzzle(int level){
        ++serial;cancelAnimations();
        puzzleIndex=level;puzzleGame=PuzzleBook.position(level);
        puzzleSelected=-1;puzzlePassed=false;
        renderPuzzle();
    }
    private void renderPuzzle(){
        screen=PUZZLE;
        shell("残局入门 · 第"+(puzzleIndex+1)+"关","一步将军训练 · "+PuzzleBook.TITLES[puzzleIndex]);
        add(content,label("目标：红方走一步，给黑方将军。",16,JADE,true),7);
        puzzleBoard=new BoardView(this);
        puzzleBoard.bind(puzzleGame);
        puzzleBoard.setSelected(puzzleSelected);
        puzzleBoard.setAllowMoves(!animating&&!puzzlePassed);
        puzzleBoard.onSquareClick(this::puzzleSquareTapped);
        add(content,puzzleBoard,10);
        if(puzzlePassed){
            add(content,label("★ 挑战成功！",19,Color.rgb(27,125,85),true),10);
            LinearLayout controls=row();
            addAction(controls,"再来一次",Color.WHITE,false,()->startPuzzle(puzzleIndex));
            addAction(controls,puzzleIndex==PuzzleBook.TITLES.length-1?"回大厅":"下一关",JADE,true,
                ()->{if(puzzleIndex+1<PuzzleBook.TITLES.length)startPuzzle(puzzleIndex+1);else home();});
            add(content,controls,8);
        }else{
            LinearLayout actions=row();
            addAction(actions,"走法提示",Color.WHITE,false,()->{
                if(animating)return;
                puzzleBoard.setHint(PuzzleBook.FROM[puzzleIndex],PuzzleBook.TO[puzzleIndex]);
                toast(PuzzleBook.TIPS[puzzleIndex]);
            });
            addAction(actions,"重新摆棋",Color.WHITE,false,()->startPuzzle(puzzleIndex));
            add(content,actions,8);
        }
        LinearLayout nav=row();
        addAction(nav,"返回大厅",Color.rgb(219,229,219),false,this::home);
        add(content,nav,5);
        add(content,label("本关只考一步将军，暂不涉及连续杀法。",12,Color.GRAY,false),5);
    }
    private void puzzleSquareTapped(int sq){
        if(screen!=PUZZLE||animating||puzzlePassed)return;
        if(puzzleSelected<0){
            if(XiangqiGame.color(puzzleGame.board[sq])==XiangqiGame.RED){
                puzzleSelected=sq;puzzleBoard.setSelected(sq);
            }
            return;
        }
        if(puzzleSelected==sq){puzzleSelected=-1;puzzleBoard.setSelected(-1);return;}
        if(XiangqiGame.color(puzzleGame.board[sq])==XiangqiGame.RED){
            puzzleSelected=sq;puzzleBoard.setSelected(sq);return;
        }
        if(!PuzzleBook.isSolution(puzzleIndex,puzzleSelected,sq)){
            toast("再想一想，要一步将军。");return;
        }
        XiangqiGame.Move move=puzzleGame.move(puzzleSelected,sq);
        if(move==null){toast("走法不合法");return;}
        puzzleSelected=-1;animating=true;
        puzzleBoard.setSelected(-1);puzzleBoard.setHint(-1,-1);
        int ticket=serial;
        puzzleBoard.animateMove(move,false,()->{
            if(screen!=PUZZLE||ticket!=serial)return;
            puzzlePassed=puzzleGame.inCheck(XiangqiGame.BLACK);
            animating=false;renderPuzzle();
        });
    }
    private void copyMoves(){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("xiangqi moves",game.exportMoves()));toast("棋谱已复制");}
    }
    @Override public void onBackPressed(){
        if(screen==REPLAY){++serial;cancelAnimations();renderGame();scheduleAi();}
        else if(screen==PUZZLE)home();
        else if(screen==GAME)home();
        else super.onBackPressed();
    }
}
