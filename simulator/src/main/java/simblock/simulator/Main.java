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
import simblock.block.GHOSTBlock;
import simblock.task.AbstractMintingTask;
import simblock.node.Node;


import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static simblock.settings.SimulationConfiguration.*;
import static simblock.simulator.Network.*;
import static simblock.simulator.Simulator.*;
import static simblock.simulator.Timer.*;


/**
 * The type Main represents the entry point.
 */
public class Main {
    /**
     * The constant to be used as the simulation seed.
     */
    public static Random random = new Random();

    /**
     * The initial simulation time.
     */
    public static long simulationTime = 0;


    public static long RSTTimes = 0;//RSTTimes = BusyTimes + SpareLinkTimes
    public static long BusyTimes = 0;
    public static long SpareLinkTimes = 0;

    public static long DelaySuccessRelayDenyTimes = 0;


    /**
     * whether use GHOST protocol
     */
    public static boolean GHOST_USE_MODE;
    /**
     * The initial simulation time.
     */
    public static long simulationStamp = 0;

    /**
     * The initial simulation time.
     */
    public static final long TotalSimulationEpoch = 3;
    /**
     * The initial simulation time.
     */
    public static long SimulationEpoch = 3;
    /**
     * Path to config file.
     */
    public static URI CONF_FILE_URI;
    /**
     * Output path.
     */
    public static URI OUT_FILE_URI;
    //https://www.yiibai.com/java/lang/classloader_getsystemresource.html
    static {
        try {
            //CONF_FILE_URI = ClassLoader.getSystemResource("simulator.conf").toURI();
            //OUT_FILE_URI = CONF_FILE_URI.resolve(new URI("../output/"));
            OUT_FILE_URI = new URI("file:/E:/InputFile/GitBase/Simblock/simulator/src/dist/output");// 只需要改好这个路径，就能得到设定的输出
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * The output writer.
     */
    //TODO use logger
    public static PrintWriter OUT_JSON_FILE;

    /**
     * The constant STATIC_JSON_FILE.
     */
    //TODO use logger
    public static PrintWriter STATIC_JSON_FILE;

    /**
     * The constant CUSTOM_TEXT_FILE.
     */
    //TODO use logger
    public static PrintWriter CUSTOM_TEXT_FILE;

    /**
     * The constant PROPAGATION_TEXT_FILE.
     */
    //TODO use logger
    public static PrintWriter PROPAGATION_TEXT_FILE;

    /**
    * The constant REWARD_TEXT_FILE.
    */
    //TODO use logger
    public static PrintWriter REWARD_TEXT_FILE;

    /**
     * The constant REWARD_TEXT_FILE.
     */
    //TODO use logger
    public static PrintWriter BLOCK_TEXT_FILE;

    /**
     * How much is The unclePayment.
     *
     * @param  generationGap
     */
    public static float unclePaymentMult(int generationGap) {
      if(8>generationGap&&generationGap>0)
        return (float) ((float)(8.0-generationGap)/8.0);// 7 No Pay
      else
        return 0;
    }



    /**
     * The entry point.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
      GHOST_USE_MODE = ALGO=="simblock.node.consensus.ProofOfWorkEth"?true:false;
      while(SimulationEpoch <= TotalSimulationEpoch){
        long start = System.currentTimeMillis();
        long startStamp = getCurrentTime();
        System.out.println("The " + SimulationEpoch + " Epoch finished,start timestamp:"+startStamp);

        try {
            OUT_JSON_FILE = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./output%d.json",SimulationEpoch))))));
            STATIC_JSON_FILE = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./static%d.json",SimulationEpoch))))));
            REWARD_TEXT_FILE = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./reward%d.txt",SimulationEpoch))))));
            CUSTOM_TEXT_FILE = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./custom%d.txt",SimulationEpoch))))));
            PROPAGATION_TEXT_FILE = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./propagation%d.txt",SimulationEpoch))))));
            BLOCK_TEXT_FILE = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./block%d.txt",SimulationEpoch))))));

        } catch (IOException e) {
            e.printStackTrace();
        }
        printNetProtocolSetting();
        setTargetInterval(INTERVAL);

        //start json format
        OUT_JSON_FILE.print("[");
        OUT_JSON_FILE.flush();

        // Log regions
        printRegion();

        // Setup network
        constructNetworkWithAllNodes(NUM_OF_NODES);

        // Initial block height, we stop at END_BLOCK_HEIGHT
        int currentBlockHeight = 1;
        boolean IsMaxChain = false;

        // Iterate over tasks and handler
        try {
            while (getTask() != null) {
                if (getTask() instanceof AbstractMintingTask) {
                    AbstractMintingTask task = (AbstractMintingTask) getTask();
                    if (task.getParent().getHeight() == currentBlockHeight) {
                        currentBlockHeight++;
                    }
                    if (currentBlockHeight % 3000 == 0) {
                        System.out.println("currentBlockHeight:" + currentBlockHeight);
                    }
                    if (currentBlockHeight > END_BLOCK_HEIGHT) {
                        break;
                    }
                    // |Log| every 100 OnChainBlock and at the second block
                    // TODO use constants here
                    if (QUIET) {
                        continue;
                    }
                    IsMaxChain = ((task.getParent().getHeight() + 1) == currentBlockHeight);
                    if (VERBOSE) {
                        System.out.println(IsMaxChain + "\tNowHeight:" + (task.getParent().getHeight() + 1) + "\tcurrentBlockHeight:" + currentBlockHeight + "\tParentBlock:" + task.getParent().getId());
                    } else {
                        if (currentBlockHeight % 100 == 0 || currentBlockHeight == 2) {
                            writeGraph(currentBlockHeight);
                            System.out.println(IsMaxChain + "\tNowHeight:" + (task.getParent().getHeight() + 1) + "\tcurrentBlockHeight:" + currentBlockHeight + "\tParentBlock:" + task.getParent().getId());
                        }
                    }

                }
                // Execute task
                runTask();
            }
        }
        catch (java.lang.OutOfMemoryError e) {// 内存不足
            System.out.println("OutOfMemoryError...");
            e.printStackTrace();
        }
        catch ( OutOfMemoryError e) {// 内存不足
            System.out.println("OutOfMemoryError...");
            e.printStackTrace();
        }

        //TODO logger
        System.out.println();

        Set<Block> orphans = new HashSet<>();
        Set<Block> UncleCandidates = new HashSet<>();
        Set<Block> OnChainBlock = new HashSet<>();
        Set<Block> OnChainUncleBlock = new HashSet<>();




        float averageOrphansSize;
        int totalOrphansSize = -1;
        int totalUncleCandidatesSize = -1;
        // Gather all known orphans
        int OnZhaoAnCount = -1;
        int RealOrphans = -1;
        int RealUncleCandidates = -1;
        int NotZhaoAnNum = -1;
        int RealNotZhaoAnNum = -1;
        int OutDateOrphan = -1;
        int RealZhaoAnRealNum = -1;
        int AllOrphanCount = -1;

        Block selBlock = getSimulatedNodes().get(0).getBlock();
        OnChainBlock.add(selBlock);
        // 获取区块  并  结算矿工工资
        while((selBlock = selBlock.getParent())!=null){
          OnChainBlock.add(selBlock);
          selBlock.getMinter().addBalance(50);
          int currHeight = selBlock.getHeight();
          if(GHOST_USE_MODE) {
            if(((GHOSTBlock)selBlock).getUncleA() != null){
              OnChainUncleBlock.add(((GHOSTBlock)selBlock).getUncleA());
              ((GHOSTBlock)selBlock).getUncleA().getMinter().addBalance((float) (50.0 * unclePaymentMult((currHeight-((GHOSTBlock)selBlock).getUncleA().getHeight()))));
              ((GHOSTBlock)selBlock).getMinter().addBalance((float) (50.0 * (1/32)));
            }
            if(((GHOSTBlock)selBlock).getUncleB() != null){
              OnChainUncleBlock.add(((GHOSTBlock)selBlock).getUncleB());
              ((GHOSTBlock)selBlock).getUncleB().getMinter().addBalance((float) (50.0 * unclePaymentMult((currHeight-((GHOSTBlock)selBlock).getUncleB().getHeight()))));
              ((GHOSTBlock)selBlock).getMinter().addBalance((float) (50.0 * (1/32)));
            }
          }
        }


        // 非GHOST没有叔叔
        if(!GHOST_USE_MODE){
          for (Node node : getSimulatedNodes()) {
            orphans.addAll(node.getOrphans());
          }
          totalOrphansSize = orphans.size();// 总孤块

          orphans.removeAll(OnChainBlock);// 链上的孤块不算

          RealOrphans = orphans.size();// 真孤块数目

          System.out.println("\"RealOrphans\":" + RealOrphans);// 真孤块数目 valid
        }
        // GHOST统计叔叔
        else{
          for (Node node : getSimulatedNodes()) {
            orphans.addAll(node.getOrphans());
            LinkedList<Block> UncleCandidate = node.getUncleCandidate();
            Block uncle = null;
            while((uncle = UncleCandidate.pollFirst())!=null){
              UncleCandidates.add(uncle);
            }
          }
          Set<Block> orphansCP = new HashSet<>();

          orphansCP.addAll(orphans);
          totalOrphansSize = orphans.size();
          totalUncleCandidatesSize = UncleCandidates.size();// 总候选叔叔
          orphansCP.addAll(OnChainUncleBlock);
          AllOrphanCount = orphansCP.size();// 全孤块 = 真孤块 + 招安

          orphans.removeAll(OnChainBlock);// 链上的孤块不算
          orphans.removeAll(OnChainUncleBlock);// 链上的叔叔不算
          UncleCandidates.removeAll(OnChainBlock);// 链上的不算候选叔叔
          UncleCandidates.removeAll(OnChainUncleBlock);// 链上的叔叔区块不算候选叔叔

          RealOrphans = orphans.size();// 真孤块数目
          RealUncleCandidates = UncleCandidates.size();// 真候选叔叔
          OnZhaoAnCount = OnChainUncleBlock.size();// 链上的叔叔区块


          // NotZhaoAnNum = RealOrphans - RealUncleCandidates;// 未招安的真孤块
          // NotZhaoAnNum = RealOrphans;// 未招安的真孤块

          orphansCP.addAll(orphans);
          orphansCP.removeAll(OnChainUncleBlock);// A-B 未招安的孤块
          // RealNotZhaoAnNum = RealOrphans - orphansCP.size();// 因为招安而减少的孤块数目 = 孤块 - 未招安的孤块
          RealNotZhaoAnNum = AllOrphanCount - RealOrphans;// 因为招安而减少的孤块数目 = 全孤块 - 真孤块

          // orphansCP.clear();
          // orphansCP.addAll(orphans);
          // orphansCP.removeAll(UncleCandidates);// A-B 不在候选的孤块
          // OutDateOrphan = orphansCP.size();// 过期的孤块
          OutDateOrphan = AllOrphanCount - OnZhaoAnCount - RealUncleCandidates;// 过期的孤块 = 全孤块 - 链上的叔叔区块 - 未招安的孤块
          // RealZhaoAnRealNum = RealOrphans - orphansCP.size();// 待招安数目 =  孤块 - 过期的孤块
          // RealZhaoAnRealNum = RealUncleCandidates;// 待招安数目 =  孤块 - 过期的孤块




          System.out.println("\"RealOrphans\":" + RealOrphans);// 真孤块数目 valid
          System.out.println("\"AllOrphanCount\":" + AllOrphanCount);//  全孤块 valid
          System.out.println("\"RealNotZhaoAnNum\":" + RealNotZhaoAnNum);// 因为招安而减少的孤块数目 = 全孤块 - 真孤块 valid
          System.out.println("\"RealUncleCandidates\":" + RealUncleCandidates);// 真候选叔叔 valid
          System.out.println("\"OnZhaoAnCount\":" + OnZhaoAnCount);// 链上的叔叔区块 Invalid 存在分叉时会导致+-1偏差
          System.out.println("\"OutDateOrphan\":" + OutDateOrphan);// 过期的孤块 不在候选内 valid  因为没有校验就该为0
          // System.out.println("\"RealZhaoAnRealNum\":" + RealZhaoAnRealNum);// 待招安数目 valid
          // System.out.println("\"NotZhaoAnNum\":" + NotZhaoAnNum);// 未招安的孤块 = 真孤块数目 - 真候选叔叔


          // 孤块：RealOrphans 75
          // 叔叔：OnZhaoAnCount 48   RealNotZhaoAnNum 47
          // 候选：RealUncleCandidates 28
          // 过期：OutDateOrphan 0

        }


        averageOrphansSize = (float)totalOrphansSize / getSimulatedNodes().size();

        for (Node node : getSimulatedNodes()) {
          orphans.addAll(node.getOrphans());
        }

        // Record orphans to the list of all known OnChainBlock
        OnChainBlock.addAll(orphans);

        ArrayList<Block> blockList = new ArrayList<>(OnChainBlock);

        //Sort the OnChainBlock first by time, then by hash code
        blockList.sort((a, b) -> {
            int order = Long.signum(a.getTime() - b.getTime());
            if (order != 0) {
                return order;
            }
            order = System.identityHashCode(a) - System.identityHashCode(b);
            return order;
        });

        //Log all orphans
        // TODO move to method and use logger
        for (Block orphan : orphans) {
            try {
                CUSTOM_TEXT_FILE.print(orphan + "|Height:" + orphan.getHeight() + "|Minter:" + orphan.getMinter() + '\n');
                CUSTOM_TEXT_FILE.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Node node : getSimulatedNodes()) {
            if(node.getBlock().getHeight()<(END_BLOCK_HEIGHT-1))
                System.out.println("\"getHeight\":" + node.getBlock().getHeight());

            try {
            int IsInsist = 0;
            if(node.getIsInsistNode() == true)
              IsInsist = 1;
            REWARD_TEXT_FILE.print(node + ":Balance:" + node.getBalance() + ":MiningPower:" + node.getMiningPower() +
                    ":InsistNode:" + IsInsist + ":NumConnection:" + node.getNumConnection() + '\n');
            REWARD_TEXT_FILE.flush();
          } catch (Exception e) {
            e.printStackTrace();
          }

        }


        //Log all Fork
        /*
        Log in format:
        ＜fork_information, block height, block ID＞
        fork_information: One of "OnChain" and "Orphan". "OnChain" denote block is on Main chain.
        "Orphan" denote block is an orphan block.
        */
        // TODO move to method and use logger
        try {

            FileWriter fw = new FileWriter(new File(OUT_FILE_URI.resolve(String.format("./blockList%d.txt",SimulationEpoch))), false);
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

            for (Block b : blockList) {
                if (!orphans.contains(b)) {
                    pw.println("OnChain : " + b.getHeight() + " |Identity: " + b + " |BuildTime: " + b.getTime() + "|PropagationTime: " + (b.propagationFinished - b.getTime()));
                } else {
                    pw.println("Orphan : " + b.getHeight() + " |Identity: " + b + " |BuildTime: " + b.getTime() + "|CompeteTime: " + (b.propagationFinished - b.getTime()));
                }
            }
            pw.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Print propagation information about all blocks
        printAllPropagation(blockList);

        long end = System.currentTimeMillis();
        long endStamp = getCurrentTime();
        simulationTime = end - start;
        simulationStamp = endStamp - startStamp;

        // log
        OUT_JSON_FILE.print("{");
        OUT_JSON_FILE.print("\"kind\":\"simulation-end\",");
        OUT_JSON_FILE.print("\"content\":{");
        OUT_JSON_FILE.print("\"averageOrphansSize\":" + averageOrphansSize);
        OUT_JSON_FILE.print("\"totalOrphansSize\":" + totalOrphansSize);
        OUT_JSON_FILE.print("\"totalUncleCandidatesSize\":" + totalUncleCandidatesSize + '\n');
        OUT_JSON_FILE.print("\"RealOrphans\":" + RealOrphans);// 真孤块数目 valid
        OUT_JSON_FILE.print("\"AllOrphanCount\":" + AllOrphanCount);//  全孤块 valid
        OUT_JSON_FILE.print("\"RealNotZhaoAnNum\":" + RealNotZhaoAnNum);// 因为招安而减少的孤块数目 = 全孤块 - 真孤块 valid
        OUT_JSON_FILE.print("\"RealUncleCandidates\":" + RealUncleCandidates);// 真候选叔叔 valid
        OUT_JSON_FILE.print("\"OnZhaoAnCount\":" + OnZhaoAnCount);// 链上的叔叔区块 Invalid 存在分叉时会导致+-1偏差
        OUT_JSON_FILE.print("\"OutDateOrphan\":" + OutDateOrphan);// 过期的孤块 不在候选内 valid  因为没有校验就该为0
        OUT_JSON_FILE.print("\"timestamp\":" + getCurrentTime());
        OUT_JSON_FILE.print("}");
        OUT_JSON_FILE.print("}");
        OUT_JSON_FILE.print("]");
        OUT_JSON_FILE.close();
        // end json format
        CUSTOM_TEXT_FILE.println("{");
        CUSTOM_TEXT_FILE.println("\"kind\":\"simulation-end\",");
        CUSTOM_TEXT_FILE.println("\"content\":{");
        CUSTOM_TEXT_FILE.println("\"averageOrphansSize\":" + averageOrphansSize);
        CUSTOM_TEXT_FILE.println("\"totalOrphansSize\":" + totalOrphansSize);
        CUSTOM_TEXT_FILE.println("\"totalUncleCandidatesSize\":" + totalUncleCandidatesSize);
        CUSTOM_TEXT_FILE.println("\"RealOrphans\":" + RealOrphans);// 真孤块数目 valid
        CUSTOM_TEXT_FILE.println("\"AllOrphanCount\":" + AllOrphanCount);//  全孤块 valid
        CUSTOM_TEXT_FILE.println("\"RealNotZhaoAnNum\":" + RealNotZhaoAnNum);// 因为招安而减少的孤块数目 = 全孤块 - 真孤块 valid
        CUSTOM_TEXT_FILE.println("\"RealUncleCandidates\":" + RealUncleCandidates);// 真候选叔叔 valid
        CUSTOM_TEXT_FILE.println("\"OnZhaoAnCount\":" + OnZhaoAnCount);// 链上的叔叔区块 Invalid 存在分叉时会导致+-1偏差
        CUSTOM_TEXT_FILE.println("\"OutDateOrphan\":" + OutDateOrphan);// 过期的孤块 不在候选内 valid  因为没有校验就该为0
        CUSTOM_TEXT_FILE.println("\"timestamp\":" + getCurrentTime());
        CUSTOM_TEXT_FILE.println("\"simulationStamp\":" + simulationStamp);
        CUSTOM_TEXT_FILE.println("}");
        CUSTOM_TEXT_FILE.println("}");
        CUSTOM_TEXT_FILE.println("]");
        CUSTOM_TEXT_FILE.close();
        PROPAGATION_TEXT_FILE.close();
        BLOCK_TEXT_FILE.close();
          // end json format
        // Log simulation time in milliseconds
        System.out.println(simulationTime);
        System.out.println("\"averageOrphansSize\":" + averageOrphansSize);
        System.out.println("\"totalOrphansSize\":" + totalOrphansSize);
        System.out.println("\"totalUncleCandidatesSize\":" + totalUncleCandidatesSize);
        System.out.println("\"RealOrphans\":" + RealOrphans);// 真孤块数目 valid
        System.out.println("\"AllOrphanCount\":" + AllOrphanCount);//  全孤块 valid
        System.out.println("\"RealNotZhaoAnNum\":" + RealNotZhaoAnNum);// 因为招安而减少的孤块数目 = 全孤块 - 真孤块 valid
        System.out.println("\"RealUncleCandidates\":" + RealUncleCandidates);// 真候选叔叔 valid
        System.out.println("\"OnZhaoAnCount\":" + OnZhaoAnCount);// 链上的叔叔区块 Invalid 存在分叉时会导致+-1偏差
        System.out.println("\"OutDateOrphan\":" + OutDateOrphan);// 过期的孤块 不在候选内 valid  因为没有校验就该为0
        System.out.println("\"timestamp\":" + getCurrentTime());
        System.out.println("\"simulationStamp\":" + simulationStamp);
        System.out.println("The " + SimulationEpoch + " Epoch finished,simulation Time:" + (simulationTime/1000)
                + " simulation Stamp:" + simulationStamp
                + " simulation Stamp Minutes:" + simulationStamp/(1000*60));
        resetTask();
        clearNode();//For multiEpoch
        SimulationEpoch ++;
        System.out.println("RSTTime:"+RSTTimes);
        System.out.println("BusyTimes:"+BusyTimes);
        System.out.println("SpareLinkTimes:"+SpareLinkTimes);
        System.out.println("DelaySuccessRelayDenyTimes:"+DelaySuccessRelayDenyTimes);


      }
    }


    //TODO　以下の初期生成はシナリオを読み込むようにする予定
    //ノードを参加させるタスクを作る(ノードの参加と，リンクの貼り始めるタスクは分ける)
    //シナリオファイルで上の参加タスクをTimer入れていく．

    // TRANSLATED FROM ABOVE STATEMENT
    // The following initial generation will load the scenario
    // Create a task to join the node (separate the task of joining the node and the task of
    // starting to paste the link)
    // Add the above participating tasks with a timer in the scenario file.

    /**
     * Populate the list using the distribution.
     *
     * @param distribution the distribution
     * @param facum        whether the distribution is cumulative distribution
     * @return array list
     */
    //TODO explanation on facum etc.
    public static ArrayList<Integer> makeRandomListFollowDistribution(double[] distribution, boolean facum) {
        ArrayList<Integer> list = new ArrayList<>();
        int index = 0;

        if (facum) {
            for (; index < distribution.length; index++) {
                while (list.size() <= NUM_OF_NODES * distribution[index]) {
                    list.add(index);
                }
            }
            while (list.size() < NUM_OF_NODES) {
                list.add(index);
            }
        } else {
            double acumulative = 0.0;
            for (; index < distribution.length; index++) {
                acumulative += distribution[index];
                while (list.size() <= NUM_OF_NODES * acumulative) {
                    list.add(index);
                }
            }
            while (list.size() < NUM_OF_NODES) {
                list.add(index);
            }
        }

        Collections.shuffle(list, random);// 打乱顺序
        return list;
    }

    /**
     * Populate the list using the rate.
     *
     * @param rate the rate of true
     * @return array list
     */
    public static ArrayList<Boolean> makeRandomList(float rate){
        ArrayList<Boolean> list = new ArrayList<>();
        for(int i=0; i < NUM_OF_NODES; i++){
            list.add(i < NUM_OF_NODES*rate);
        }
        Collections.shuffle(list, random);
        return list;
    }

    /**
     * Generates a random mining power expressed as Hash Rate, and is the number of mining (hash
     * calculation) executed per millisecond.
     *
     * @return the number of hash  calculations executed per millisecond.
     */
    public static int genMiningPower() {
        double r = random.nextGaussian();

        return Math.max((int) (r * STDEV_OF_MINING_POWER + AVERAGE_MINING_POWER), 1);
    }

    /**
     * Construct network with the provided number of nodes.
     *
     * @param numNodes the num nodes
     */
    public static void constructNetworkWithAllNodes(int numNodes) {

        // Random distribution of nodes per region
        double[] regionDistribution = getRegionDistribution();
        List<Integer> regionList = makeRandomListFollowDistribution(regionDistribution, false);

        // Random distribution of node degrees
        double[] degreeDistribution = getDegreeDistribution();
        List<Integer> degreeList = makeRandomListFollowDistribution(degreeDistribution, true);

        // List of nodes using compact block relay.
        List<Boolean> useCBRNodes = makeRandomList(CBR_USAGE_RATE);

        // List of churn nodes.
        List<Boolean> churnNodes = makeRandomList(CHURN_NODE_RATE);

      // List of churn nodes.
      List<Boolean> insistNodes = makeRandomList(INSIST_NODE_RATE);

        for (int id = 1; id <= numNodes; id++) {
            // Each node gets assigned a region, its degree, mining power, routing table and
            // consensus algorithm
            Node node = new Node(
                    id, degreeList.get(id - 1) + 1, regionList.get(id - 1), genMiningPower(), TABLE,
                    ALGO, useCBRNodes.get(id - 1), churnNodes.get(id - 1),insistNodes.get(id - 1)
            );
            // Add the node to the list of simulated nodes
            addNode(node);

            OUT_JSON_FILE.print("{");
            OUT_JSON_FILE.print("\"kind\":\"add-node\",");
            OUT_JSON_FILE.print("\"content\":{");
            OUT_JSON_FILE.print("\"timestamp\":0,");
            OUT_JSON_FILE.print("\"node-id\":" + id + ",");
            OUT_JSON_FILE.print("\"region-id\":" + regionList.get(id - 1));
            OUT_JSON_FILE.print("}");
            OUT_JSON_FILE.print("},");
            OUT_JSON_FILE.flush();

        }

        // Link newly generated nodes
        for (Node node : getSimulatedNodes()) {
            node.joinNetwork();
        }

        // Designates a random node (nodes in list are randomized) to mint the genesis block
        getSimulatedNodes().get(0).genesisBlock();
    }

    /**
     * Network information when block height is <em>blockHeight</em>, in format:
     *
     * <p><em>nodeID_1</em>, <em>nodeID_2</em>
     *
     * <p>meaning there is a connection from nodeID_1 to right nodeID_1.
     *
     * @param blockHeight the index of the graph and the current block height
     */
    //TODO use logger
    public static void writeGraph(int blockHeight) {
        if(PRINTGRAPH){
            try {
                FileWriter fw = new FileWriter(
                        new File(OUT_FILE_URI.resolve("./output/graph/" + blockHeight + ".txt")), false);
                PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

                for (int index = 1; index <= getSimulatedNodes().size(); index++) {
                    Node node = getSimulatedNodes().get(index - 1);
                    for (int i = 0; i < node.getNeighbors().size(); i++) {
                        Node neighbor = (Node) node.getNeighbors().get(i);
                        pw.println(node.getNodeID() + " " + neighbor.getNodeID());
                    }
                }
                pw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
