/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.simulator;

import simblock.block.Block;
import simblock.node.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static simblock.settings.SimulationConfiguration.*;
import static simblock.simulator.Main.PROPAGATION_TEXT_FILE;
import static simblock.simulator.Main.STATIC_JSON_FILE;
import static simblock.simulator.Timer.getCurrentTime;


/**
 * The type Simulator is tasked with maintaining the list of simulated nodes and managing the
 * block interval. It observes and manages the arrival of new blocks at the simulation level.
 */
public class Simulator {

  /**
   * A list of nodes that will be used in a simulation.
   */
  private static final ArrayList<Node> simulatedNodes = new ArrayList<>();

  /**
   * The target block interval in milliseconds.
   */
  private static long targetInterval;


  /**
   * Get simulated nodes list.
   *
   * @return the array list
   */
  public static ArrayList<Node> getSimulatedNodes() {
    return simulatedNodes;
  }

  /**
   * Get target block interval.
   *
   * @return the target block interval in milliseconds
   */
  public static long getTargetInterval() {
    return targetInterval;
  }

  /**
   * Sets the target block interval.
   *
   * @param interval - block interval in milliseconds
   */
  public static void setTargetInterval(long interval) {
    targetInterval = interval;
  }

  /**
   * Add node to the list of simulated nodes.
   *
   * @param node the node
   */
  public static void addNode(Node node) {
    simulatedNodes.add(node);
  }

  /**
   * Remove node from the list of simulated nodes.
   *
   * @param node the node
   */
  public static void removeNode(Node node) {
    simulatedNodes.remove(node);
  }

  /**
   * Clear node from the list of simulated nodes.
   *
   *
   */
  public static void clearNode() {
    while(simulatedNodes.size() != 0)
      simulatedNodes.remove(0);
  }

  /**
   * Add node to the list of simulated nodes and immediately try to add the new node as a
   * neighbor to all simulated
   * nodes.
   *
   * @param node the node
   */
  @SuppressWarnings("unused")
  public static void addNodeWithConnection(Node node) {
    node.joinNetwork();
    addNode(node);
    for (Node existingNode : simulatedNodes) {
      existingNode.addNeighbor(node);
    }
  }

  /**
   * A list of observed {@link Block} instances.
   */
  private static final ArrayList<Block> observedBlocks = new ArrayList<>();

  /**
   * A list of observed block propagation times. The map key represents the id of the node that
   * has seen the
   * block, the value represents the difference between the current time and the block minting
   * time, effectively
   * recording the absolute time it took for a node to witness the block.
   */
  private static final ArrayList<LinkedHashMap<Integer, Long>> observedPropagations =
          new ArrayList<>();

  /**
   * Handle the arrival of a new block. For every observed block, propagation information is
   * updated, and for a new
   * block propagation information is created.
   *
   * @param block the block
   * @param node  the node
   */
  public static void arriveBlock(Block block, Node node) {
    // 记录区块抵达节点的传播时间

    // If block is already seen by any node
    if (observedBlocks.contains(block)) {
      // Get the propagation information for the current block
      LinkedHashMap<Integer, Long> propagation = observedPropagations.get(
              observedBlocks.indexOf(block)
      );
      // Update information for the new block
      propagation.put(node.getNodeID(), getCurrentTime() - block.getTime());
    } else {
      // If the block has not been seen by any node and there is ||no memory|| allocated
      // TODO move magic number to constant
      if ((MEMORYSAVEMODE)&(observedBlocks.size() > 0)) {
        // After the observed blocks limit is reached, log and remove old blocks by FIFO principle
        // Now there is no limit
        printPropagation(observedBlocks.get(0), observedPropagations.get(0));
        observedBlocks.remove(0);
        observedPropagations.remove(0);
      }
      // If the block has not been seen by any node and there is additional memory
      LinkedHashMap<Integer, Long> propagation = new LinkedHashMap<>();
      // propagation   (节点ID，该节点收到该区块的时间：区别于接受区块)
      propagation.put(node.getNodeID(), getCurrentTime() - block.getTime());
      // Record the block as seen
      observedBlocks.add(block);
      // Record the propagation time
      observedPropagations.add(propagation);//propagation 存在该结构中，索引为区块对象地址
    }
  }

  /**
   * Print propagation information about the propagation of the provided block  in the format:
   *
   * <p><em>node_ID, propagation_time</em>
   *
   * <p><em>propagation_time</em>: The time from when the block of the block ID is generated to
   * when the
   * node of the <em>node_ID</em> is reached.
   *
   * @param block       the block
   * @param propagation the propagation of the provided block as a list of {@link Node} IDs and
   *                    propagation times
   */
  public static void printPropagation(Block block, LinkedHashMap<Integer, Long> propagation) {
    // Print block and its height
    // TODO block does not have a toString method, what is printed here
    //    PROPAGATION_TEXT_FILE.print(block + ":" + block.getHeight() + '\n');
    for (Map.Entry<Integer, Long> timeEntry : propagation.entrySet()) {
      PROPAGATION_TEXT_FILE.print(timeEntry.getKey() + "," + timeEntry.getValue() + '\n');
    }
    //    PROPAGATION_TEXT_FILE.print('\n');
    PROPAGATION_TEXT_FILE.flush();

  }
  /**
   * Prints the currently active regions to outfile.
   */
  //TODO
  public static void printNetProtocolSetting() {
    STATIC_JSON_FILE.print("{\"NetProtocolSetting\":[");
    STATIC_JSON_FILE.print("{");
    STATIC_JSON_FILE.print("\"NUM_OF_NODES\":" + NUM_OF_NODES + ",\n");
    STATIC_JSON_FILE.print("\"END_BLOCK_HEIGHT\":" + END_BLOCK_HEIGHT + ",\n");
    STATIC_JSON_FILE.print("\"BLOCK_SIZE\":" + BLOCK_SIZE + ",\n");
    STATIC_JSON_FILE.print("\"NOBANDWITTHREDUCTION\":" + NOBANDWITTHREDUCTION + ",\n");
    STATIC_JSON_FILE.print("\"NOEXTRA\":" + NOEXTRA + ",\n");
    STATIC_JSON_FILE.print("\"INISITMODE\":" + INISITMODE + ",\n");
    STATIC_JSON_FILE.print("\"ALGO\":" + ALGO + ",\n");
    STATIC_JSON_FILE.print("\"INTERVAL\":" + INTERVAL + ",\n");
    STATIC_JSON_FILE.print("\"CBR_USAGE_RATE\":" + CBR_USAGE_RATE + ",\n");
    STATIC_JSON_FILE.print("\"CHURN_NODE_RATE\":" + CHURN_NODE_RATE + ",\n");
    STATIC_JSON_FILE.print("\"COMPACT_BLOCK_SIZE\":" + COMPACT_BLOCK_SIZE + ",\n");
    STATIC_JSON_FILE.print("\"CBR_FAILURE_RATE_FOR_CONTROL_NODE\":" + CBR_FAILURE_RATE_FOR_CONTROL_NODE + ",\n");
    STATIC_JSON_FILE.print("\"CBR_FAILURE_RATE_FOR_CHURN_NODE\":" + CBR_FAILURE_RATE_FOR_CHURN_NODE );
    STATIC_JSON_FILE.print("}]}\n");
    STATIC_JSON_FILE.flush();
  }

  /**
   * Print propagation information about all blocks, internally relying on
   * {@link Simulator#printPropagation(Block, LinkedHashMap)}.
   */
  public static void printAllPropagation(ArrayList<Block> blockList, Set<Block> orphans) {
    PROPAGATION_TEXT_FILE.print("printAllPropagation\n");
    for (Block b : blockList) {
      if (!orphans.contains(b)) {
        PROPAGATION_TEXT_FILE.print("onchain\n");
      }
      else {
        PROPAGATION_TEXT_FILE.print("orphan\n");
      }
      printPropagation(b, observedPropagations.get(observedBlocks.indexOf(b)));
    }

//    for (int i = 0; i < observedBlocks.size(); i++) {
//      printPropagation(observedBlocks.get(i), observedPropagations.get(i));
//    }
  }
}
