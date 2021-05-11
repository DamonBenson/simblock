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

package simblock.task;

import simblock.block.GHOSTBlock;
import simblock.block.Block;
import simblock.node.Node;

import java.math.BigInteger;

import static simblock.simulator.Timer.getCurrentTime;

/**
 * The type Mining task.
 */
public class EthMiningTask extends AbstractMintingTask {
  private final BigInteger difficulty;
  private final Block uncleA;
  private final Block uncleB;

  /**
   * Instantiates a new Mining task.
   *
   * @param minter     the minter
   * @param interval   the interval
   * @param difficulty the difficulty
   */
  //TODO how is the difficulty expressed and used here?
  public EthMiningTask(Node minter, long interval, BigInteger difficulty, Block uncleA, Block uncleB) {
    super(minter, interval);
    this.difficulty = difficulty;
    this.uncleA = uncleA;
    this.uncleB = uncleB;
  }

  @Override
  public void run() {
    GHOSTBlock createdBlock = new GHOSTBlock(
        (GHOSTBlock) this.getParent(), this.getMinter(), getCurrentTime(),
        this.difficulty, this.uncleA, this.uncleB
    );
    this.getMinter().receiveBlock(createdBlock);

  }
}
