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
 * MaxNormBPartitionSimple.java
 * ---------------------------------------
 * (C) Copyright 2009-2011, by Benjamin Schiller (P2P, TU Darmstadt)
 * and Contributors 
 *
 * Original Author: Andreas Höfer;
 * Contributors:    -;
 *
 * Changes since 2011-05-17
 * ---------------------------------------
 *
 */
package gtna.id.prefix;

import java.util.Random;

import gtna.id.SIdentifier;
import gtna.id.SPartition;
import gtna.id.Identifier;
import gtna.id.Partition;

/**
 * @author Andreas Höfer
 *
 * TODO: STill the same semnatics as PrefixSPartitionSimple, needs to be changed
 */
public class PrefixSPartition extends SPartition {

	private PrefixSIdentifier id;
	
	public PrefixSPartition(PrefixSIdentifier id) {
		this.id = id;
	}

	public String toString() {
		return this.id.toString();
	}

	/* (non-Javadoc)
	 * @see gtna.id.SPartition#distance(gtna.id.SIdentifier)
	 */
	@Override
	public short distance(SIdentifier id) {
		return this.id.distance(id);
	}

	/* (non-Javadoc)
	 * @see gtna.id.SPartition#distance(gtna.id.SPartition)
	 */
	@Override
	public short distance(SPartition p) {
		return this.id.distance((SIdentifier) p.getRepresentativeIdentifier());
	}

	/* (non-Javadoc)
	 * @see gtna.id.Partition#asString()
	 */
	@Override
	public String asString() {
		return id.toString();
	}

	/* (non-Javadoc)
	 * @see gtna.id.Partition#contains(gtna.id.Identifier)
	 */
	@Override
	public boolean contains(Identifier id) {
		return this.id.equals(id);
	}

	/* (non-Javadoc)
	 * @see gtna.id.Partition#getRepresentativeIdentifier()
	 */
	@Override
	public Identifier getRepresentativeIdentifier() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see gtna.id.Partition#getRandomIdentifier(java.util.Random)
	 */
	@Override
	public Identifier getRandomIdentifier(Random rand) {
		return new PrefixSIdentifier(id);
	}

	/* (non-Javadoc)
	 * @see gtna.id.Partition#equals(gtna.id.Partition)
	 */
	@Override
	public boolean equals(Partition p) {
		return this.id.equals((PrefixSIdentifier) p.getRepresentativeIdentifier());
	}
}
