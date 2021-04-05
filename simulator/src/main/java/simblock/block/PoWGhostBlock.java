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
 * The representation of a block.
 */
public class PoWGhostBlock extends ProofOfWorkBlock{
  /**
   * Unique uncleA.
   */
  private final PoWGhostBlock uncleA;
  /**
   * Unique uncleB.
   */
  private final PoWGhostBlock uncleB;

  private static BigInteger genesisNextDifficulty;//@override ProofOfWorkBlock

  /**
   * Instantiates a new Block.
   *
   * @param parent the parent
   * @param minter the minter
   * @param time   the time
   */
  public PoWGhostBlock(PoWGhostBlock parent, Node minter, long time, BigInteger difficulty, PoWGhostBlock uncleA, PoWGhostBlock uncleB) {
    super(parent, minter, time, difficulty);
    this.uncleA = uncleA;
    this.uncleB = uncleB;

  }
  /**
   * Generates the genesis block, gets the total mining power and adjusts the difficulty of the
   * next block accordingly.
   * @override
   * @param minter the minter
   * @return the genesis block
   */
  public static PoWGhostBlock genesisBlock(Node minter) {
    long totalMiningPower = 0;
    for (Node node : getSimulatedNodes()) {
      totalMiningPower += node.getMiningPower();
    }
    genesisNextDifficulty = BigInteger.valueOf(totalMiningPower * getTargetInterval());
    return new PoWGhostBlock(null, minter, 0, BigInteger.ZERO, null, null);
  }

}
