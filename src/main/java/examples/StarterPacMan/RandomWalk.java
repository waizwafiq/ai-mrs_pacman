package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants.MOVE;
import pacman.game.Game;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

// A* Algorithm Implementation
public class RandomWalk extends PacmanController{
    private Random random = new Random();


    @Override
    public MOVE getMove(Game game, long timeDue) {
        // Should always be possible as we are PacMan
        int current = game.getPacmanCurrentNodeIndex();

        // Generate a random move
        MOVE randomMove = getRandomMove(game.getPacmanLastMoveMade());

        // Print the chosen move (for simulation purposes)
        System.out.println("Random Move: " + randomMove);

        return randomMove;
    }

    // Method to get a random move
    private MOVE getRandomMove(MOVE lastMove) {
        MOVE[] possibleMoves = MOVE.values();

        // Filter out the opposite move to avoid turning back
        MOVE[] filteredMoves = new MOVE[possibleMoves.length - 1];
        int index = 0;
        for (MOVE move : possibleMoves) {
            if (!move.opposite().equals(lastMove)) {
                filteredMoves[index++] = move;
            }
        }

        // Choose a random move from the filtered list
        return filteredMoves[random.nextInt(filteredMoves.length)];
    }
    
}
