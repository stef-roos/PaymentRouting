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
 * MaxNormBIdentifier.java
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
package gtna.id.prefix;

import gtna.id.SIdentifier;
import gtna.id.Identifier;

/**
 * @author Andreas Höfer
 *
 */
public class PrefixSIdentifier extends SIdentifier {

	private short[] pos;
	private boolean set = true;
	private boolean bitseqeunce = true;
	
	/**
	 * @param string
	 */
	public PrefixSIdentifier(String string) {
		//System.out.println(string);
		if (string.equals("null")){
			this.set = false;
		} 
		else { 
			if (this.bitseqeunce){
				if (string.equals("")){
					pos = new short[0];
				} 
				else {
					pos = new short[string.length()]; 
					for (int i=0; i< string.length(); i++){
						pos[i] = Short.parseShort(string.substring(i,i+1));
					}
				}
			} else {
			if (string.equals("()")){
				pos = new short[0];
			} 
			else {
				string = string.substring(1, string.length()-1);
				String[] substrings = string.split(",");
				pos = new short[substrings.length]; 
				for (int i=0; i< substrings.length; i++){
					pos[i] = Short.parseShort(substrings[i].trim());
				}
			}
			}
		}
	}	

	public String toString(){
		if (set){
			if (this.bitseqeunce){
				String s = "";
				for (int i=0; i<pos.length; i++){
					s = s + pos[i];
				}
				return s;
			}else {
			StringBuilder strb = new StringBuilder("(");
			for (int i=0; i<pos.length -1; i++){
				strb.append(pos[i] + ", ");
			}
			if (pos.length > 0) 
				strb.append(pos[pos.length-1] + ")");
			else
				strb.append(")");
			return strb.toString();
			}
		}
		return "null";
	}
	

	public PrefixSIdentifier(short[] pos) {
		this.pos = pos;
		if (pos == null){
			this.set = false;
		}
	}
	
	/**
	 * @param id
	 * 
	 * Copy constructor
	 */
	public PrefixSIdentifier(PrefixSIdentifier id) {
		this.pos = id.pos.clone();
		this.set = id.set;
	}

	/* (non-Javadoc)
	 * @see gtna.id.Identifier#distance(gtna.id.Identifier)
	 */
	@Override
	public short distance(SIdentifier id) {
		short[] otherpos = ((PrefixSIdentifier) id).pos;
		if (!this.set || !(((PrefixSIdentifier) id).set)){
			return Short.MAX_VALUE;
		}
		int commonlength = (pos.length < otherpos.length) ? pos.length : otherpos.length; 
		// find the length of the common prefix 
		int commonprefix = 0;
		for (int i=0; i < commonlength; i++){
			if (pos[i] == otherpos[i])
				commonprefix++;
			else
				break;			
		}
		// dist = |pos| + |otherpos| - 2 commonprefix
		return (short) (pos.length + otherpos.length - 2 * commonprefix);
	}

	/* (non-Javadoc)
	 * @see gtna.id.Identifier#equals(gtna.id.Identifier)
	 */
	@Override
	public boolean equals(Identifier id) {
		short[] otherpos = ((PrefixSIdentifier) id).pos;
		if (pos.length != otherpos.length)
			return false;
		for (int i=0; i < pos.length; i++)
			if (pos[i] != otherpos[i])
				return false;
		return true;
	}

	public boolean isSet() {
		return this.set;
	}

	public void setSet(boolean set) {
		this.set = set;
	}

	public boolean isPrefixTo(PrefixSIdentifier s){
		short[] otherpos = s.getPos();
		if (otherpos.length < pos.length) return false;
		for (int i = 0; i < pos.length; i++){
			if (otherpos[i] != pos[i]){
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see gtna.id.Identifier#asString()
	 */
	@Override
	public String asString() {
		return this.toString();
	}
	
	public short[] getPos(){
		return pos;
	}

}
