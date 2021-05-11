package simblock.task;

import simblock.block.Block;
import simblock.block.Block;
import simblock.node.Node;

/**
 * @author: Bernard
 * @date: 2021/5/10 17:33
 * @description:
 */

/**
 * The type RST message task deny receive.
 */
public class RSTMessageTask extends AbstractMessageTask {

    /**
     * The block to  be received.
     */
    private final Block block;

    /**
     * Instantiates a new Rec message task.
     *
     * @param from  the sending node
     * @param to    the receiving node
     * @param block the block to be received
     */
    public RSTMessageTask(Node from, Node to, Block block) {
        super(from, to);
        this.block = block;
    }

    /**
     * Gets the block to be received.
     *
     * @return the block
     */
    public Block getBlock() {
        return this.block;
    }
    /**
     * Make Transation Stop
     */
    @Override
    public void run() {
        super.run();
    }
}

