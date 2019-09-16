package priester.ayto.hetero;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import priester.ayto.hetero.PermutationGenerator.PermutationListener;

public class AYTO implements PermutationListener {
	private static final Logger LOGGER = Logger.getLogger(AYTO.class);

	private String[] keys;
	private String[] values;
	private int[] pairs;
	private MatchCeremony[] matchCeremonies;
	private TruthBooth[] truthBooths;

	public static void main(String[] args) {
		AYTO ayto = new AYTO("../config/season7.xml");
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
				private String key;
				private String value;
				private boolean perfectMatch;
				private List<TruthBooth> truthBoothList = new ArrayList<>();
				private int[] matches;
				private int correct;
				private List<MatchCeremony> matchCeremonyList = new ArrayList<>();

				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					switch (qName) {
					case "Keys":
					case "Values":
					case "Key":
					case "Value":
					case "Result":
					case "Matches":
					case "Correct":
						processCharacters = true;
						tagBody = new StringBuilder(512);
						break;
					}
				}

				public void endElement(String uri, String localName, String qName) throws SAXException {
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

					switch (qName) {
					case "Keys":
						keys = str.toString().split(" ");
						pairs = new int[keys.length * keys.length];
						break;
					case "Values":
						values = str.split(" ");
						break;
					case "Key":
						key = str;
						break;
					case "Value":
						value = str;
						break;
					case "Result":
						perfectMatch = str.equals("Match");
						break;
					case "TruthBooth":
						truthBoothList.add(
								AYTO.this.new TruthBooth(indexOf(keys, key), indexOf(values, value), perfectMatch));
						break;
					case "TruthBooths":
						truthBooths = truthBoothList.toArray(new TruthBooth[truthBoothList.size()]);
						break;
					case "Matches":
						String[] array = str.split(" ");
						matches = new int[array.length];
						for (int i = 0; i < array.length; i++)
							matches[i] = indexOf(values, array[i]);
						break;
					case "Correct":
						correct = Integer.parseInt(str);
						break;
					case "MatchCeremony":
						matchCeremonyList.add(AYTO.this.new MatchCeremony(matches, correct));
						break;
					case "MatchCeremonies":
						matchCeremonies = matchCeremonyList.toArray(new MatchCeremony[matchCeremonyList.size()]);
						break;
					}

					processCharacters = false;
				}

				public void characters(char[] ch, int start, int length) throws SAXException {
					if (!processCharacters)
						return;

					tagBody.append(ch, start, length);
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
			LOGGER.error(e);
		}
	}

	private void calc() {
		try {
			PermutationGenerator generator = new PermutationGenerator(this);
			generator.generate(keys.length);
		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

	@Override
	public void onNextPermutation(int[] permutation) {
		if (isPossible(permutation)) {
			for (int i = 0; i < permutation.length; i++)
				pairs[i * permutation.length + permutation[i]]++;
		}
	}

	private boolean isPossible(int[] permutation) {
		for (int i = 0; i < truthBooths.length; i++) {
			if (!isPossible(permutation, truthBooths[i]))
				return false;
		}
		for (int i = 0; i < matchCeremonies.length; i++) {
			if (!isPossible(permutation, matchCeremonies[i]))
				return false;
		}
		return true;
	}

	private boolean isPossible(int[] permutation, TruthBooth truthBooth) {
		return (permutation[truthBooth.key] == truthBooth.value && truthBooth.perfectMatch)
				|| (permutation[truthBooth.key] != truthBooth.value && !truthBooth.perfectMatch);
	}

	private boolean isPossible(int[] permutation, MatchCeremony matchCeremony) {
		int theoCorrect = 0;
		for (int i = 0; i < permutation.length; i++) {
			if (permutation[i] == matchCeremony.matches[i])
				theoCorrect++;
		}
		return theoCorrect == matchCeremony.correct;
	}

	@Override
	public void onComplete() {
		for (int i = 0; i < keys.length; i++) {
			for (int j = 0; j < values.length; j++)
				LOGGER.info(keys[i] + " " + values[j] + ": " + pairs[i * keys.length + j]);
		}
	}

	private class TruthBooth {
		private final int key;
		private final int value;
		private final boolean perfectMatch;

		public TruthBooth(int key, int value, boolean perfectMatch) {
			this.key = key;
			this.value = value;
			this.perfectMatch = perfectMatch;
		}
	}

	private class MatchCeremony {
		private final int[] matches;
		private final int correct;

		public MatchCeremony(int[] matches, int correct) {
			this.matches = matches;
			this.correct = correct;
		}
	}
}