package simblock.node.consensus;

import simblock.block.Block;
import simblock.block.PoWGhostBlock;

import simblock.node.Node;
import simblock.task.EthMiningTask;

import java.math.BigInteger;

import static simblock.simulator.Main.random;
/**
 * The type Proof of work In Eth.
 */
@SuppressWarnings("unused")
public class PoWEth extends AbstractConsensusAlgo {

    public PoWEth(Node selfNode) {
        super(selfNode);

    }

    /**
     * Mints a new block by simulating Proof of Work.
     */
    @Override
    public EthMiningTask minting() {
        Node selfNode = this.getSelfNode();
        PoWGhostBlock parent = (PoWGhostBlock) selfNode.getBlock();
        BigInteger difficulty = parent.getNextDifficulty();
        double p = 1.0 / difficulty.doubleValue();
        double u = random.nextDouble();
        return p <= Math.pow(2, -53) ? null : new EthMiningTask(selfNode, (long) (Math.log(u) / Math.log(
                1.0 - p) / selfNode.getMiningPower()), difficulty, selfNode.getUncle(), selfNode.getUncle());
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
        if (!(receivedBlock instanceof PoWGhostBlock)) {
            return false;
        }
        PoWGhostBlock recPoWBlock = (PoWGhostBlock) receivedBlock;
        PoWGhostBlock currPoWBlock = (PoWGhostBlock) currentBlock;
        int receivedBlockHeight = receivedBlock.getHeight();
        PoWGhostBlock receivedBlockParent = receivedBlockHeight == 0 ? null :
                (PoWGhostBlock) receivedBlock.getBlockWithHeight(receivedBlockHeight - 1);

        //TODO - dangerous to split due to short circuit operators being used, refactor?
        return (
                receivedBlockHeight == 0 ||
                        recPoWBlock.getDifficulty().compareTo(receivedBlockParent.getNextDifficulty()) >= 0
        ) && (
                currentBlock == null ||
                        recPoWBlock.getTotalDifficulty().compareTo(currPoWBlock.getTotalDifficulty()) > 0
        );
    }

    @Override
    public PoWGhostBlock genesisBlock() {
        return PoWGhostBlock.genesisBlock(this.getSelfNode());
    }
}
