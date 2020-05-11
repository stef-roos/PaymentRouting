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
 * PrefixSIdentiferSpaceSimple.java
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

import gtna.graph.Graph;
import gtna.id.Identifier;
import gtna.id.IdentifierSpace;
import gtna.id.Partition;
import gtna.id.SIdentifierSpace;
import gtna.io.Filereader;
import gtna.io.Filewriter;
import gtna.util.Config;

import java.io.IOException;
import java.util.Random;

/**
 * @author Andreas Höfer
 *
 */
public class PrefixSIdentiferSpace extends SIdentifierSpace{
	
    private int size;
    private int bitsPerCoord;
	private boolean virtual;
	
	
	public PrefixSIdentiferSpace(PrefixSPartitionSimple[] partitions, int bitsPerCoord, int size, boolean virtual){
		super(partitions);
		this.bitsPerCoord = bitsPerCoord;
		this.size = size;
		this.virtual = virtual;
	}
	
	public int getSize() {
		return this.size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getBitsPerCoord() {
		return this.bitsPerCoord;
	}
	public void setBitsPerCoord(int bitsPerCoord) {
		this.bitsPerCoord = bitsPerCoord;
	}
	
	/* (non-Javadoc)
	 * @see gtna.id.IdentifierSpace#getMaxDistance()
	 */
	@Override
	public short getMaxDistance() {
		return Short.MAX_VALUE;
	}

	/**
	 * @return the virtual
	 */
	public boolean isVirtual() {
		return virtual;
	}
	/**
	 * @param virtual the virtual to set
	 */
	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	/* (non-Javadoc)
	 * @see gtna.id.IdentifierSpace#writeParameters(gtna.io.Filewriter)
	 */
	@Override
	protected void writeParameters(Filewriter fw) {
        // SIZE	
		this.writeParameter(fw, "size", this.size);

		// BITSPERCOORD
		this.writeParameter(fw, "bitsPerCoord", this.bitsPerCoord);
		
		// Type
		this.writeParameter(fw, "virtual ", this.virtual);
	}

	/* (non-Javadoc)
	 * @see gtna.id.IdentifierSpace#readParameters(gtna.io.Filereader)
	 */
	@Override
	protected void readParameters(Filereader fr) {
		
		// SIZE
		this.size = Integer.parseInt(fr.readLine());
				
		// BITSPERCOORD
		this.bitsPerCoord = Integer.parseInt(fr.readLine());
				
		// Type
		this.virtual = Boolean.parseBoolean(fr.readLine());
	}


	/* (non-Javadoc)
	 * @see gtna.id.IdentifierSpace#getRandomIdentifier(java.util.Random)
	 */
	@Override
	public Identifier getRandomIdentifier(Random rand) {
		return this.partitions[rand.nextInt(this.partitions.length)].getRepresentativeIdentifier();
	}
}
