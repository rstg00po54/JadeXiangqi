package com.badukai.xiangqi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Standalone, pure Java Chinese-chess game rules. Red starts at the bottom. */
public final class XiangqiGame {
    public static final int RED = 1, BLACK = -1;
    public static final int GENERAL = 1, ADVISOR = 2, ELEPHANT = 3,
            HORSE = 4, ROOK = 5, CANNON = 6, SOLDIER = 7;
    public final int[] board = new int[90];
    public int turn = RED;
    public int halfMoves = 0;
    public final ArrayList<Move> history = new ArrayList<>();

    public static final class Move {
        public final int from, to, piece, captured;
        public Move(int from, int to, int piece, int captured) {
            this.from = from; this.to = to; this.piece = piece; this.captured = captured;
        }
        public String serialize() { return from + ":" + to; }
    }
    public XiangqiGame() { reset(); }
    private static int at(int r, int c) { return r * 9 + c; }
    private static int row(int index) { return index / 9; }
    private static int col(int index) { return index % 9; }
    private static boolean inside(int r, int c) { return r >= 0 && r < 10 && c >= 0 && c < 9; }
    public static int color(int piece) { return Integer.compare(piece, 0); }

    public void reset() {
        Arrays.fill(board, 0);
        int[] line = {ROOK, HORSE, ELEPHANT, ADVISOR, GENERAL, ADVISOR, ELEPHANT, HORSE, ROOK};
        for (int c = 0; c < 9; c++) {board[at(0,c)] = -line[c]; board[at(9,c)] = line[c];}
        board[at(2,1)] = -CANNON; board[at(2,7)] = -CANNON;
        board[at(7,1)] = CANNON; board[at(7,7)] = CANNON;
        for(int c=0;c<9;c+=2) {board[at(3,c)] = -SOLDIER;board[at(6,c)] = SOLDIER;}
        turn = RED; halfMoves = 0; history.clear();
    }

    public int generalSquare(int who) {
        for(int i=0;i<90;i++) if(board[i]==who*GENERAL) return i;
        return -1;
    }
    private static boolean inPalace(int r,int c,int who) {
        return c>=3&&c<=5&&(who==RED ? r>=7&&r<=9 : r>=0&&r<=2);
    }
    private void add(List<Move> out,int from,int r,int c,int who) {
        if(!inside(r,c))return;
        int to=at(r,c);
        if(color(board[to]) != who) out.add(new Move(from,to,board[from],board[to]));
    }

    /** Generates capture/movement candidates; check and flying-general legality are filtered separately. */
    private List<Move> pseudoMoves(int who) {
        ArrayList<Move> moves=new ArrayList<>(72);
        for(int from=0;from<90;from++) {
            int piece=board[from];
            if(color(piece)!=who)continue;
            int type=Math.abs(piece),r=row(from),c=col(from);
            switch(type) {
                case GENERAL:
                    for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nr=r+d[0],nc=c+d[1];
                        if(inPalace(nr,nc,who))add(moves,from,nr,nc,who);
                    }
                    // Special capture when the two generals see each other.
                    int other=generalSquare(-who);
                    if(other>=0&&col(other)==c) {
                        int dr=Integer.compare(row(other),r),rr=r+dr;
                        while(rr!=row(other)&&board[at(rr,c)]==0)rr+=dr;
                        if(rr==row(other))add(moves,from,rr,c,who);
                    }
                    break;
                case ADVISOR:
                    for(int dr:new int[]{-1,1})for(int dc:new int[]{-1,1})
                        if(inPalace(r+dr,c+dc,who))add(moves,from,r+dr,c+dc,who);
                    break;
                case ELEPHANT:
                    for(int dr:new int[]{-2,2})for(int dc:new int[]{-2,2}) {
                        int nr=r+dr,nc=c+dc;
                        if(!inside(nr,nc)||(who==RED&&nr<5)||(who==BLACK&&nr>4))continue;
                        if(board[at(r+dr/2,c+dc/2)]==0)add(moves,from,nr,nc,who);
                    }
                    break;
                case HORSE:
                    for(int[] leg:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int lr=r+leg[0],lc=c+leg[1];
                        if(!inside(lr,lc)||board[at(lr,lc)]!=0)continue;
                        if(leg[0]!=0) {add(moves,from,r+2*leg[0],c+1,who);add(moves,from,r+2*leg[0],c-1,who);}
                        else {add(moves,from,r+1,c+2*leg[1],who);add(moves,from,r-1,c+2*leg[1],who);}
                    }
                    break;
                case ROOK:
                    for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nr=r+d[0],nc=c+d[1];
                        while(inside(nr,nc)) {
                            if(color(board[at(nr,nc)])==who)break;
                            add(moves,from,nr,nc,who);
                            if(board[at(nr,nc)]!=0)break;
                            nr+=d[0];nc+=d[1];
                        }
                    }
                    break;
                case CANNON:
                    for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nr=r+d[0],nc=c+d[1];boolean screen=false;
                        while(inside(nr,nc)) {
                            int target=board[at(nr,nc)];
                            if(!screen) {if(target==0)add(moves,from,nr,nc,who);else screen=true;}
                            else if(target!=0) {if(color(target)==-who)add(moves,from,nr,nc,who);break;}
                            nr+=d[0];nc+=d[1];
                        }
                    }
                    break;
                case SOLDIER:
                    add(moves,from,r-who,c,who);
                    if(who==RED?r<=4:r>=5) {add(moves,from,r,c-1,who);add(moves,from,r,c+1,who);}
                    break;
                default: throw new IllegalStateException("Unknown piece " + piece);
            }
        }
        return moves;
    }

    public boolean inCheck(int who) {
        int general=generalSquare(who);
        if(general<0)return true;
        for(Move m:pseudoMoves(-who)) if(m.to==general) return true;
        return false;
    }

    /** Legal moves never leave one's own general threatened or allow two facing generals. */
    public List<Move> legalMoves(int who) {
        ArrayList<Move> legal=new ArrayList<>();
        if(generalSquare(who)<0)return legal;
        for(Move m:pseudoMoves(who)) {
            board[m.to]=board[m.from];board[m.from]=0;
            boolean safe=!inCheck(who);
            board[m.from]=m.piece;board[m.to]=m.captured;
            if(safe)legal.add(m);
        }
        return legal;
    }
    public List<Move> legalMoves() {return legalMoves(turn);}
    public Move move(int from,int to) {
        for(Move m:legalMoves())if(m.from==from&&m.to==to) {makeUnchecked(m);return m;}
        return null;
    }
    public void makeUnchecked(Move m) {
        board[m.to]=board[m.from];board[m.from]=0;
        history.add(m);turn=-turn;halfMoves++;
    }
    public void unmakeLast() {
        if(history.isEmpty())return;
        Move m=history.remove(history.size()-1);
        board[m.from]=m.piece;board[m.to]=m.captured;turn=-turn;halfMoves--;
    }
    public boolean isOver() {return generalSquare(turn)<0 || legalMoves().isEmpty();}
    public int winner() {return isOver()?-turn:0;}
    public String exportMoves() {
        StringBuilder s=new StringBuilder();for(Move m:history) {if(s.length()>0)s.append(',');s.append(m.serialize());}
        return s.toString();
    }
    public void importMoves(String text) {
        reset();if(text==null||text.trim().isEmpty())return;
        for(String token:text.split(",")) {
            String[] cells=token.split(":");
            if(cells.length!=2)break;
            try {if(move(Integer.parseInt(cells[0]),Integer.parseInt(cells[1]))==null)break;}
            catch(NumberFormatException ex){break;}
        }
    }
    public static String pieceName(int piece) {
        String red="帅仕相马车炮兵",black="将士象马车炮卒";
        int t=Math.abs(piece);
        return t<1||t>7?"":String.valueOf((piece>0?red:black).charAt(t-1));
    }
    public static String point(int sq){return "("+(sq/9+1)+","+(sq%9+1)+")";}
    public static String moveText(Move move) {
        return pieceName(move.piece)+point(move.from)+"→"+point(move.to)+(move.captured!=0?" 吃"+pieceName(move.captured):"");
    }
}
