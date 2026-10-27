package com.badukai.xiangqi;

import java.util.Comparator;
import java.util.List;

/** Lightweight baseline CPU AI; Pikafish JNI/UCI can replace it later. */
public final class SimpleAi {
    private static final int[] VALUE={0,20000,210,210,435,980,480,100};
    private static final int MATE=1000000;
    private long deadline;
    private boolean expired;
    private long nodes;
    public static final class Result {
        public final XiangqiGame.Move move;
        public final int score, depth;
        Result(XiangqiGame.Move move,int score,int depth){this.move=move;this.score=score;this.depth=depth;}
    }
    public Result best(XiangqiGame g,int maxDepth,long budgetMs) {
        if(maxDepth<1)maxDepth=1;
        deadline=System.nanoTime()+Math.max(100,budgetMs)*1_000_000L;
        expired=false;nodes=0;
        List<XiangqiGame.Move> legal=g.legalMoves();
        if(legal.isEmpty())return new Result(null,-MATE,0);
        sort(legal);
        Result best=new Result(legal.get(0),0,0);
        for(int depth=1;depth<=maxDepth;depth++) {
            int alpha=-MATE-1000,beta=MATE+1000;
            XiangqiGame.Move chosen=null;int score=-MATE-1000;
            for(XiangqiGame.Move m:legal) {
                if(timeout())break;
                g.makeUnchecked(m);
                int value=-negamax(g,depth-1,-beta,-alpha,1);
                g.unmakeLast();
                if(expired)break;
                if(value>score){score=value;chosen=m;}
                alpha=Math.max(alpha,value);
            }
            if(!expired&&chosen!=null) {
                best=new Result(chosen,score,depth);
                // Put the PV choice first for the next iteration.
                legal.remove(chosen);legal.add(0,chosen);
            }
            if(expired)break;
        }
        return best;
    }
    private boolean timeout(){return (++nodes%128==0&&System.nanoTime()>deadline)||(expired);}
    private int negamax(XiangqiGame g,int depth,int alpha,int beta,int ply) {
        if(timeout()) {expired=true;return evaluate(g);}
        // Game's current side loses even when stalemated, as in Chinese chess.
        List<XiangqiGame.Move> legal=g.legalMoves();
        if(legal.isEmpty())return -MATE+ply;
        if(depth==0)return evaluate(g);
        sort(legal);
        int best=-MATE-1000;
        for(XiangqiGame.Move m:legal) {
            g.makeUnchecked(m);
            int value=-negamax(g,depth-1,-beta,-alpha,ply+1);
            g.unmakeLast();
            if(expired)return 0;
            if(value>best)best=value;
            if(value>alpha)alpha=value;
            if(alpha>=beta)break;
        }
        return best;
    }
    private static void sort(List<XiangqiGame.Move> moves) {
        moves.sort(Comparator.comparingInt((XiangqiGame.Move m)->
                Math.abs(m.captured)*100 + (m.captured==0?0:VALUE[Math.abs(m.captured)])-VALUE[Math.abs(m.piece)]/15).reversed());
    }
    private static int evaluate(XiangqiGame g) {
        int score=0;
        for(int sq=0;sq<90;sq++) {
            int p=g.board[sq];if(p==0)continue;
            int who=XiangqiGame.color(p),type=Math.abs(p),r=sq/9,c=sq%9;
            int value=VALUE[type];
            if(type==XiangqiGame.SOLDIER) {
                int advanced=who==XiangqiGame.RED?9-r:r;
                value+=advanced*7;
                if(who==XiangqiGame.RED?r<=4:r>=5)value+=70;
            }
            if(type==XiangqiGame.HORSE||type==XiangqiGame.CANNON||type==XiangqiGame.ROOK)value+=Math.min(c,8-c)*5;
            score+=who*value;
        }
        return score*g.turn;
    }
}
