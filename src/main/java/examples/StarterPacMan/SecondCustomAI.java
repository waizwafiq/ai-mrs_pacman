package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class SecondCustomAI extends PacmanController {
    private static final int SIMULATIONS = 50;
    private static final double EXPLORATION_PARAMETER = 1.0; // You can experiment with different values
    private Random random = new Random();

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();

        // Create the root node of the Monte Carlo Tree
        MonteCarloNode rootNode = new MonteCarloNode(current);

        // Perform Monte Carlo Tree Search
        for (int i = 0; i < SIMULATIONS; i++) {
            // Select a node to expand from
            MonteCarloNode selectedNode = select(rootNode, game);

            // Expand the selected node
            MonteCarloNode expandedNode = expand(selectedNode, game);

            // Simulate a playout from the expanded node
            int score = simulate(expandedNode, game);

            // Backpropagate the result
            backpropagate(expandedNode, score);
        }

        // Choose the best move based on the Monte Carlo Tree
        return getBestMove(rootNode, game);
    }

    private MonteCarloNode select(MonteCarloNode rootNode, Game game) {
        // Implement selection strategy, e.g., UCT (Upper Confidence Bound for Trees)
        // This method should traverse the tree to find the most promising node to
        // expand
        // You might want to use a balance between exploration and exploitation.
        // The UCT formula is commonly used for this purpose.
        // UCT(node) = Q(node) / N(node) + C * sqrt(ln(N(parent_node)) / N(node))
        // where Q is the total reward, N is the number of visits, C is the exploration
        // parameter.
        // Experiment with different values for C.

        // Placeholder implementation, replace with your own logic
        MonteCarloNode currentNode = rootNode;

        // Selection phase
        while (!currentNode.getChildren().isEmpty()) {
            currentNode = UCTSelectChild(currentNode);
        }

        return currentNode;
    }

    private MonteCarloNode UCTSelectChild(MonteCarloNode node) {
        double maxUCT = Double.NEGATIVE_INFINITY;
        MonteCarloNode selectedChild = null;

        for (MonteCarloNode child : node.getChildren()) {
            double uctValue = calculateUCTValue(child);
            if (uctValue > maxUCT) {
                maxUCT = uctValue;
                selectedChild = child;
            }
        }

        return selectedChild;
    }

    private double calculateUCTValue(MonteCarloNode node) {
        if (node.getVisits() == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double exploitation = (double) node.getTotalScore() / node.getVisits();
        double exploration = Math.sqrt(Math.log(node.getParent().getVisits()) / node.getVisits());

        // Adjust the exploration parameter (EXPLORATION_PARAMETER) based on your needs
        return exploitation + EXPLORATION_PARAMETER * exploration;
    }

    private MonteCarloNode expand(MonteCarloNode node, Game game) {
        // Implement node expansion strategy, e.g., expand to an unvisited child node
        // This method should create a new node based on the current node and the game
        // state.

        // Placeholder implementation, replace with your own logic
        // Expansion phase
        ArrayList<MOVE> legalMoves = new ArrayList<>(Arrays.asList(MOVE.values()));
        legalMoves.remove(MOVE.NEUTRAL); // Remove the NEUTRAL move

        for (MOVE move : legalMoves) {
            Game copiedGame = game.copy();
            copiedGame.advanceGame(move, new StarterGhosts().getMove(copiedGame, -1)); // Replace with your ghost
                                                                                       // controller
            int newNodeIndex = copiedGame.getPacmanCurrentNodeIndex();

            // Check if the child node already exists
            MonteCarloNode existingChild = node.getChildWithMove(move);
            if (existingChild == null) {
                MonteCarloNode newChild = new MonteCarloNode(newNodeIndex, move, node);
                node.addChild(newChild);
                return newChild;
            }
        }

        // If all child nodes already exist, choose one randomly
        return node.getRandomChild(random);
    }

    private int simulate(MonteCarloNode node, Game game) {
        // Implement playout simulation
        // This method should simulate a random game from the given node and return the
        // result.

        // Placeholder implementation, replace with your own logic
        // Playout phase
        Game copiedGame = game.copy();
        copiedGame.advanceGame(MOVE.NEUTRAL, new StarterGhosts().getMove(copiedGame, -1)); // Replace with your ghost
                                                                                           // controller

        while (!copiedGame.gameOver()) {
            ArrayList<MOVE> legalMoves = new ArrayList<>(Arrays.asList(MOVE.values()));
            legalMoves.remove(MOVE.NEUTRAL); // Remove the NEUTRAL move

            MOVE randomMove = legalMoves.get(random.nextInt(legalMoves.size()));
            copiedGame.advanceGame(randomMove, new StarterGhosts().getMove(copiedGame, -1)); // Replace with your ghost
                                                                                             // controller
        }

        return copiedGame.getScore();
    }

    private void backpropagate(MonteCarloNode node, int score) {
        // Implement backpropagation of the simulation result
        // This method should update the node and its ancestors based on the simulation
        // result.

        // Placeholder implementation, replace with your own logic
        while (node != null) {
            node.update(score);
            node = node.getParent();
        }
    }

    private MOVE getBestMove(MonteCarloNode rootNode, Game game) {
        // Implement a strategy to choose the best move based on the Monte Carlo Tree
        // This method should select the move that leads to the child with the highest
        // average reward.

        // Placeholder implementation, replace with your own logic
        return rootNode.getBestChild().getMove();
    }

    private class MonteCarloNode {
        private int nodeIndex;
        private MOVE move;
        private MonteCarloNode parent;
        private ArrayList<MonteCarloNode> children;
        private int visits;
        private int totalScore;

        public MonteCarloNode(int nodeIndex) {
            this.nodeIndex = nodeIndex;
            this.move = MOVE.NEUTRAL;
            this.parent = null;
            this.children = new ArrayList<>();
            this.visits = 0;
            this.totalScore = 0;
        }

        public MonteCarloNode(int nodeIndex, MOVE move, MonteCarloNode parent) {
            this.nodeIndex = nodeIndex;
            this.move = move;
            this.parent = parent;
            this.children = new ArrayList<>();
            this.visits = 0;
            this.totalScore = 0;
        }

        public int getNodeIndex() {
            return nodeIndex;
        }

        public MOVE getMove() {
            return move;
        }

        public MonteCarloNode getParent() {
            return parent;
        }

        public ArrayList<MonteCarloNode> getChildren() {
            return children;
        }

        public int getVisits() {
            return visits;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void addChild(MonteCarloNode child) {
            children.add(child);
        }

        public MonteCarloNode getChildWithMove(MOVE move) {
            for (MonteCarloNode child : children) {
                if (child.getMove().equals(move)) {
                    return child;
                }
            }
            return null;
        }

        public MonteCarloNode getRandomChild(Random random) {
            return children.get(random.nextInt(children.size()));
        }

        public MonteCarloNode getBestChild() {
            double bestScore = Double.NEGATIVE_INFINITY;
            MonteCarloNode bestChild = null;

            for (MonteCarloNode child : children) {
                double childScore = (double) child.getTotalScore() / child.getVisits();
                if (childScore > bestScore) {
                    bestScore = childScore;
                    bestChild = child;
                }
            }

            return bestChild;
        }

        public void update(int score) {
            visits++;
            totalScore += score;
        }
    }
}
