/*
 * Q-learning Pac-Man Controller
 *
 * This Pac-Man controller uses Q-learning, a model-free reinforcement learning algorithm,
 * to make decisions on the best moves in a given game state. Q-learning learns a policy
 * that tells the agent what action to take under what circumstances to maximize the cumulative
 * reward over time. The algorithm maintains a Q-value table, where each entry represents the
 * expected cumulative reward for a state-action pair. During gameplay, the Q-values are updated
 * based on the agent's experiences and rewards received. The agent then selects actions based
 * on a balance between exploration (trying new actions) and exploitation (choosing known
 * high-reward actions).
 *
 * Strategies implemented:
 * 1. Evading ghosts that are not edible and too close.
 * 2. Hunting the nearest edible ghost.
 * 3. Going after visible pills and power pills.
 * 4. Exploring randomly or exploiting the best-known action based on Q-values.
 *
 * Q-learning Parameters:
 * - LEARNING_RATE: Controls the extent to which the agent updates its Q-values.
 * - DISCOUNT_FACTOR: Represents the importance of future rewards in the decision-making process.
 * - EXPLORATION_PROBABILITY: Probability of choosing a random action for exploration.
 *
 * Q-value update equation: Q(s, a) = Q(s, a) + alpha * [R + gamma * max Q(s', a') - Q(s, a)]
 * where:
 *   alpha is the learning rate (LEARNING_RATE),
 *   gamma is the discount factor (DISCOUNT_FACTOR),
 *   R is the observed reward,
 *   max Q(s', a') is the maximum Q-value for the next state,
 *   Q(s, a) is the current Q-value.
 * 
 * 
 * Note: Fine-tune parameters and reward functions for optimal performance in a specific Pac-Man scenario.
 *
 * Implemented By: Waiz Wafiq (17203410/2)
 */

package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Q_RL extends PacmanController {
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_PROBABILITY = 1;

    private Random random = new Random();
    private Map<StateActionPair, Double> qValues = new HashMap<>();
    private MOVE lastMove;

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();

        // System.out.println(current);

        // Strategy 1: Adjusted for PO (Pill Observations)
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
                int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
                if (ghostLocation != -1) {
                    if (game.getShortestPathDistance(current, ghostLocation) < 20) {
                        // Evade the ghost
                        return game.getNextMoveAwayFromTarget(current, ghostLocation, Constants.DM.PATH);
                    }
                }
            }
        }

        /// Strategy 2: Find nearest edible ghost and go after them
        int minDistance = Integer.MAX_VALUE;
        Constants.GHOST minGhost = null;
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) > 0) {
                int distance = game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(ghost));
                if (distance < minDistance) {
                    minDistance = distance;
                    minGhost = ghost;
                }
            }
        }

        if (minGhost != null) {
            // Hunt the nearest edible ghost
            return game.getNextMoveTowardsTarget(current, game.getGhostCurrentNodeIndex(minGhost), Constants.DM.PATH);
        }

        // Strategy 3: Go after the pills and power pills that we can see
        int[] pills = game.getPillIndices();
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < pills.length; i++) {
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                // Move towards the nearest visible pill
                return game.getNextMoveTowardsTarget(current, pills[i], Constants.DM.PATH);
            }
        }

        for (int i = 0; i < powerPills.length; i++) {
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                // Move towards the nearest visible power pill
                return game.getNextMoveTowardsTarget(current, powerPills[i], Constants.DM.PATH);
            }
        }

        // Strategy 4: New PO strategy as now S3 can fail if nothing you can see
        // Going to pick a random action here
        MOVE[] possibleMoves = game.getPossibleMoves(current, lastMove);

        if (possibleMoves.length > 0) {
            MOVE selectedMove;
            if (random.nextDouble() < EXPLORATION_PROBABILITY) {
                // Exploration: choose a random move
                selectedMove = possibleMoves[random.nextInt(possibleMoves.length)];
                System.out.println("Exploration: Choosing a random move - " + selectedMove);
            } else {
                // Exploitation: choose the move with the highest Q-value
                selectedMove = getBestMove(current, possibleMoves);
                System.out.println("Exploitation: Choosing the best move based on Q-values - " + selectedMove);
            }

            if (lastMove != null) {
                // Q-learning update step
                StateActionPair stateActionPair = new StateActionPair(current, lastMove);
                double reward = calculateReward(game);
                double currentQValue = qValues.getOrDefault(stateActionPair, 0.0);
                double maxNextQValue = getMaxQValue(game, current);
                double updatedQValue = currentQValue
                        + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQValue - currentQValue);
                qValues.put(stateActionPair, updatedQValue);

                // System.out.println("Q-learning Update:");
                // System.out.println("   State-Action Pair: " + stateActionPair.state + " - " + stateActionPair.action);
                // System.out.println("   Reward: " + reward);
                // System.out.println("   Current Q-value: " + currentQValue);
                // System.out.println("   Max Next Q-value: " + maxNextQValue);
                // System.out.println("   Updated Q-value: " + updatedQValue);
            }

            lastMove = selectedMove;
            return selectedMove;
        }

        // Must be possible to turn around
        return game.getPacmanLastMoveMade().opposite();
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

        return bestMove;
    }

    // Get the maximum Q-value for the current state
    private double getMaxQValue(Game game, int current) {
        MOVE[] possibleMoves = game.getPossibleMoves(current, lastMove);
        double maxQValue = Double.NEGATIVE_INFINITY;

        for (MOVE move : possibleMoves) {
            StateActionPair stateActionPair = new StateActionPair(current, move);
            double qValue = qValues.getOrDefault(stateActionPair, 0.0);
            if (qValue > maxQValue) {
                maxQValue = qValue;
            }
        }

        return maxQValue;
    }

    // Calculate the reward based on the game state
    private double calculateReward(Game game) {
        // Implement your own reward function based on the game state
        // Example: +1 for eating a pill, -1 for getting caught by a ghost, etc.
        // Return 0 for now (no immediate reward)
        return 0;
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
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            StateActionPair that = (StateActionPair) o;
            return state == that.state && action == that.action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, action);
        }
    }
}
