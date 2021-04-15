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

package simblock.block;

import simblock.node.Node;

import java.math.BigInteger;

import static simblock.simulator.Simulator.getSimulatedNodes;
import static simblock.simulator.Simulator.getTargetInterval;

/**
 * The type Proof of work GhostBlock.
 */
public class GHOSTBlock extends Block {
  private final BigInteger difficulty;
  private final BigInteger totalDifficulty;
  private final BigInteger nextDifficulty;
  private static BigInteger genesisNextDifficulty;
  /**
   * Unique uncleA.
   */
  private final Block uncleA;
  /**
   * Unique uncleB.
   */
  private final Block uncleB;

  /**
   * The parent {@link Block}.
   */
  private final GHOSTBlock parent;

  /**
   * Instantiates a new Proof of work PoWGhostBlock.
   *
   * @param parent     the parent
   * @param minter     the minter
   * @param time       the time
   * @param difficulty the difficulty
   * @param uncleA the uncleA
   * @param uncleB the uncleB
   */
  public GHOSTBlock(GHOSTBlock parent, Node minter, long time, BigInteger difficulty, Block uncleA, Block uncleB) {
    super(parent, minter, time);
    this.parent = parent;
    this.difficulty = difficulty;
    this.uncleA = uncleA;
    this.uncleB = uncleB;
    if (parent == null) {
      this.totalDifficulty = BigInteger.ZERO.add(difficulty);
      this.nextDifficulty = GHOSTBlock.genesisNextDifficulty;
    } else {
      this.totalDifficulty = parent.getTotalDifficulty().add(difficulty);
      // TODO: difficulty adjustment
      this.nextDifficulty = parent.getNextDifficulty();
    }

  }

  /**
   * Gets difficulty.
   *
   * @return the difficulty
   */
  public BigInteger getDifficulty() {
    return this.difficulty;
  }

  /**
   * Gets total difficulty.
   *
   * @return the total difficulty
   */
  public BigInteger getTotalDifficulty() {
    return this.totalDifficulty;
  }

  /**
   * Gets next difficulty.
   *
   * @return the next difficulty
   */
  public BigInteger getNextDifficulty() {
    return this.nextDifficulty;
  }

  /**
   * Generates the genesis block, gets the total mining power and adjusts the difficulty of the
   * next block accordingly.
   * @override
   * @param minter the minter
   * @return the genesis block
   */
  public static GHOSTBlock genesisBlock(Node minter) {
    long totalMiningPower = 0;
    for (Node node : getSimulatedNodes()) {
      totalMiningPower += node.getMiningPower();
    }
    genesisNextDifficulty = BigInteger.valueOf(totalMiningPower * getTargetInterval());
    return new GHOSTBlock(null, minter, 0, BigInteger.ZERO, null, null);
  }
  /**
   * Gets the block uncleA.
   *
   * @return the uncleA
   */
  public Block getUncleA() {    return this.uncleA;  }
  /**
   * Gets the block uncleB.
   *
   * @return the uncleB
   */
  public Block getUncleB() {    return this.uncleB;  }
  /**
   * Get parent block.
   *
   * @return the block
   */
  public GHOSTBlock getParent() {    return this.parent;  }
}


