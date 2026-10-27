import com.badukai.xiangqi.PuzzleBook;
import com.badukai.xiangqi.XiangqiGame;

public class PuzzleTest {
    public static void main(String[] args){
        for(int level=0;level<PuzzleBook.TITLES.length;level++){
            XiangqiGame game=PuzzleBook.position(level);
            if(game.inCheck(XiangqiGame.RED)||game.inCheck(XiangqiGame.BLACK))
                throw new AssertionError("Initial puzzle position is already in check: "+level);
            if(game.move(PuzzleBook.FROM[level],PuzzleBook.TO[level])==null)
                throw new AssertionError("Puzzle solution is illegal: "+level);
            if(!game.inCheck(XiangqiGame.BLACK))
                throw new AssertionError("Solution does not give check: "+level);
        }
        System.out.println("PASS: "+PuzzleBook.TITLES.length+" beginner check-training puzzles.");
    }
}
