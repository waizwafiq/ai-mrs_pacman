package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.Arrays;
import java.util.Random;

public class MCTS extends PacmanController {
    private static final int NUM_SIMULATIONS = 100;
    private Random random = new Random();

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();

        // Initialize the root of the tree
        Node root = new Node(null, null, current);

        // Perform Monte Carlo Tree Search
        for (int i = 0; i < NUM_SIMULATIONS; i++) {
            Node selectedNode = select(root, game);
            int score = simulate(selectedNode, game);
            backpropagate(selectedNode, score);
        }

        // Choose the best move based on the number of visits
        Node bestChild = Arrays.stream(root.getChildren())
                .max(Node::compareVisitCount)
                .orElseThrow(IllegalStateException::new);

        return bestChild.getMove();
    }

    // Selection phase
    private Node select(Node node, Game game) {
        // Implement tree traversal logic (e.g., UCT)
        // This part is specific to the MCTS algorithm and depends on your problem
        // domain
        // For simplicity, I'm using a random selection here
        if (node.isFullyExpanded()) {
            return select(node.selectChild(), game);
        } else {
            return node.expand(game);
        }
    }

    // Simulation phase
    private int simulate(Node node, Game game) {
        // Implement simulation logic (e.g., random playout)
        // This part is problem-specific and should be tailored to your game
        // For simplicity, I'm using a random playout here
        Game clonedGame = game.copy();
        int score = 0;

        while (!clonedGame.gameOver()) {
            MOVE randomMove = MOVE.values()[random.nextInt(MOVE.values().length)];
            clonedGame.advanceGame(randomMove, null);
            score += clonedGame.getScore();
        }

        return score;
    }

    // Backpropagation phase
    private void backpropagate(Node node, int score) {
        // Update the visit count and total score of each node in the path
        // back to the root based on the result of the simulation
        while (node != null) {
            node.update(score);
            node = node.getParent();
        }
    }

    private static class Node {
        private Node parent;
        private MOVE move;
        private int visitCount;
        private int totalScore;
        private int[] childIndices;
        private Node[] children;

        public Node(Node parent, MOVE move, int currentNodeIndex) {
            this.parent = parent;
            this.move = move;
            this.visitCount = 0;
            this.totalScore = 0;
            this.childIndices = Arrays.stream(MOVE.values())
                    .mapToInt(Enum::ordinal)
                    .filter(i -> i != MOVE.NEUTRAL.ordinal())
                    .toArray();
            this.children = new Node[childIndices.length];
        }

        public boolean isFullyExpanded() {
            return visitCount == childIndices.length;
        }

        public Node selectChild() {
            int selectedChildIndex = childIndices[visitCount];
            return new Node(this, MOVE.values()[selectedChildIndex], -1);
        }

        public Node expand(Game game) {
            if (!isFullyExpanded()) {
                int selectedChildIndex = childIndices[visitCount];
                MOVE selectedMove = MOVE.values()[selectedChildIndex];
                visitCount++;
                return new Node(this, selectedMove, -1);
            } else {
                throw new IllegalStateException("Node is fully expanded.");
            }
        }

        public void update(int score) {
            visitCount++;
            totalScore += score;
        }

        public Node getParent() {
            return parent;
        }

        public Node[] getChildren() {
            return children;
        }

        public MOVE getMove() {
            return move;
        }

        public static int compareVisitCount(Node a, Node b) {
            return Integer.compare(a.visitCount, b.visitCount);
        }

        // Additional getters and methods can be added based on your requirements
    }
}