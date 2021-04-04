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

/**
 * The representation of a block.
 */
public class GhostBlock extends Block{
  /**
   * Unique uncleA.
   */
  private final GhostBlock uncleA;
  /**
   * Unique uncleB.
   */
  private final GhostBlock uncleB;

  /**
   * Instantiates a new Block.
   *
   * @param parent the parent
   * @param minter the minter
   * @param time   the time
   */
  public GhostBlock(Block parent, Node minter, long time, GhostBlock uncleA, GhostBlock uncleB) {

    super( parent, minter, time );
    this.uncleA = uncleA;
    this.uncleB = uncleB;

  }

}
