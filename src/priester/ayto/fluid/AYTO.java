package priester.ayto.fluid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import priester.ayto.fluid.OddFactorialCombinationGenerator.OddFactorialCombinationListener;

public class AYTO implements OddFactorialCombinationListener {
	private static final Logger LOGGER = Logger.getLogger(AYTO.class);

	private String[] keys;
	private int numPairs;
	private String[][] pairPopulation;
	private int[][] pairIdLookupTable;
	private boolean[] truthBoothLookupTable;
	private List<MatchCeremony> matchCeremonies = new ArrayList<>();;
	private int[] results;

	public static void main(String[] args) {
		AYTO ayto = new AYTO("../config/season8.xml");
		ayto.calc();

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}
	}

	public AYTO(String xmlFile) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {
				private boolean processCharacters;
				private StringBuilder tagBody;

				private List<String> pairStrings = new ArrayList<>();
				private boolean perfectMatch;
				private int correct;

				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					switch (qName) {
					case "Keys":
					case "Pair":
					case "Result":
					case "Match":
					case "Correct":
						processCharacters = true;
						tagBody = new StringBuilder(512);
						break;
					case "TruthBooth":
					case "MatchCeremony":
						pairStrings.clear();
						break;
					}
				}

				public void endElement(String uri, String localName, String qName) throws SAXException {
					String tagBody = getTagBody();
					processCharacters = false;

					switch (qName) {
					case "Keys":
						keys = tagBody.split(" ");
						numPairs = keys.length / 2;
						pairPopulation = new String[keys.length * (keys.length - 1) / 2][2];
						pairIdLookupTable = new int[keys.length][keys.length];
						for (int[] row : pairIdLookupTable)
							Arrays.fill(row, -1);
						int counter = 0;
						for (int i = 0; i < keys.length - 1; i++) {
							for (int j = i + 1; j < keys.length; j++) {
								pairPopulation[counter] = new String[] { keys[i], keys[j] };
								pairIdLookupTable[i][j] = counter;
								pairIdLookupTable[j][i] = counter;
								counter++;
							}
						}
						truthBoothLookupTable = new boolean[pairPopulation.length];
						results = new int[pairPopulation.length];
						break;
					case "Pair":
						pairStrings.add(tagBody);
						break;
					case "Result":
						perfectMatch = tagBody.equals("Match");
						break;
					case "TruthBooth":
						String[] pair = pairStrings.get(0).split(" ");
						int key0 = indexOf(keys, pair[0]);
						int key1 = indexOf(keys, pair[1]);
						int pairId = pairIdLookupTable[key0][key1];
						if (!perfectMatch)
							truthBoothLookupTable[pairId] = true;
						else {
							for (int i = 0; i < keys.length; i++) {
								if (key0 != i && i != key1)
									truthBoothLookupTable[pairIdLookupTable[key0][i]] = true;
								if (key1 != i && i != key0)
									truthBoothLookupTable[pairIdLookupTable[key1][i]] = true;
							}
						}
						break;
					case "Correct":
						correct = Integer.parseInt(tagBody);
						break;
					case "MatchCeremony":
						matchCeremonies.add(AYTO.this.new MatchCeremony(getPairIdArray(pairStrings), correct));
						break;
					}
				}

				public void characters(char[] ch, int start, int length) throws SAXException {
					if (processCharacters)
						tagBody.append(ch, start, length);
				}

				private String getTagBody() {
					String str = null;
					if (processCharacters) {
						StringBuffer filteredTagBody = new StringBuffer(tagBody.length());
						char prev = ' ';
						for (int i = 0; i < tagBody.length(); i++) {
							char curr = tagBody.charAt(i);
							if ((int) curr == 9)
								curr = ' ';

							if (Character.isLetterOrDigit(curr) || (curr == ' ' && prev != ' ')) {
								filteredTagBody.append(curr);
								prev = curr;
							}
						}
						if (filteredTagBody.charAt(filteredTagBody.length() - 1) == ' ')
							filteredTagBody.deleteCharAt(filteredTagBody.length() - 1);
						str = filteredTagBody.toString();
					}
					return str;
				}

				private int getPairId(String pair) {
					String[] pairArray = pair.split(" ");
					return pairIdLookupTable[indexOf(keys, pairArray[0])][indexOf(keys, pairArray[1])];
				}

				private int[] getPairIdArray(List<String> pairs) {
					int[] pairIdArray = new int[pairs.size()];
					for (int i = 0; i < pairs.size(); i++)
						pairIdArray[i] = getPairId(pairs.get(i));
					return pairIdArray;
				}

				private int indexOf(String[] array, String str) {
					for (int i = 0; i < array.length; i++) {
						if (array[i].equals(str))
							return i;
					}
					return -1;
				}
			};

			saxParser.parse(xmlFile, handler);
		} catch (Exception e) {
			LOGGER.error(e, e);
		}
	}

	private void calc() {
		try {
			OddFactorialCombinationGenerator generator = new OddFactorialCombinationGenerator(this);
			generator.generate(keys.length);
		} catch (Exception e) {
			LOGGER.error(e, e);
		}
	}

	@Override
	public void onNextCombination(int[] combination) {
		int[] pairCombination = new int[numPairs];
		for (int i = 0; i < numPairs; i++)
			pairCombination[i] = pairIdLookupTable[combination[i * 2]][combination[i * 2 + 1]];

		if (isPossible(pairCombination)) {
			for (int i = 0; i < numPairs; i++)
				results[pairCombination[i]]++;
		}
	}

	private boolean isPossible(int[] pairCombination) {
		for (int i = 0; i < numPairs; i++) {
			if (truthBoothLookupTable[pairCombination[i]])
				return false;
		}

		for (int i = 0; i < matchCeremonies.size(); i++) {
			if (!isPossible(pairCombination, matchCeremonies.get(i)))
				return false;
		}
		return true;
	}

	private boolean isPossible(int[] pairCombination, MatchCeremony matchCeremony) {
		int theoCorrect = 0;
		for (int i = 0; i < numPairs; i++) {
			for (int j = 0; j < numPairs; j++) {
				if (pairCombination[i] == matchCeremony.pairIdArray[j]) {
					theoCorrect++;
					break;
				}
			}
		}
		return theoCorrect == matchCeremony.correct;
	}

	@Override
	public void onComplete() {
		for (int i = 0; i < results.length; i++)
			LOGGER.info(pairPopulation[i][0] + " " + pairPopulation[i][1] + ": " + results[i]);
	}

	private class MatchCeremony {
		private final int[] pairIdArray;
		private final int correct;

		public MatchCeremony(int[] pairIdArray, int correct) {
			this.pairIdArray = pairIdArray;
			this.correct = correct;
		}
	}
}