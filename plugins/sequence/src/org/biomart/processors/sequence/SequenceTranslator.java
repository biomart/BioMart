package org.biomart.processors.sequence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.biomart.processors.JGUtils;

/**
 *
 * @author jhsu, jguberman
 */
public class SequenceTranslator {
    private static final String[] aaTables =
    {"FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 0: Standard
            "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSS**VVVVAAAADDEEGGGG", // 1: Vertebrate mitochondrial
            "FFLLSSSSYY**CCWWTTTTPPPPHHQQRRRRIIMMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 2: Yeast Mitochondrial
            "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 3: Mold, Protozoan, and CoelenterateMitochondrial and Mycoplasma/Spiroplasma
            "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSSSVVVVAAAADDEEGGGG", //4: Invertebrate Mitochondrial
            "FFLLSSSSYYQQCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", //5: Ciliate, Dasycladacean and Hexamita Nuclear
            "", "",
            "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG", // 8: Echinoderm Mitochondrial
            "FFLLSSSSYY**CCCWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 9: Euplotid Nuclear
            "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 10: "Bacterial"
            "FFLLSSSSYY**CC*WLLLSPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 11: Alternative Yeast Nuclear
            "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSGGVVVVAAAADDEEGGGG", // 12: Ascidian Mitochondrial
            "FFLLSSSSYYY*CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG", // 13: Flatworm Mitochondrial
            "FFLLSSSSYY*QCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 14: Blepharisma Nuclear
            "FFLLSSSSYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 15: Chlorophycean Mitochondrial
            "", "", "", "",
            "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNNKSSSSVVVVAAAADDEEGGGG", // 20: Trematode Mitochondrial
            "FFLLSS*SYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 21: Scenedesmus obliquus Mitochondrial
            "FF*LSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 22: Thraustochytrium Mitochondrial
    };

    private static final HashMap<Character, String[]> nucleotideTable = new HashMap<Character,String[]>() {{
		put('A', new String[] {"A"});
		put('C', new String[] {"C"});
		put('G', new String[] {"G"});
		put('T', new String[] {"T"});
		put('U', new String[] {"U"});
		put('M', new String[] {"A", "C"});
		put('R', new String[] {"A", "G"});
		put('W', new String[] {"A", "T"});
		put('S', new String[] {"C", "G"});
		put('Y', new String[] {"C", "T"});
		put('K', new String[] {"G", "T"});
		put('V', new String[] {"A", "C", "G"});
		put('H', new String[] {"A", "C", "T"});
		put('D', new String[] {"A", "G", "T"});
		put('B', new String[] {"C", "G", "T"});
		put('X', new String[] {"G", "A", "T", "C"});
		put('N', new String[] {"G", "A", "T", "C"});
    }};

	/**
	 * Translates a DNA sequence to a protein sequence. Currently uses only
	 * the human translation table.
	 * @param untranslated	Sequence to be translated.
	 * @return Translated sequence.
	 */
	public static String translateSequence(String untranslated, HashSet<String> seqEdit, String codonTableID) {
		untranslated = untranslated.toUpperCase();
		if(codonTableID.equals("")){
			codonTableID = "1";
		}
		String aaTable = aaTables[Integer.parseInt(codonTableID)-1];

		// The codon_table_id field doesn't appear to be used, so I'm just directly checking the chromosome name as a stopgap solution
		/*if (chr.equals("MT")){
			aaTable = aaTables[1];
		} else {
			aaTable = aaTables[0];
		}*/
		final String nucleotides[] = {"T","C","A","G"};
		HashMap<String, Character> codonTable = new HashMap<String, Character>();
		int i = 0;
		for(String x : nucleotides){
			for(String y : nucleotides){
				for(String z : nucleotides){
					codonTable.put(x+y+z, aaTable.charAt(i));
					++i;
				}
			}
		}

		StringBuilder translated = new StringBuilder();
		Character nextAA = null;
        int n = untranslated.length();
		for(int j = 0; j+3 <= n; j += 3){
			String currentCodon = untranslated.substring(j, j+3);
			nextAA = codonTable.get(currentCodon);
			if(nextAA==null){
				// Ambiguous codon
				nextAA='X';
				HashSet<Character> ambiguous = new HashSet<Character>();
                // Do some checks due to NullPointer problems when doing nucleotide lookup
                if (nucleotideTable.get(currentCodon.charAt(0)) == null) {
                    System.err.println(String.format("Could not lookup %s in nucleotide table", currentCodon.charAt(0)));
                } else {
                    for(String x : nucleotideTable.get(currentCodon.charAt(0))){
                        if (nucleotideTable.get(currentCodon.charAt(1)) == null) {
                            System.err.println(String.format("Could not lookup %s in nucleotide table", currentCodon.charAt(1)));
                        } else {
                            for(String y : nucleotideTable.get(currentCodon.charAt(1))){
                                if (nucleotideTable.get(currentCodon.charAt(2)) == null) {
                                    System.err.println(String.format("Could not lookup %s in nucleotide table", currentCodon.charAt(2)));
                                } else {
                                    for(String z : nucleotideTable.get(currentCodon.charAt(2))){
                                        ambiguous.add(codonTable.get(x+y+z));
                                    }
                                }
                            }
                        }
                    }
                }
				if(ambiguous.size()==1){
					for(char dummy : ambiguous){
						nextAA = dummy;
					}
				} else if (ambiguous.size()==2){
					if(ambiguous.contains('D') && ambiguous.contains('N')){
						nextAA = 'B';
					} else if(ambiguous.contains('E') && ambiguous.contains('Q')){
						nextAA= 'Z';
					}
				}
			}
			translated.append(nextAA);
		}
		if (untranslated.length() % 3 == 2){
			String overhang = untranslated.substring(untranslated.length()-2,untranslated.length());
			HashSet<Character> ambiguous = new HashSet<Character>();
			for(String x : nucleotides){
				ambiguous.add(codonTable.get(overhang + x));
			}
			if(ambiguous.size()==1){
				for(char dummy : ambiguous){
					translated.append(dummy);
				}
			} else if (ambiguous.size()==2){
				if(ambiguous.contains('D') && ambiguous.contains('N')){
					translated.append("B");
				} else if(ambiguous.contains('E') && ambiguous.contains('Q')){
					translated.append("Z");
				}
			}
		}
		for(String curSeqEdit : seqEdit){
			if(!(curSeqEdit.equals(""))){
				List<String> splitSeqEdit = JGUtils.splitLine(" ", curSeqEdit);
				translated.replace(Integer.parseInt(splitSeqEdit.get(0))-1, Integer.parseInt(splitSeqEdit.get(1)), splitSeqEdit.get(2));
			}
		}
		return translated.toString();
	}
}
