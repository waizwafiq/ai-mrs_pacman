
import examples.StarterGhostComm.Blinky;
import examples.StarterGhostComm.Inky;
import examples.StarterGhostComm.Pinky;
import examples.StarterGhostComm.Sue;
import examples.StarterPacMan.*;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants.*;
import pacman.game.internal.POType;

import java.util.EnumMap;


/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    public static void main(String[] args) {
    	
    	int sightRadius = 10; // 5000 is maximum

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(false)
                .setTickLimit(20000)
                .setScaleFactor(3) // Increase game visual size
                .setPOType(POType.RADIUS) // pacman sense objects around it in a radius wide fashion instead of straight line sights
                .setSightLimit(sightRadius) // The sight radius limit, set to maximum 
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());
        
        
        MASController ghosts = new POCommGhosts(50);
        
        
        int speed = 25; // smaller number will run faster
        // executor.runGame(new SecondCustomAI(), ghosts, speed); 
        
        // executor.runGame(new RandomWalk(), ghosts, speed); 
        // executor.runGame(new AStar(), ghosts, speed); 
        executor.runGame(new Q_RL(), new MASController(controllers), speed); 
        // executor.runGame(new Dijkstra(), ghosts, speed); 
        // executor.runGame(new MCTS(), ghosts, speed); 
        // executor.runGame(new TreeSearchPacMan(), ghosts, speed); 
        // executor.runGame(new MyPacMan(), new MASController(controllers), speed);

    }
}
