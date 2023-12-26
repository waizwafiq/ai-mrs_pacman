package examples.StarterGhostComm;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * QLearning_POCommGhost is a Pac-Man ghost controller using Q-learning with communication.
 *
 * This controller makes decisions for a ghost agent using Q-learning, a reinforcement learning algorithm.
 * The agent learns to maximize its cumulative reward by updating Q-values based on observed rewards.
 * Additionally, the controller communicates information about the Pac-Man's location with other ghosts using Messenger.
 *
 * Created by pwillic on 25/02/2016.
 */
public class QLearning_POCommGhost extends IndividualGhostController {

    // Constants for controlling ghost behavior
    private final static float CONSISTENCY = 0.9f;    // Attack Ms. Pac-Man with this probability
    private final static int PILL_PROXIMITY = 15;      // If Ms. Pac-Man is this close to a power pill, back away

    // Q Learning Parameters
    private final static float LEARNING_RATE = 0.1f;
    private final static float DISCOUNT_FACTOR = 0.9f;
    private final static float EXPLORATION_PROBABILITY = 0.1f;
    private final static int NUM_TRAINING = 100;
    private final static int episodesSoFar = 0;

    // Reward constants
    private static final double PACMAN_NOT_EATING_PILLS_REWARD = 1.0;
    private static final double EAT_PACMAN_REWARD = 50.0;
    private static final double DECAY_REWARD = 0.05;
    private static final double GHOST_EATEN_PENALTY = -15.0;
    private static final double PACMAN_EATING_POWERPILLS_PENALTY = -5.0;
    private static final double PACMAN_EATING_PILLS_PENALTY = 0.0;
    private static final double LEVEL_UP_PENALTY = -100.0;

    // Q-learning data structures
    private ArrayList<Double> current_reward = new ArrayList<>(Collections.singletonList(0.0));
    private Map<StateActionPair, Double> qValues = new HashMap<>();
    private MOVE lastMove;

    // Random number generator
    Random rnd = new Random();

    // Communication and tick tracking variables
    private int TICK_THRESHOLD;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;

    /**
     * Constructs a QLearning_POCommGhost with default TICK_THRESHOLD.
     *
     * @param ghost The ghost type controlled by this controller.
     */
    public QLearning_POCommGhost(Constants.GHOST ghost) {
        this(ghost, 5);
    }

    /**
     * Constructs a QLearning_POCommGhost with a specified TICK_THRESHOLD.
     *
     * @param ghost The ghost type controlled by this controller.
     * @param TICK_THRESHOLD The threshold for resetting information about Pac-Man.
     */
    public QLearning_POCommGhost(Constants.GHOST ghost, int TICK_THRESHOLD) {
        super(ghost);
        this.TICK_THRESHOLD = TICK_THRESHOLD;
    }

    // Helper method to print information about the current move and reward
    private void printMoveInfo(String info, MOVE move, Game game) {
        double reward = calculateReward(game);
        System.out.println("[GHOST] " + info + " - Current Move: " + move + " - Current Reward: " + reward);
    }

    /**
     * Gets the next move for the ghost based on Q-learning and communication.
     *
     * @param game The current game state.
     * @param timeDue The time when the move is due.
     * @return The next move for the ghost.
     */
    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        // Housekeeping - throw out old info
        int currentTick = game.getCurrentLevelTime();
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
        }

        // Can we see Pac-Man? If so, tell people and update our info
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

        // Has anybody else seen Pac-Man if we haven't?
        if (pacmanIndex == -1 && game.getMessenger() != null) {
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) {
                        // Only update if it is newer information
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                }
            }
        }
        if (pacmanIndex == -1) {
            pacmanIndex = lastPacmanIndex;
        }

        Constants.MOVE[] possibleMoves = game.getPossibleMoves(currentIndex, game.getGhostLastMoveMade(ghost));
        if (possibleMoves.length > 0) {
            Boolean requiresAction = game.doesGhostRequireAction(ghost);
            if (requiresAction != null && requiresAction) {
                // System.out.println("[GHOST] Requires action");
                // If ghost requires an action
                if (pacmanIndex != -1) {
                    // If Pac-Man is seen, use Q-learning
                    if (game.getGhostEdibleTime(ghost) > 0 || closeToPower(game)) {
                        // Retreat from Ms. Pac-Man if edible or if Ms. Pac-Man is close to power pill
                        System.out.println("[GHOST] Retreating");
                        try {
                            MOVE move = game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                                    game.getPacmanCurrentNodeIndex(), game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                            return move;
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println(e);
                            System.out.println(pacmanIndex + " : " + currentIndex);
                        }
                    } else {
                        MOVE selectedMove;
                        if (rnd.nextFloat() < EXPLORATION_PROBABILITY) {
                            // Decide if we are going to explore or exploit
                            selectedMove = possibleMoves[rnd.nextInt(possibleMoves.length)];
                            try {
                                printMoveInfo("Exploring", selectedMove, game);
                                return selectedMove;
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.println(e);
                                System.out.println(pacmanIndex + " : " + currentIndex);
                            }
                        } else {
                            selectedMove = getBestMove(currentIndex, possibleMoves);
                        }
                        if (lastMove != null) {
                            // Q-learning update step
                            StateActionPair stateActionPair = new StateActionPair(currentIndex, lastMove);
                            double reward = calculateReward(game);
                            double currentQValue = qValues.getOrDefault(stateActionPair, 0.0);
                            double maxNextQValue = getMaxQValue(game, currentIndex);
                            double updatedQValue = currentQValue
                                    + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQValue - currentQValue);
                            qValues.put(stateActionPair, updatedQValue);
        
                            // System.out.println("Q-learning Update:");
                            // System.out.println(" State-Action Pair: " + stateActionPair.state + " - " +
                            // stateActionPair.action);
                            System.out.println(" [GHOST] Reward: " + reward);
                            // System.out.println(" Current Q-value: " + currentQValue);
                            // System.out.println(" Max Next Q-value: " + maxNextQValue);
                            // System.out.println(" Updated Q-value: " + updatedQValue);
                        }

                        lastMove = selectedMove;
                        return selectedMove;
                    }
                } else {
                    // If Pac-Man is not seen, make a random move
                    return possibleMoves[rnd.nextInt(possibleMoves.length)];
                }
            }
            // displayQTable();
        }

        return null;
    }

    // Get the best move based on Q-values
    private MOVE getBestMove(int current, MOVE[] possibleMoves) {
        double maxQValue = Double.NEGATIVE_INFINITY;
        MOVE bestMove = possibleMoves[0];

        for (MOVE move : possibleMoves) {
            StateActionPair stateActionPair = new StateActionPair(current, move);
            double qValue = qValues.getOrDefault(stateActionPair, 0.0);
            if (qValue > maxQValue) {
                maxQValue = qValue;
                bestMove = move;
            }
        }
        System.out.println("Best move: " + bestMove);
        return bestMove;
    }

    // Get the maximum Q-value for the current state
    private double getMaxQValue(Game game, int current) {
        MOVE[] possibleMoves = game.getPossibleMoves(current, lastMove);
        double maxQValue = Double.NEGATIVE_INFINITY;

        if (possibleMoves != null) {
            for (MOVE move : possibleMoves) {
            StateActionPair stateActionPair = new StateActionPair(current, move);
            double qValue = qValues.getOrDefault(stateActionPair, 0.0);
            if (qValue > maxQValue) {
                maxQValue = qValue;
            }
        }
        }
        

        return maxQValue;
    }

    // Calculate the reward based on the game state
    private double calculateReward(Game game) {
        double livesReward = (3 - game.getPacmanNumberOfLivesRemaining()) * EAT_PACMAN_REWARD;
        double timeStepReward = game.getCurrentLevelTime() * DECAY_REWARD;
        double pacmanEatenPillsPenalty = (game.getNumberOfPills() - game.getNumberOfActivePills()) * PACMAN_EATING_PILLS_PENALTY;
        double eatenGhostsPenalty = game.getNumGhostsEaten() * GHOST_EATEN_PENALTY;
        double levelUpPenalty = game.getCurrentLevel() * LEVEL_UP_PENALTY;

        double reward = (livesReward +
                timeStepReward +
                pacmanEatenPillsPenalty +
                eatenGhostsPenalty +
                levelUpPenalty);
        this.current_reward.add(reward);
        return this.current_reward.get(this.current_reward.size() - 1);
    }

    // Helper method to display Q-table
    public void displayQTable() {
        System.out.println("Q-table:");
        for (Map.Entry<StateActionPair, Double> entry : qValues.entrySet()) {
            StateActionPair stateActionPair = entry.getKey();
            double qValue = entry.getValue();
            System.out.println(stateActionPair.state + " - " + stateActionPair.action + " - " + qValue);
        }
        System.out.println();
    }

    // Helper method to check if Ms. Pac-Man is close to an available power pill
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

    // Represents a state-action pair for Q-learning
    private static class StateActionPair {
        private final int state;
        private final MOVE action;

        public StateActionPair(int state, MOVE action) {
            this.state = state;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateActionPair that = (StateActionPair) o;
            return state == that.state && action == that.action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, action);
        }
    }
}
