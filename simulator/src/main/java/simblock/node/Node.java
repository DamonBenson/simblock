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

import static simblock.settings.SimulationConfiguration.*;
import static simblock.simulator.Main.GHOST_USE_MODE;
import static simblock.simulator.Main.OUT_JSON_FILE;
import static simblock.simulator.Network.getBandwidth;
import static simblock.simulator.SimulateRandomEvent.processingTimeExtra;
import static simblock.simulator.Simulator.arriveBlock;
import static simblock.simulator.Timer.getCurrentTime;
import static simblock.simulator.Timer.putTask;
import static simblock.simulator.Timer.removeTask;

import java.util.*;

import simblock.block.Block;
import simblock.block.GHOSTBlock;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.routing.AbstractRoutingTable;
import simblock.task.AbstractMessageTask;
import simblock.task.AbstractMintingTask;
import simblock.task.BlockMessageTask;
import simblock.task.CmpctBlockMessageTask;
import simblock.task.GetBlockTxnMessageTask;
import simblock.task.InvMessageTask;
import simblock.task.RecMessageTask;

/**
 * A class representing a node in the network.
 */
public class Node {
  /**
   * Unique node ID.
   */
  protected final int nodeID;

  /**
   * Region assigned to the node.
   */
  protected final int region;

  /**
   * Mining power assigned to the node.
   * Which can Boost by the time minted.
   */
  protected long miningPower;

  /**
   * Mining power waste in compete.
   */
  // TODO verify
  protected long miningPowerWaste;

  /**
   * Coin that the Node has been rewarded.
   */
  // TODO verify
  protected float balance;

  /**
   * currentNetConnection, when currentNetConnection boost
   * local host may get overwhelmed
   */
  // TODO verify
  protected int currentNetConnection;



  /**
   * A nodes routing table.
   */
  protected AbstractRoutingTable routingTable;

  /**
   * The consensus algorithm used by the node.
   */
  protected AbstractConsensusAlgo consensusAlgo;

  /**
   * Whether the node uses compact block relay.
   * Hope Mind can't Change,so it's final
   */
  protected final boolean useCBR;

  /**
   * The node causes churn.
   * Hope Mind can't Change,so it's final
   */
  protected final boolean isChurnNode;

  /**
   * The current block.
   */
  protected Block block;
  /**
   * Region assigned to the node.
   */
  protected int receiveOrphanCount = 0;

  /**
   * Orphaned blocks known to node.
   */
  private final Set<Block> orphans = new HashSet<>();
  /**
   * uncleCandidate
   */
  private final LinkedList<Block> uncleCandidate = new LinkedList<>();



  /**
   * The current minting task
   */
  protected AbstractMintingTask mintingTask = null;
  //******************@Process Status@******************//
  //TODO Node own it's status
  /**
   * time bring trouble.
   */
  protected long AliveTime ;
  /**
   * @sendingBlock    In the process of sending blocks.
   * @localBusying    In busy with local thread
   * @networkBusying  network are In busy
   * @crashed         Crashed
   */
  // TODO verify
  protected boolean sendingBlock = false;
  protected boolean localBusying = false;
  protected boolean networkBusying = false;
  protected boolean crashed = false;
  //******************@Process Status@******************//

  //TODO
  protected final ArrayList<AbstractMessageTask> messageQue = new ArrayList<>();
  // TODO
  protected final Set<Block> downloadingBlocks = new HashSet<Block>();

  /**
   * Processing time of tasks expressed in milliseconds.
   * average ,while in simulation ,processed error will consume more time.
   * 3、4 in local busy, rand(300) when network busy ,30000 in crashed.
   */
  // TODO verify
  protected final long processingTime = 2;

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
  public Node(
      int nodeID, int numConnection, int region, long miningPower, String routingTableName,
      String consensusAlgoName, boolean useCBR, boolean isChurnNode
  ) {
    this.nodeID = nodeID;
    this.region = region;
    this.miningPower = miningPower;
    this.useCBR = useCBR;
    this.isChurnNode = isChurnNode;
    this.balance = 0;
    this.miningPowerWaste = 0;
    this.AliveTime = getCurrentTime();

    try {
      this.routingTable = (AbstractRoutingTable) Class.forName(routingTableName).getConstructor(
          Node.class).newInstance(this);
      this.consensusAlgo = (AbstractConsensusAlgo) Class.forName(consensusAlgoName).getConstructor(
              Node.class).newInstance(this);
      this.setNumConnection(numConnection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /**
   * Generates a uncle
   */
  public Block generateUncleA() {
    if(this.uncleCandidate.isEmpty())
      return null;
    return this.uncleCandidate.get(this.uncleCandidate.size()-1);
  }
  public Block generateUncleB() {
    if(this.uncleCandidate.size()<2)
      return null;
    return this.uncleCandidate.get(this.uncleCandidate.size()-2);
  }
  /**
   * Gets the node id.
   *
   * @return the node id
   */
  public int getNodeID() {
    return this.nodeID;
  }

  /**
   * Gets the region ID assigned to a node.
   *
   * @return the region
   */
  public int getRegion() {
    return this.region;
  }

  /**
   * Gets mining power.
   *
   * @return the mining power
   */
  public long getMiningPower() { return this.miningPower; }

  /**
   * Gets mining power.
   *
   * @return the mining power that waste in compete
   */
  public long getMiningPowerWaste() {
    return this.miningPowerWaste;
  }

  /**
   * Gets mining power.
   *
   * @return the balance
   */
  public float getBalance() { return this.balance; }

  /**
   * Gets the consensus algorithm.
   *
   * @return the consensus algorithm. See {@link AbstractConsensusAlgo}
   */
  @SuppressWarnings("unused")
  public AbstractConsensusAlgo getConsensusAlgo() {
    return this.consensusAlgo;
  }

  /**
   * Gets routing table.
   *
   * @return the routing table
   */
  public AbstractRoutingTable getRoutingTable() {
    return this.routingTable;
  }

  /**
   * Gets the current block.
   *
   * @return the block
   */
  public Block getBlock() {
    return this.block;
  }

  /**
   * Gets all orphans known to node.
   *
   * @return the orphans
   */
  public Set<Block> getOrphans() {
    return this.orphans;
  }

  /**
   * Gets all uncleCandidate known to node.
   *
   * @return the uncleCandidate
   */
  public LinkedList<Block> getUncleCandidate() {
    return this.uncleCandidate;
  }

  /**
   * Gets the number of connections a node can have.
   *
   * @return the number of connection
   */
  @SuppressWarnings("unused")
  public int getNumConnection() {
    return this.routingTable.getNumConnection();
  }

  /**
   * Sets the number of connections a node can have.
   *
   * @param numConnection the n connection
   */
  public void setNumConnection(int numConnection) {
    this.routingTable.setNumConnection(numConnection);
  }

  /**
   * Gets the nodes neighbors.
   *
   * @return the neighbors
   */
  public ArrayList<Node> getNeighbors() {
    return this.routingTable.getNeighbors();
  }

  /**
   * Adds the node as a neighbor.
   *
   * @param node the node to be added as a neighbor
   * @return the success state of the operation
   */
  @SuppressWarnings("UnusedReturnValue")
  public boolean addNeighbor(Node node) {
    return this.routingTable.addNeighbor(node);
  }

  /**
   * Removes the neighbor form the node.
   *
   * @param node the node to be removed as a neighbor
   * @return the success state of the operation
   */
  @SuppressWarnings("unused")
  public boolean removeNeighbor(Node node) {
    return this.routingTable.removeNeighbor(node);
  }

  /**
   * Initializes the routing table.
   */
  public void joinNetwork() {
    this.routingTable.initTable();
  }

  /**
   * Mint the genesis block.
   */
  public void genesisBlock() {
    Block genesis = this.consensusAlgo.genesisBlock();
    this.receiveBlock(genesis);
  }

  /**
   * Adds a new block to the to chain. If node was minting that task instance is abandoned, and
   * the new block arrival is handled.
   *
   * @param newBlock the new block
   */
  public void addToChain(GHOSTBlock newBlock) {
    // If the node has been minting
    if (this.mintingTask != null) {
      removeTask(this.mintingTask);
      this.mintingTask = null;
    }
    // Update the current block
    this.block = newBlock;
    printAddBlock(newBlock);
    // Observe and handle new block arrival
    if(this.uncleCandidate.isEmpty())
      return;
    this.uncleCandidate.remove(newBlock.getUncleA());
    if(this.uncleCandidate.isEmpty())
      return;
    this.uncleCandidate.remove(newBlock.getUncleB());
  }
  /**
   * Adds a new block to the to chain. If node was minting that task instance is abandoned, and
   * the new block arrival is handled.
   *
   * @param newBlock the new block
   */
  public void addToChain(Block newBlock) {
    // If the node has been minting
    if (this.mintingTask != null) {
      removeTask(this.mintingTask);
      this.mintingTask = null;
    }
    // Update the current block
    this.block = newBlock;
    printAddBlock(newBlock);
    // Observe and handle new block arrival
  }

  /**
   * Logs the provided block to the logfile.
   *
   * @param newBlock the block to be logged
   */
  protected void printAddBlock(Block newBlock) {
    if(!PRINTADDBLOCK)
      return;
    OUT_JSON_FILE.print("{");
    OUT_JSON_FILE.print("\"kind\":\"add-block\",");
    OUT_JSON_FILE.print("\"content\":{");
    OUT_JSON_FILE.print("\"timestamp\":" + getCurrentTime() + ",");
    OUT_JSON_FILE.print("\"node-id\":" + this.getNodeID() + ",");
    OUT_JSON_FILE.print("\"block-id\":" + newBlock.getId());
    OUT_JSON_FILE.print("}");
    OUT_JSON_FILE.print("},");
    OUT_JSON_FILE.flush();
  }
  /**
   * to figure out why LinkedList can't like set
   * TODO
   */
  public boolean checkUncleCandidate() {
    for(int checkIndex = uncleCandidate.size()-1 ; checkIndex > -1 ; checkIndex--){
      for(int index = uncleCandidate.size()-1 ; index > -1 ; index--){
        if(uncleCandidate.get(checkIndex).equals(uncleCandidate.get(index))){
          return true;
        }
      }
    }
    return false;
    // eg
    //    if(checkUncleCandidate()){
    //      System.out.println("Error checkUncleCandidate IS TRUE");
    //    }
  }

  /**
   * nominate Uncle Candidates
   */
  public void nominateUncleCandidate(Block orphanBlock, Block validBlock) {
    // TODO 7 Height to deNominate
//    if(MEMORYSAVEMODE) {
//      while (uncleCandidate.size() > 15) {
//        uncleCandidate.getFirst();
//      }
//    }
    int IndexMax = uncleCandidate.size() - 1;
    int selectedIndex = IndexMax;

    if(IndexMax == -1){
      uncleCandidate.add(orphanBlock);
      return;// 完成推举了
    } else{
      //最长的最先，先来的先招安
      while(selectedIndex >= 0){
        //找到高度
        if(uncleCandidate.get(selectedIndex).getHeight() <= orphanBlock.getHeight()) {//孤块高度
          break;
        }
        selectedIndex -- ;
      }

      if(selectedIndex == IndexMax){//最长
        if(uncleCandidate.get(selectedIndex).equals(orphanBlock)) {
          return;// 已经推举了
        }
        uncleCandidate.add(orphanBlock);
        return;// 完成推举了
      }

      while(selectedIndex >= 0){
        // 找到位置插入
        if(uncleCandidate.get(selectedIndex).equals(orphanBlock)){
          return;// 已经推举了
        }
        if(uncleCandidate.get(selectedIndex).getHeight() < orphanBlock.getHeight()){
          uncleCandidate.add(selectedIndex + 1,orphanBlock);
          return;// 完成推举了
        }
        selectedIndex -- ;
      }
      uncleCandidate.add(0,orphanBlock);
    }

    System.out.println(uncleCandidate.getLast().getHeight() + "\"HEIGHT\":" + uncleCandidate.getFirst().getHeight());

  }

  /**
   * Add orphans.
   *
   * @param orphanBlock the orphan block  作为孤块
   * @param validBlock  the valid block   作为合法块
   */
  //TODO check this out later
  public void addOrphans(Block orphanBlock, Block validBlock) {
    if (orphanBlock != validBlock) {
      // TODO Abandon Design
      // receiveOrphanCount += 1;

      this.orphans.add(orphanBlock);
      this.orphans.remove(validBlock);
      if (validBlock == null || orphanBlock.getHeight() > validBlock.getHeight()) {//
        this.addOrphans(orphanBlock.getParent(), validBlock); // 发现分叉
      } else if (orphanBlock.getHeight() == validBlock.getHeight()) {
        this.addOrphans(orphanBlock.getParent(), validBlock.getParent()); // 发现竞争分叉
      } else {//合法链更长 开始承认合法链
        this.addOrphans(orphanBlock, validBlock.getParent());
      }
    }
  }
  /**
   * Add orphans.
   *
   * @param orphanBlock the orphan block  作为孤块
   * @param validBlock  the valid block   作为合法块
   */
  //TODO check this out later
  public void addOrphans(GHOSTBlock orphanBlock, GHOSTBlock validBlock) {
    if (orphanBlock != validBlock) {
      // TODO Abandon Design
      // receiveOrphanCount += 1;

      this.orphans.add(orphanBlock);
      this.orphans.remove(validBlock);
      this.uncleCandidate.remove(validBlock);
      nominateUncleCandidate(orphanBlock,validBlock);

      if (validBlock == null || orphanBlock.getHeight() > validBlock.getHeight()) {//
        this.addOrphans(orphanBlock.getParent(), validBlock); // 发现分叉
      } else if (orphanBlock.getHeight() == validBlock.getHeight()) {
        this.addOrphans(orphanBlock.getParent(), validBlock.getParent()); // 发现竞争分叉
      } else {//合法链更长 开始承认合法链
        this.addOrphans(orphanBlock, validBlock.getParent());
      }
    }
  }
  /**
   * Generates a new minting task and registers it
   */
  public void minting() {
    AbstractMintingTask task = this.consensusAlgo.minting();
    this.mintingTask = task;
    if (task != null) {
      putTask(task);
    }
  }

  /**
   * Send inv.
   *
   * @param block the block
   */
  public void sendInv(Block block) {
    for (Node to : this.routingTable.getNeighbors()) {
      AbstractMessageTask task = new InvMessageTask(this, to, block);
      putTask(task);
    }
  }
  /**
   * surrender and fake own block.
   *
   * @param block the block
   */
  private void surrender(GHOSTBlock block){
    // 招安成功
    if (block.getUncleA() == this.block && block.getUncleB() == this.block) {
      System.out.println("Thanks,I give in" + "\tcurrentBlockHeight:" + block.getHeight() + "\tMineBlockHeight:" + this.block.getHeight());
    }
    else{
      System.out.println("Yes,I surrender" + "\tcurrentBlockHeight:" + block.getHeight() + "\tMineBlockHeight:" + this.block.getHeight());
    }

    this.addOrphans((GHOSTBlock)this.block, (GHOSTBlock)block);
    acceptBlock((GHOSTBlock)block);
  }
  /**
   * surrender and fake own block.
   *
   * @param block the block
   */
  private void surrender(Block block){
    System.out.println("Yes,I surrender" + "\tcurrentBlockHeight:" + block.getHeight() + "\tMineBlockHeight:" + this.block.getHeight());

    this.addOrphans(this.block, block);
    acceptBlock(block);
  }
  /**
   * giveIn and fake own block.
   *
   * @param block the block
   */
  private boolean giveIn(GHOSTBlock block){
    // if(block.getUncleA() != null)
    //   System.out.println("block.getUncleA():" + block.getUncleA());
    // if(block.getUncleB() != null)
    //   System.out.println("block.getUncleB():" + block.getUncleB());

    if (block.getUncleA() != this.block && block.getUncleB() != this.block) {// 招安失败
      if(!INSITNOTPROUD) {
        System.out.println("NOT!!!I wanna not give up" + "\tcurrentBlockHeight:" + block.getHeight() + "\tMineBlockHeight:" + this.block.getHeight());
      }
      this.addOrphans((GHOSTBlock)block, (GHOSTBlock)this.block);
      return false;
    }
    if(!GIVEINNOTCOMPLAIN) {
      System.out.println("All right,I give in" + "\tcurrentBlockHeight:" + block.getHeight() + "\tMineBlockHeight:" + this.block.getHeight());
    }
    // 招安成功
    this.addOrphans((GHOSTBlock)this.block, (GHOSTBlock)block);
    acceptBlock((GHOSTBlock)block);
    return true;
  }

  /**
   * acceptBlock a block.
   *
   * @param block the block
   */
  private void acceptBlock(Block block){
    // Else add to canonical chain
    this.addToChain(block);
    // Generates a new minting task
    this.minting();
    // Advertise received block
    this.sendInv(block);
  }
  /**
   * acceptBlock a block.
   *
   * @param block the block
   */
  private void acceptBlock(GHOSTBlock block){
    // Else add to canonical chain
    this.addToChain(block);
    // Generates a new minting task
    this.minting();
    // Advertise received block
    this.sendInv(block);
  }
  /**
   * Make Node Keep Insisting.
   *
   * @param block the block
   */
  private void insistBlock(GHOSTBlock block){
    //空
    if (block == null) {
      return;
    }
    //过时的区块
    if (this.block.getHeight() > block.getHeight()) {
      this.addOrphans(block, (GHOSTBlock)this.block);
    }
    //同级的区块
    else if(this.block.getHeight() == block.getHeight()) {
      // T共识
      if(this.block.equals(block)) {
        // 不存在该状态，这里做个保护
        return;
      }
      // F同级竞争不存在招安
      else {
        this.addOrphans(block, (GHOSTBlock)this.block);
      }

    }
    // 大级 保护自己的权利
    else if(this.block.getHeight() > (block.getHeight() - INSISTNUM)){
      // T我过时了,更新
      if(this.block.equals(block.getBlockWithHeight(this.block.getHeight()))) {
        acceptBlock((GHOSTBlock)block);
      }
      // F我的竞争是落后的 招安
      else{
        // 坚持保护自己的辛苦成果
        if(this.block.getMinter().equals(this)){
          giveIn(block);
        }
        // 及时更新
        else{
          this.addOrphans((GHOSTBlock)this.block, (GHOSTBlock)block);
          acceptBlock((GHOSTBlock)block);
        }
      }
    }
    // 六块放弃努力
    else{
      surrender(block);
    }
  }
  /**
   * Make Node Keep Insisting.
   *
   * @param block the block
   */
  private void insistBlock(Block block){
    //空
    if (block == null) {
      return;
    }
    //过时的区块
    if (this.block.getHeight() > block.getHeight()) {

      this.addOrphans(block, this.block);

    }
    //同级的区块
    else if(this.block.getHeight() == block.getHeight()) {
      // T共识
      if(this.block.equals(block)) {
        // 不存在该状态，这里做个保护
        return;
      }
      // F同级竞争不存在招安
      else {
        this.addOrphans(block, this.block);
      }

    }
    // 大级 保护自己的权利
    else if(this.block.getHeight() > (block.getHeight() - INSISTNUM)){
      // T我过时了,更新
      if(this.block.equals(block.getBlockWithHeight(this.block.getHeight()))) {
        acceptBlock(block);
      }
      // F我的竞争是落后的
      else{
        // 坚持保护自己的辛苦成果
        if(this.block.getMinter().equals(this)){
          return;
        }
        // 及时更新 接受
        else{
          this.addOrphans(this.block, block);
          acceptBlock(block);
        }
      }
    }
    // 六块放弃努力
    else{
      surrender(block);
    }
  }
  /**
   * Receive block.
   *
   * @param block the block
   */
  public void receiveBlock(Block block) {
    arriveBlock(block, this);
    // 合法块
    if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
      // 创世块
      if (this.block == null) {
        acceptBlock(block);
        return;
      }
      // 招安：最长链共识 及时消除分叉
      if(GHOST_USE_MODE){
        if (!this.block.isOnSameChainAs(block)) {// 不同链则丢弃自己的区块，接受新链，
          insistBlock((GHOSTBlock)block);// 接受新链时会坚持自我
          return;
        }else if(this.block.getHeight() >= block.getHeight()) {// 同链的旧区块不要(保护)
          return;
        }else{// 同链的新区块接受
          acceptBlock((GHOSTBlock)block);
        }

      }
      // 坚持：最长链共识
      else if(INSISTMODE){
        if (!this.block.isOnSameChainAs(block)) {// 不同链则丢弃自己的区块，接受新链，
          insistBlock(block);// 接受新链时会坚持自我 重载
          return;
        }else if(this.block.getHeight() >= block.getHeight()) {// 同链的旧区块不要(保护)
          return;
        }else{// 同链的新区块接受
          acceptBlock(block);
        }
      }
      // 正常：最长链共识
      else{

        if (!this.block.isOnSameChainAs(block)) {// 不同链则丢弃自己的区块，接受新链，
          // If orphan mark orphan
          this.addOrphans(this.block, block);
          acceptBlock(block);
        }else if(this.block.getHeight() >= block.getHeight()){// 同链的旧区块不要(保护)
          return;
        }else{// 同链的新区块接受
          acceptBlock(block);
        }

      }
      // TODO 原版的代码存在冒险 旧区块居然会接收？  似乎是旧区块不会再次中继
      //      if (this.block != null && !this.block.isOnSameChainAs(block)) {//不同链 标记孤块 接受区块
      //        // If orphan mark orphan
      //        this.addOrphans(this.block, block);
      //      }
      //      // Else add to canonical chain
      //      this.addToChain(block);
      //      // Generates a new minting task
      //      this.minting();
      //      // Advertise received block
      //      this.sendInv(block);

    }
    // 如果难度不够变成孤块  注册未登记的孤块
    else if (!this.orphans.contains(block) && !block.isOnSameChainAs(this.block)) {

      // TODO better understand - what if orphan is not valid?
      // If the block was not valid but was an unknown orphan and is not on the same chain as the
      // current block
      if(GHOST_USE_MODE) {// 招安：最长链共识 及时消除分叉
        this.addOrphans((GHOSTBlock)block, (GHOSTBlock)this.block);
      }
      else{
        this.addOrphans(block, this.block);
      }
    }
  }

  /**
   * Receive message.
   *
   * @param message the message
   */
  public void receiveMessage(AbstractMessageTask message) {
    Node from = message.getFrom();

    if (message instanceof InvMessageTask) {
      Block block = ((InvMessageTask) message).getBlock();
      if (!this.orphans.contains(block) && !this.downloadingBlocks.contains(block)) {// 孤块里没有区块且没在下载 则开始下载
        if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {// 合法块
//          if(this.block == null||block.getHeight() > this.block.getHeight()) {// TODO 合法的话必须得是新的才下载
          AbstractMessageTask task = new RecMessageTask(this, from, block);
          putTask(task);
          downloadingBlocks.add(block);
//          }
        } else if (!block.isOnSameChainAs(this.block)) {// 不合法区块且不同链则接受孤块
          // get new orphan block
          AbstractMessageTask task = new RecMessageTask(this, from, block);
          putTask(task);
          downloadingBlocks.add(block);
        }
      }
    }

    if (message instanceof RecMessageTask) {
      this.messageQue.add((RecMessageTask) message);
      if (!sendingBlock) {
        this.sendNextBlockMessage();
      }
    }

    if (message instanceof GetBlockTxnMessageTask){
      this.messageQue.add((GetBlockTxnMessageTask) message);
      if(!sendingBlock){
          this.sendNextBlockMessage();
      }
    }

    if (message instanceof CmpctBlockMessageTask){
      Block block = ((CmpctBlockMessageTask) message).getBlock();
      Random random = new Random();
      float CBRfailureRate = this.isChurnNode ? CBR_FAILURE_RATE_FOR_CHURN_NODE : CBR_FAILURE_RATE_FOR_CONTROL_NODE;
      boolean success = random.nextDouble() > CBRfailureRate ? true : false;
      if(success){
          downloadingBlocks.remove(block);
          if(this.block == null||block.getHeight() > this.block.getHeight()) {// 接受的话必须得是新的才接受
            this.receiveBlock(block);
          }
      }else{
          AbstractMessageTask task = new GetBlockTxnMessageTask(this, from, block);
          putTask(task);
      }
    }

    if (message instanceof BlockMessageTask) {
      Block block = ((BlockMessageTask) message).getBlock();
      downloadingBlocks.remove(block);
      if(this.block == null||block.getHeight() > this.block.getHeight()) {// 接受的话必须得是新的才接受
        this.receiveBlock(block);
      }
    }
  }


  /**
   * Gets block size when the node fails compact block relay.
   */
  protected long getFailedBlockSize(){
    Random random = new Random();
      if(this.isChurnNode){
          int index = random.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE.length);
          return (long)(BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE[index]);
      }else{
          int index = random.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE.length);
          return (long)(BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE[index]);
      }
  }

  /**
   * Send next block message.
   */
  // send a block to the sender of the next queued recMessage
  public void sendNextBlockMessage() {
    if (this.messageQue.size() > 0) {
      Node to = this.messageQue.get(0).getFrom();
      long bandwidth = getBandwidth(this.getRegion(), to.getRegion());

      AbstractMessageTask messageTask;

      if(this.messageQue.get(0) instanceof RecMessageTask){
        Block block = ((RecMessageTask) this.messageQue.get(0)).getBlock();
        // If use compact block relay.
        if(this.messageQue.get(0).getFrom().useCBR && this.useCBR) {
          // Convert bytes to bits and divide by the bandwidth expressed as bit per millisecond, add
          // processing time.
          long delay = COMPACT_BLOCK_SIZE * 8 / (bandwidth / 1000) + processingTimeExtra(processingTime, -1);

          // Send compact block message.
          messageTask = new CmpctBlockMessageTask(this, to, block, delay);
        } else {
          // Else use lagacy protocol.
          long delay = BLOCK_SIZE * 8 / (bandwidth / 1000) + processingTimeExtra(processingTime, -1);
          messageTask = new BlockMessageTask(this, to, block, delay);
        }
      } else if(this.messageQue.get(0) instanceof GetBlockTxnMessageTask) {
        // Else from requests missing transactions.
        Block block = ((GetBlockTxnMessageTask) this.messageQue.get(0)).getBlock();
        long delay = getFailedBlockSize() * 8 / (bandwidth / 1000) + processingTimeExtra(processingTime, -1);
        messageTask = new BlockMessageTask(this, to, block, delay);
      } else {
        throw new UnsupportedOperationException();
      }
      
      sendingBlock = true;
      this.messageQue.remove(0);
      putTask(messageTask);
    } else {
      sendingBlock = false;
    }
  }
}
