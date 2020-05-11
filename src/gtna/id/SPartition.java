/* ===========================================================
 * GTNA : Graph-Theoretic Network Analyzer
 * ===========================================================
 *
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors
 *
 * Project Info:  http://www.p2p.tu-darmstadt.de/research/gtna/
 *
 * GTNA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GTNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * ---------------------------------------
 * IDSpacePartition.java
 * ---------------------------------------
 * (C) Copyright 2009-2012, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: Andreas Höfer;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.id;

/**
 * @author Andreas Höfer
 * 
 * Partition with short as base tyoe
 */

public abstract class SPartition extends Partition {
	/**
	* @param id
	* @return distance from this partition to the identifier $id
	*/
	public abstract short distance(SIdentifier id);
	
	/**
	* @param p
	* @return distance from this partition to the partition $p
	*/
	public abstract short distance(SPartition p);

	@Override
	public boolean isCloser(Identifier to, Identifier than) {
		return this.distance((SIdentifier) to) < this
				.distance((SIdentifier) than);
	}

	@Override
	public int getClosestNode(int[] nodes, Partition[] partitions) {
		if (nodes.length <= 0) {
			return -1;
		}
		int closest = nodes[0];
		double distance = ((SPartition) partitions[closest]).distance(this);

		for (int i = 1; i < nodes.length; i++) {
			double d = ((SPartition) partitions[nodes[i]]).distance(this);
			if (d < distance) {
				closest = nodes[i];
				distance = d;
			}
		}
		return closest;
	}

}