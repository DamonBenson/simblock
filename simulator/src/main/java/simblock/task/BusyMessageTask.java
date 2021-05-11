package simblock.task;

import simblock.block.Block;
import simblock.block.Block;
import simblock.node.Node;

/**
 * @author: Bernard
 * @date: 2021/5/11 10:17
 * @description:
 */


/**
 * The type Busy message task inform receiver.
 */
public class BusyMessageTask extends AbstractMessageTask {

    /**
     * The block to  be received.
     */
    private final Block block;
    /**
     * The Queue.
     */
    private final int queLenth = 1;
    /**
     * The Queue.
     */
    private final RecMessageTask message;



    /**
     * Instantiates a new Rec message task.
     *
     * @param from  the sending node
     * @param to    the receiving node
     * @param message the message to being busy
     */
    public BusyMessageTask(Node from, Node to, RecMessageTask message) {
        super(from, to);
        this.block = message.getBlock();
        this.message = message;
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
     * Gets the message to be inform.
     *
     * @return the message
     */
    public RecMessageTask getMessage() {
        return this.message;
    }
    /**
     * Make Transation Stop
     */
    @Override
    public void run() {
        super.run();
    }
}

