///*
// * Copyright 2019 Distributed Systems Group
// *
// * <p>Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * <p>http://www.apache.org/licenses/LICENSE-2.0
// *
// * <p>Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package simblock.node.NodeRepository;
//
//import simblock.block.Block;
//import simblock.block.PoWGhostBlock;
//import simblock.node.Node;
//import simblock.node.consensus.AbstractConsensusAlgo;
//import simblock.task.*;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//import static simblock.settings.SimulationConfiguration.CBR_FAILURE_RATE_FOR_CHURN_NODE;
//import static simblock.settings.SimulationConfiguration.CBR_FAILURE_RATE_FOR_CONTROL_NODE;
//import static simblock.simulator.Simulator.arriveBlock;
//import static simblock.simulator.Timer.putTask;
//import static simblock.simulator.Timer.removeTask;
//
///**
// * A class representing a node in the network.
// */
//public class GHOSTNode extends Node {
//  /**
//   * Orphaned blocks known to node.
//   */
//  private final List<PoWGhostBlock> orphans = new ArrayList<>();
//  /**
//   * The consensus algorithm used by the node.
//   */
//  protected AbstractConsensusAlgo consensusAlgo;
//
//  /**
//   * Instantiates a new Node.
//   *
//   * @param nodeID            the node id
//   * @param numConnection     the number of connections a node can have
//   * @param region            the region
//   * @param miningPower       the mining power
//   * @param routingTableName  the routing table name
//   * @param consensusAlgoName the consensus algorithm name
//   * @param useCBR            whether the node uses compact block relay
//   * @param isChurnNode       whether the node causes churn
//   */
//  public GHOSTNode(int nodeID, int numConnection, int region, long miningPower, String routingTableName,
//                   String consensusAlgoName, boolean useCBR, boolean isChurnNode)
//  {
//    super( nodeID,  numConnection,  region,  miningPower,  routingTableName,
//            "simblock.node.consensus.ProofOfWork",  useCBR,  isChurnNode);
//
//    try {
//      this.consensusAlgo = (AbstractConsensusAlgo) Class.forName(consensusAlgoName).getConstructor(
//              GHOSTNode.class).newInstance(this);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  /**
//   * Generates a uncle
//   */
//  public Block generateUncleA() {
//    if(this.orphans.isEmpty())
//      return null;
//    return this.orphans.get(this.orphans.size()-1);
//  }
//  public Block generateUncleB() {
//    if(this.orphans.isEmpty())
//      return null;
//    return this.orphans.get(this.orphans.size()-2);
//  }
//  /**
//   * Generates a new minting task and registers it
//   */
//  public void minting() {
//    AbstractMintingTask task = this.consensusAlgo.minting();
//    this.mintingTask = task;
//    if (task != null) {
//      putTask(task);
//    }
//  }
//  /**
//   * Adds a new block to the to chain. If node was minting that task instance is abandoned, and
//   * the new block arrival is handled.
//   *
//   * @param newBlock the new block
//   */
//  public void addToChain(Block newBlock) {
//    // If the node has been minting
//    if (this.mintingTask != null) {
//      removeTask(this.mintingTask);
//      this.mintingTask = null;
//    }
//    // Update the current block
//    this.block = newBlock;
//
//
//    printAddBlock(newBlock);
//    // Observe and handle new block arrival
//    arriveBlock(newBlock, this);
//  }
//  /**
//   * Receive block.
//   *
//   * @param block the block
//   */
//  public void receiveBlock(PoWGhostBlock block) {
//    if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
//      if (this.block != null && !this.block.isOnSameChainAs(block)) {
//        // If orphan mark orphan
//        this.addOrphans(this.block, block);
//      }
//      // Else add to canonical chain
//      this.addToChain(block);
//      this.orphans.remove(block.getUncleA());
//      this.orphans.remove(block.getUncleB());
//      // Generates a new minting task
//      this.minting();
//      // Advertise received block
//      this.sendInv(block);
//    } else if (!this.orphans.contains(block) && !block.isOnSameChainAs(this.block)) {
//      // TODO better understand - what if orphan is not valid?
//      // If the block was not valid but was an unknown orphan and is not on the same chain as the
//      // current block
//      this.addOrphans(block, this.block);
//      arriveBlock(block, this);
//    }
//  }
//  /**
//   * Mint the genesis block.
//   */
//  public void genesisBlock() {
//    PoWGhostBlock genesis = (PoWGhostBlock) this.consensusAlgo.genesisBlock();
//    this.receiveBlock(genesis);
//  }
//  public AbstractConsensusAlgo getConsensusAlgo() {
//    return this.consensusAlgo;
//  }
//  /**
//   * Receive message.
//   *
//   * @param message the message
//   */
//  public void receiveMessage(AbstractMessageTask message) {
//    Node from = message.getFrom();
//
//    if (message instanceof InvMessageTask) {
//      Block block = ((InvMessageTask) message).getBlock();
//      if (!this.orphans.contains(block) && !this.downloadingBlocks.contains(block)) {
//        if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
//          AbstractMessageTask task = new RecMessageTask(this, from, block);
//          putTask(task);
//          downloadingBlocks.add(block);
//        } else if (!block.isOnSameChainAs(this.block)) {
//          // get new orphan block
//
//          AbstractMessageTask task = new RecMessageTask(this, from, block);
//          putTask(task);
//          downloadingBlocks.add(block);
//        }
//      }
//    }
//
//    if (message instanceof RecMessageTask) {
//      this.messageQue.add((RecMessageTask) message);
//      if (!sendingBlock) {
//        this.sendNextBlockMessage();
//      }
//    }
//
//    if(message instanceof GetBlockTxnMessageTask){
//      this.messageQue.add((GetBlockTxnMessageTask) message);
//      if(!sendingBlock){
//        this.sendNextBlockMessage();
//      }
//    }
//
//    if(message instanceof CmpctBlockMessageTask){
//      Block block = ((CmpctBlockMessageTask) message).getBlock();
//      Random random = new Random();
//      float CBRfailureRate = this.isChurnNode ? CBR_FAILURE_RATE_FOR_CHURN_NODE : CBR_FAILURE_RATE_FOR_CONTROL_NODE;
//      boolean success = random.nextDouble() > CBRfailureRate ? true : false;
//      if(success){
//        downloadingBlocks.remove(block);
//        this.receiveBlock(block);
//      }else{
//        AbstractMessageTask task = new GetBlockTxnMessageTask(this, from, block);
//        putTask(task);
//      }
//    }
//
//    if (message instanceof BlockMessageTask) {
//      Block block = ((BlockMessageTask) message).getBlock();
//      downloadingBlocks.remove(block);
//      this.receiveBlock(block);
//    }
//  }
//
//
//}
//
//
