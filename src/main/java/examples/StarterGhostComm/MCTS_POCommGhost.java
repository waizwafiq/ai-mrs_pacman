package examples.StarterGhostComm;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

import java.util.Random;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Zyad Zarin on 26/12/2023.
 */

/*
 * MCTS_POCommGhost Algorithm Explanation
1. Overall Structure
The code defines a ghost controller class (MCTS_POCommGhost) using the Monte Carlo Tree Search (MCTS) algorithm with partial observability. The ghost's behavior is determined based on simulations and tree exploration.

2. Parameters
CONSISTENCY: Probability of the ghost attacking Ms. Pac-Man.
PILL_PROXIMITY: Threshold distance to power pills.
TICK_THRESHOLD: Time threshold to discard old information.
NUM_SIMULATIONS: Number of simulations in the MCTS algorithm.
UCT_CONSTANT: Exploration constant in the UCT (Upper Confidence Bound for Trees) formula.

3. Main Flow (getMove Method)
Initialization: Initialize variables and clear old information.
Visibility Check: Check if Ms. Pac-Man is visible and update information accordingly.
MCTS or Random Move: If the ghost requires an action, decide whether to attack, retreat, or perform MCTS-based decision making.

4. MCTS (runMCTS Method)
Initialization: Create the root node based on the current game state.
Simulations: Run a specified number of simulations (iterations of selection, expansion, simulation, and backpropagation).
Select Best Move: Choose the best move based on the most visited child node.
Debug Information: Print debug information about the MCTS tree.

5. MCTS Components (select, expand, simulate, backpropagate)
Select: Choose nodes until a terminal or unexplored node is reached.
Expand: Generate child nodes for legal moves from the current game state.
Simulate: Use random playouts on the cloned game state and return the score.
Backpropagate: Update node statistics (visits and score) based on the simulated score.

6. UCT Value Calculation (getUCTValue Method)
Calculate the UCT value for a child node, balancing exploration and exploitation.
Use a custom scoring function to prioritize nodes where Pac-Man is closer to the ghost.

7. Fitness Function (calculateExploitationValue Method)
Custom scoring function to calculate the exploitation value.
Gives higher scores to nodes where Pac-Man is closer to the ghost.

8. Node Class
Represents a node in the MCTS tree, containing information about moves, visits, scores, and positions.

9. Debug Information (printDebugInfo Method)
Print debug information about the MCTS tree, including root visits and information about child nodes.

10. Power Pill Proximity Check (closeToPower Method)
Helper function to check if Ms. Pac-Man is close to an available power pill.

MCTS (Monte Carlo Tree Search) Explanation
MCTS is a probabilistic search algorithm used for decision-making in games. It involves four main steps in each iteration: selection, expansion, simulation, and backpropagation.

Selection: Start from the root node and recursively choose child nodes based on a selection policy (often UCT) until an unexplored or terminal node is reached.

Expansion: If the selected node has unexplored children, expand the tree by adding one or more child nodes representing possible future states.

Simulation (Rollout): Conduct a simulation (random playout) from the newly expanded node to a terminal state, collecting the outcome or score.

Backpropagation: Update the statistics of all nodes along the path from the expanded node to the root based on the simulation outcome.

The algorithm repeats these steps for a specified number of iterations, and the decision is made based on the statistics of the children of the root node.

Fitness Functions and Objectives
Objective of the Ghost: The primary objective is to catch Ms. Pac-Man or hinder her progress. The ghost's decision-making involves a balance between chasing Pac-Man, retreating when edible, and avoiding dangerous situations.

Exploitation Value (Scoring Function): The calculateExploitationValue function provides a custom scoring function for the exploitation value. It prioritizes nodes where Pac-Man is closer to the ghost, influencing the decision-making process.

These fitness functions and objectives guide the ghost's behavior and strategy during the game, allowing it to adapt to different game states and make decisions based on the information available.
 * 
 */

public class MCTS_POCommGhost extends IndividualGhostController {
    private final static float CONSISTENCY = 0.9f;    //attack Ms Pac-Man with this probability
    private final static int PILL_PROXIMITY = 15;        //if Ms Pac-Man is this close to a power pill, back away
    Random rnd = new Random();
    private int TICK_THRESHOLD;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;

    // MCTS PARAMS
    private final static int NUM_SIMULATIONS = 500; // Adjust the number of simulations as needed
    private final static double UCT_CONSTANT = Math.sqrt(2); // Adjust the UCT constant as needed

    public MCTS_POCommGhost(Constants.GHOST ghost) {
        this(ghost, 5);
    }

    public MCTS_POCommGhost(Constants.GHOST ghost, int TICK_THRESHOLD) {
        super(ghost);
        this.TICK_THRESHOLD = TICK_THRESHOLD;
    }

    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        // Housekeeping - throw out old info
        int currentTick = game.getCurrentLevelTime();
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
        }

        // Can we see PacMan? If so tell people and update our info
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();
        if (pacmanIndex != -1) {
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
            if (messenger != null) {
                messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex, game.getCurrentLevelTime()));
            }
        }

        // Has anybody else seen PacMan if we haven't?
        if (pacmanIndex == -1 && game.getMessenger() != null) {
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer information
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                }
            }
        }
        if (pacmanIndex == -1) {
            pacmanIndex = lastPacmanIndex;
        }

        Boolean requiresAction = game.doesGhostRequireAction(ghost);
        if (requiresAction != null && requiresAction)        //if ghost requires an action
        {
            if (pacmanIndex != -1) {
                if (game.getGhostEdibleTime(ghost) > 0)    //retreat from Ms Pac-Man if edible or if Ms Pac-Man is close to power pill
                {
                    try {
                        return game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                                game.getPacmanCurrentNodeIndex(), game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println(e);
                        System.out.println(pacmanIndex + " : " + currentIndex);
                    }
                } else {
                    // try {
                        // Constants.MOVE move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                        //         pacmanIndex, game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                        // return move;
                        
                        // MCTS
                        return runMCTS(game, pacmanIndex);
                    // } catch (ArrayIndexOutOfBoundsException e) {
                    //     System.out.println(e);
                    //     System.out.println(pacmanIndex + " : " + currentIndex);
                    // }
                    
                }
            } else {
                Constants.MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost));
                return possibleMoves[rnd.nextInt(possibleMoves.length)];
            }
        }
        return null;
    }

    private Constants.MOVE runMCTS(Game game, int pacmanIndex) {
        Node root = new Node(null, game.getGhostLastMoveMade(ghost), game.getGhostCurrentNodeIndex(ghost), pacmanIndex); // root node

        // Run simulations
        for (int i = 0; i< NUM_SIMULATIONS; i++) {
            // Selection, expansion, simulation, and backpropagration
            Node selectedNode = select(root, game);
            expand(selectedNode, game);
            int score = simulate(selectedNode, game);
            backpropagate(selectedNode, score);
        }

        // Choose the best move based on the most visited child
        Node bestChild = getBestChild(root, game);

        // Print debug information about the MCTS tree
        printDebugInfo(root, game);

        return bestChild == null ? Constants.MOVE.NEUTRAL : bestChild.move;

    }

    private Node select(Node root, Game game) {
        Node currentNode = root;

        while (!currentNode.children.isEmpty() && !isTerminal(currentNode)) {
            currentNode = getBestChild(currentNode, game);
        }

        return currentNode;
    }

    private void expand(Node node, Game game) {
        try {
            // Generate child nodes for legal moves from the current game state
            Constants.MOVE[] ghostPossibleMoves = game.getPossibleMoves(node.ghostPosition, node.move);
            Constants.MOVE[] pacmanPossibleMoves = game.getPossibleMoves(node.pacManPosition);
    
            if (ghostPossibleMoves != null && pacmanPossibleMoves != null) {
                for (Constants.MOVE ghostMove : ghostPossibleMoves) {
                    for (Constants.MOVE pacmanMove : pacmanPossibleMoves) {
                        try {
                            // Simulate the ghost's move
                            int newGhostPosition = game.getNeighbour(node.ghostPosition, ghostMove);
    
                            // Simulate Pac-Man's move
                            int newPacManPosition = game.getNeighbour(node.pacManPosition, pacmanMove);
    
                            // Check if the positions are valid
                            if (newGhostPosition != -1 && newPacManPosition != -1) {
                                Node child = new Node(node, ghostMove, newGhostPosition, newPacManPosition);
                                node.children.add(child);
                            } else {
                                // Handle the case where the positions are invalid
                                // You may want to skip adding this child or take appropriate action
                            }
                        } catch (Exception e) {
                            // Handle specific exception or log the exception
                            System.err.println("Error in expand: " + e.getMessage());
                        }
                    }
                }
            } else {
                // Handle the case where ghostPossibleMoves or pacmanPossibleMoves is null
            }
        } catch (Exception e) {
            // Handle specific exception or log the exception
            System.err.println("Error in expand: " + e.getMessage());
        }
    }
    
    

    private int simulate(Node node, Game game) {
        // Clone the game state to perform a simulation
        Game clonedGame = game.copy();

        int score = 0;
        int simulationDepth = 20;

        for (int i = 0; i < simulationDepth; i++) {
            // use random playouts on the cloned game state
            Constants.MOVE ghostMove = getRandomMove(game, node);
            Constants.MOVE pacManMove = getRandomMove(game, node);

            // Update the cloned game state
            clonedGame.advanceGame(pacManMove, new EnumMap<>(Map.of(ghost, ghostMove)));

            if (clonedGame.wasPacManEaten()) {
                score = Integer.MIN_VALUE;
                break;
            }
            else if (clonedGame.gameOver()) {
                score = Integer.MAX_VALUE;
                break;
            }
            else {
                score = clonedGame.getScore();
            }
        }

        return score;
    }

    private void backpropagate(Node node, int score) {
        // Backpropagate the score up the tree
        while (node != null ) {
            node.visits++;
            node.score += score;
            node = node.parent;
        }
    }

    private Node getBestChild(Node node, Game game) {
        // Choose the child with the highest UCT value
        double bestUCTValue = Double.NEGATIVE_INFINITY;
        Node bestChild = null;

        for (Node child : node.children) {
            double uctValue = getUCTValue(node, child, game);
            if (uctValue > bestUCTValue) {
                bestUCTValue = uctValue;
                bestChild = child;
            }
        }

        return bestChild;
    }

/**
 * Calculates the UCT (Upper Confidence Bound for Trees) value for a given child node in the Monte Carlo Tree Search (MCTS).
 * UCT is a balance between exploration and exploitation in the tree search.
 *
 * @param parent The parent node in the tree.
 * @param child  The child node for which the UCT value is calculated.
 * @param game   The current game state.
 * @return The UCT value for the child node.
 */
private double getUCTValue(Node parent, Node child, Game game) {
    // If the child has not been visited yet, return positive infinity to prioritize its exploration
    if (child.visits == 0) {
        return Double.POSITIVE_INFINITY;
    }

    // Calculate exploitation value using a custom scoring function
    double exploitationValue = calculateExploitationValue(child, game);

    // Calculate exploration value based on the number of visits and the parent's visits
    double explorationValue = Math.sqrt(2 * Math.log(parent.visits) / child.visits);

    // Factor based on the reciprocal of the shortest path distance between ghost and Pac-Man
    double ghostDistanceFactor = 1.0 / (1.0 + game.getShortestPathDistance(child.ghostPosition, child.pacManPosition));

    // Combine exploitation and exploration values with the influence of ghost and Pac-Man positions
    return exploitationValue + UCT_CONSTANT * explorationValue * ghostDistanceFactor;
}

/**
 * Calculates the exploitation value for a node based on a custom scoring function.
 * This function gives higher scores to nodes where Pac-Man is closer to the ghost.
 *
 * @param node The node for which the exploitation value is calculated.
 * @param game The current game state.
 * @return The exploitation value for the node.
 */
private double calculateExploitationValue(Node node, Game game) {
    // Custom scoring function: higher score for nodes where Pac-Man is closer to the ghost
    int pacmanDistance = game.getShortestPathDistance(node.ghostPosition, node.pacManPosition);
    return 1.0 / (1.0 + pacmanDistance);
}
    

    private Constants.MOVE getRandomMove(Game game, Node node) {
        if (node == null || node.move == null) {
            return Constants.MOVE.NEUTRAL;
        }
        else {
            Constants.MOVE[] possibleMoves = game.getPossibleMoves(node.ghostPosition, node.move);
            return possibleMoves[rnd.nextInt(possibleMoves.length)];
        }
    }

    private boolean isTerminal(Node node) {
        return node.children.isEmpty();
    }

    private void printDebugInfo(Node root, Game game) {
        // Print debug information about the MCTS tree
        System.out.println("MCTS Tree Information:");
        System.out.println("Root Visits: " + root.visits);

        // Print information about the children of the root node
        System.out.println("Root Children Information:");
        System.out.println("Number of Children: " + root.children.size());


        for (Node child : root.children) {
            System.out.println("Child Move: " + child.move);
            System.out.println("Child Visits: " + child.visits);
            System.out.println("Child Score: " + child.score);
            System.out.println("--------------------");
            
        }

        System.out.println("Best Move Chosen: " + getBestChild(root, game).move);
    }

    private static class Node {
        Node parent;
        Constants.MOVE move;
        int visits;
        int score;

        int ghostPosition;
        int pacManPosition;
        List<Node> children;

        public Node(Node parent, Constants.MOVE move, int ghostPosition, int pacManPosition) {
            this.parent = parent;
            this.move = move;
            this.visits = 0;
            this.score = 0;
            this.ghostPosition = ghostPosition;
            this.pacManPosition = pacManPosition;
            this.children = new ArrayList<>();
        }

    }

    //This helper function checks if Ms Pac-Man is close to an available power pill
    private boolean closeToPower(Game game) {
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < powerPills.length; i++) {
            Boolean powerPillStillAvailable = game.isPowerPillStillAvailable(i);
            int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
            if (pacmanNodeIndex == -1) {
                pacmanNodeIndex = lastPacmanIndex;
            }
            if (powerPillStillAvailable == null || pacmanNodeIndex == -1) {
                return false;
            }
            if (powerPillStillAvailable && game.getShortestPathDistance(powerPills[i], pacmanNodeIndex) < PILL_PROXIMITY) {
                return true;
            }
        }

        return false;
    }
}