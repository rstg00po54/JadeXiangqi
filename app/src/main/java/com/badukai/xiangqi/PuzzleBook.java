package com.badukai.xiangqi;

import java.util.Arrays;

/** Four original, deterministic one-move-check training positions. */
public final class PuzzleBook {
    private PuzzleBook(){}
    public static final String[] TITLES={
        "车行将军","炮架将军","马跳将军","小兵逼宫"
    };
    public static final String[] TIPS={
        "横移车到中路，将军。",
        "炮的将军要隔一个炮架。",
        "马的落点决定它能攻击谁，注意马腿。",
        "过河兵能横着走，逼近敌方将位。"
    };
    public static final int[] FROM={3*9+1,4*9+2,4*9+4,1*9+3};
    public static final int[] TO={3*9+4,4*9+4,2*9+3,1*9+4};

    public static XiangqiGame position(int level){
        if(level<0||level>=TITLES.length)throw new IllegalArgumentException("level");
        XiangqiGame game=new XiangqiGame();
        Arrays.fill(game.board,0);
        game.history.clear();
        game.board[9*9+3]=XiangqiGame.GENERAL;
        game.board[0*9+4]=-XiangqiGame.GENERAL;
        game.turn=XiangqiGame.RED;
        switch(level){
            case 0: game.board[FROM[level]]=XiangqiGame.ROOK;break;
            case 1:
                game.board[FROM[level]]=XiangqiGame.CANNON;
                game.board[2*9+4]=-XiangqiGame.SOLDIER;
                break;
            case 2: game.board[FROM[level]]=XiangqiGame.HORSE;break;
            case 3: game.board[FROM[level]]=XiangqiGame.SOLDIER;break;
            default: throw new IllegalArgumentException("level");
        }
        return game;
    }
    public static boolean isSolution(int level,int from,int to){
        return level>=0&&level<TITLES.length&&FROM[level]==from&&TO[level]==to;
    }
}
