import com.badukai.xiangqi.XiangqiGame;
import com.badukai.xiangqi.SimpleAi;
import java.util.Arrays;

public class CoreTest {
    static int sq(int r,int c){return r*9+c;}
    static void ok(boolean cond,String msg){if(!cond)throw new AssertionError(msg);}
    static boolean has(XiangqiGame g,int a,int b){return g.legalMoves().stream().anyMatch(m->m.from==a&&m.to==b);}
    static XiangqiGame blank(){XiangqiGame g=new XiangqiGame();Arrays.fill(g.board,0);g.board[sq(9,4)]=1;g.board[sq(0,3)]=-1;g.turn=1;return g;}
    public static void main(String[] args){
        XiangqiGame g=new XiangqiGame();
        ok(g.turn==1,"red starts");
        ok(g.board[sq(9,0)]==XiangqiGame.ROOK,"red rook");
        ok(g.board[sq(0,4)]==-XiangqiGame.GENERAL,"black general");
        ok(g.legalMoves().size()>=35,"initial moves");
        ok(!has(g,sq(6,0),sq(6,1)),"soldier cannot move sideways before river");
        ok(!has(g,sq(9,4),sq(6,4)),"general stuck in palace");
        ok(g.move(sq(6,0),sq(5,0))!=null,"soldier forward");
        ok(g.history.size()==1&&g.turn==-1,"move flip");
        String save=g.exportMoves();
        XiangqiGame imported=new XiangqiGame(); imported.importMoves(save);
        ok(Arrays.equals(imported.board,g.board)&&imported.turn==g.turn,"save/replay");
        g.unmakeLast();ok(g.history.isEmpty()&&g.turn==1,"undo");
        g=blank();g.board[sq(7,4)]=XiangqiGame.HORSE;g.board[sq(6,4)]=XiangqiGame.SOLDIER;
        ok(!has(g,sq(7,4),sq(5,3)),"blocked horse leg");
        g.board[sq(6,4)]=0;
        ok(has(g,sq(7,4),sq(5,3)),"free horse leg");
        g=blank();g.board[sq(7,1)]=XiangqiGame.CANNON;g.board[sq(5,1)]=XiangqiGame.SOLDIER;g.board[sq(2,1)]=-XiangqiGame.ROOK;
        ok(has(g,sq(7,1),sq(2,1)),"cannon must jump exactly one screen");
        g.board[sq(5,1)]=0;
        ok(!has(g,sq(7,1),sq(2,1)),"cannon capture without screen forbidden");
        g=blank();g.board[sq(9,4)]=1;g.board[sq(0,4)]=-1;g.board[sq(5,4)]=XiangqiGame.ROOK;
        ok(!has(g,sq(5,4),sq(5,3)),"cannot expose facing generals");
        ok(has(g,sq(5,4),sq(0,4)),"flying along file to capture black general");
        g=blank();g.board[sq(5,0)]=XiangqiGame.SOLDIER;
        ok(has(g,sq(5,0),sq(4,0)),"advance soldier to opposite bank");
        g.move(sq(5,0),sq(4,0));g.turn=1;
        ok(has(g,sq(4,0),sq(4,1)),"soldier sideways after crossing");
        // Protect against board corruption after a long series of legal moves and undo.
        for(int seed=1;seed<=5;seed++){
            XiangqiGame walk=new XiangqiGame();
            int[] initial=walk.board.clone();
            java.util.Random rnd=new java.util.Random(seed);
            for(int j=0;j<55;j++){
                java.util.List<XiangqiGame.Move> moves=walk.legalMoves();
                if(moves.isEmpty())break;
                XiangqiGame.Move m=moves.get(rnd.nextInt(moves.size()));
                walk.makeUnchecked(m);
                ok(!walk.inCheck(-walk.turn),"played move must never leave mover in check");
            }
            while(!walk.history.isEmpty())walk.unmakeLast();
            ok(walk.turn==1&&Arrays.equals(initial,walk.board),"random round-trip restore seed "+seed);
        }
        XiangqiGame standard=new XiangqiGame();
        String before=standard.exportMoves();
        SimpleAi.Result best=new SimpleAi().best(standard,2,1000);
        ok(best.move!=null && standard.history.isEmpty() && standard.exportMoves().equals(before),"AI does not mutate board");
        System.out.println("PASS: Chinese chess rules, moves, checks, undo, persistence, and AI. Initial legal moves: "+new XiangqiGame().legalMoves().size());
    }
}
