package examples.StarterGhostComm;

import com.fossgalaxy.object.annotations.ObjectDef;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

import java.util.EnumMap;
import java.util.Random;

/**
 * Created by pwillic on 25/02/2016.
 */
public class MCTS_Ghost extends MASController {

    public MCTS_Ghost() {
        this(50);
    }

    @ObjectDef("POGC")
    public MCTS_Ghost(int TICK_THRESHOLD) {
        super(true, new EnumMap<GHOST, IndividualGhostController>(GHOST.class));
        controllers.put(GHOST.BLINKY, new MCTS_POCommGhost(GHOST.BLINKY, TICK_THRESHOLD));
        controllers.put(GHOST.INKY, new MCTS_POCommGhost(GHOST.INKY, TICK_THRESHOLD));
        controllers.put(GHOST.PINKY, new MCTS_POCommGhost(GHOST.PINKY, TICK_THRESHOLD));
        controllers.put(GHOST.SUE, new MCTS_POCommGhost(GHOST.SUE, TICK_THRESHOLD));
    }

    @Override
    public String getName() {
        return "POGC";
    }
}

