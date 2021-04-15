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

package simblock.node;

import simblock.block.Block;
import simblock.block.PoWGhostBlock;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.routing.AbstractRoutingTable;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;

import static simblock.simulator.Simulator.arriveBlock;
import static simblock.simulator.Timer.getCurrentTime;
import static simblock.simulator.Timer.removeTask;

/**
 * A class representing a node in the network.
 */
public class GhostNode extends Node {
  /**
   * Orphaned blocks known to node.
   */
  private final List<PoWGhostBlock> orphans = new ArrayList<>();

  /**
   * Instantiates a new Node.
   *
   * @param nodeID            the node id
   * @param numConnection     the number of connections a node can have
   * @param region            the region
   * @param miningPower       the mining power
   * @param routingTableName  the routing table name
   * @param consensusAlgoName the consensus algorithm name
   * @param useCBR            whether the node uses compact block relay
   * @param isChurnNode       whether the node causes churn
   */
  public GhostNode(
      int nodeID, int numConnection, int region, long miningPower, String routingTableName,
      String consensusAlgoName, boolean useCBR, boolean isChurnNode
  ) {
    super( nodeID,  numConnection,  region,  miningPower,  routingTableName,
            consensusAlgoName,  useCBR,  isChurnNode);
  }
  /**
   * Generates a uncle
   */
  public Block generateUncleA() {
    return this.orphans.get(this.orphans.size()-1);
  }
  public Block generateUncleB() {
    return this.orphans.get(this.orphans.size()-2);
  }

  /**
   * Adds a new block to the to chain. If node was minting that task instance is abandoned, and
   * the new block arrival is handled.
   *
   * @param newBlock the new block
   */
  public void addToChain(PoWGhostBlock newBlock) {
    // If the node has been minting
    if (this.mintingTask != null) {
      removeTask(this.mintingTask);
      this.mintingTask = null;
    }
    // Update the current block
    this.block = newBlock;
    this.orphans.remove(newBlock.getUncleA());
    this.orphans.remove(newBlock.getUncleB());

    printAddBlock(newBlock);
    // Observe and handle new block arrival
    arriveBlock(newBlock, this);
  }
  /**
   * Receive block.
   *
   * @param block the block
   */
  public void receiveBlock(Block block) {
    if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
      if (this.block != null && !this.block.isOnSameChainAs(block)) {
        // If orphan mark orphan
        this.addOrphans(this.block, block);
      }
      // Else add to canonical chain
      this.addToChain(block);
      // Generates a new minting task
      this.minting();
      // Advertise received block
      this.sendInv(block);
    } else if (!this.orphans.contains(block) && !block.isOnSameChainAs(this.block)) {
      // TODO better understand - what if orphan is not valid?
      // If the block was not valid but was an unknown orphan and is not on the same chain as the
      // current block
      this.addOrphans(block, this.block);
      arriveBlock(block, this);
    }
  }

}
