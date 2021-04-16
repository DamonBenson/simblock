package simblock.node.consensus;

import simblock.block.Block;
import simblock.block.GHOSTBlock;

import simblock.node.Node;
import simblock.task.EthMiningTask;

import java.math.BigInteger;

import static simblock.simulator.Main.random;
/**
 * The type Proof of work In Eth.
 */
@SuppressWarnings("unused")
public class PoWEth extends AbstractConsensusAlgo {
    private final Node selfNode;

    public PoWEth(Node selfNode) {
        super(selfNode);
        this.selfNode = selfNode;
    }

    public Node getSelfNode() {
        return this.selfNode;
    }
    /**
     * Mints a new block by simulating Proof of Work.
     */
    @Override
    public EthMiningTask minting() {
        Node selfNode = this.getSelfNode();
        GHOSTBlock parent = (GHOSTBlock) selfNode.getBlock();
        BigInteger difficulty = parent.getNextDifficulty();
        double p = 1.0 / difficulty.doubleValue();
        double u = random.nextDouble();
        return p <= Math.pow(2, -53) ? null : new EthMiningTask((Node) selfNode, (long) (Math.log(u) / Math.log(
                1.0 - p) / selfNode.getMiningPower()), difficulty, selfNode.generateUncleA(), selfNode.generateUncleB());
    }

    /**
     * Tests if the receivedBlock is valid with regards to the current block. The receivedBlock
     * is valid if it is an instance of a Proof of Work block and the received block needs to have
     * a bigger difficulty than its parent next difficulty and a bigger total difficulty compared to
     * the current block.
     *
     * @param receivedBlock the received block
     * @param currentBlock  the current block
     * @return true if block is valid false otherwise
     */
    @Override
    public boolean isReceivedBlockValid(Block receivedBlock, Block currentBlock) {
        if (!(receivedBlock instanceof GHOSTBlock)) {//需要是个正常的块
            return false;
        }
        GHOSTBlock recPoWBlock = (GHOSTBlock) receivedBlock;
        GHOSTBlock currPoWBlock = (GHOSTBlock) currentBlock;
        int receivedBlockHeight = receivedBlock.getHeight();
        GHOSTBlock receivedBlockParent = receivedBlockHeight == 0 ? null :
                (GHOSTBlock) receivedBlock.getBlockWithHeight(receivedBlockHeight - 1);

        //TODO - dangerous to split due to short circuit operators being used, refactor?
        return (
                receivedBlockHeight == 0 ||
                        recPoWBlock.getDifficulty().compareTo(receivedBlockParent.getNextDifficulty()) >= 0//新块难度需要合规
        ) && (
                currentBlock == null ||
                        recPoWBlock.getTotalDifficulty().compareTo(currPoWBlock.getTotalDifficulty()) > 0//创世块合法
        );
    }

    @Override
    public GHOSTBlock genesisBlock() {
        return GHOSTBlock.genesisBlock(this.getSelfNode());
    }
}
