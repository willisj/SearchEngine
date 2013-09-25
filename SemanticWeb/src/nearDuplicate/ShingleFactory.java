package nearDuplicate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Jordan
 * 
 */
public class ShingleFactory {

	ShingleBuildingMethod method;
	static final int shinglesPerDocument = 20;
	private int nextShingleID =0; 
	Map<String, Integer> shingleRef;

	ShingleFactory(ShingleBuildingMethod method) {
		this.method = method;
		shingleRef = new HashMap<String,Integer>();
	}

	public int[] shingleDocument(String body, int k) {
		String[] shingles = new String[shinglesPerDocument];
		int[] shingleIDs = new int[shinglesPerDocument];
		
		switch (method) {
			case STARTATSTOPWORDS:
				break;
			case RANDOMSTART:
				shingles = produceRandomShingles(body, k);
		}
		
		
		// register the shingles 
		int i=0;
		for(String s: shingles)
			shingleIDs[i++] = registerShingle(s);			

		return shingleIDs;
	}

	/**
	 * @param body
	 *            the document to be shingled
	 * @param k
	 *            the number of words per shingle
	 * @return the array of constructed shingles
	 */
	public String[] produceRandomShingles(String body, int k) {
		String[] shingles = new String[shinglesPerDocument];
		Random rand = new Random();
		int start;
		int end;
		int wordCount;
		for (int i = 0; i < shinglesPerDocument; ++i) {
			start = end = rand.nextInt(body.length());
			wordCount = 0;

			// rewind start to the begining of the word
			while (start > 0 && !wordBoundryChar(body.charAt(start - 1)))
				--start;

			// while we don't have all k words
			while (wordCount < k) {

				// if the current letter is a word boundary break
				while (end < body.length() - 1
						&& !wordBoundryChar(body.charAt(++end)))
					;

				++wordCount; // that counts as a word

				// continue forward until we find the next word start
				while (end < body.length() - 1
						&& wordBoundryChar(body.charAt(end)))
					++end;
			}
			
			//rewind start on top of the next word
			if(wordCount < k)
				while (start > 0
						&& wordBoundryChar(body.charAt(--start)))
					;
			
			// if we don't have k words yet
			while (wordCount < k && start > 0) {
				while (start > 0
						&& !wordBoundryChar(body.charAt(--start)))
					;

				++wordCount; // that counts as a word

				// continue back until we find the next word ending
				while (start > 0
						&& wordBoundryChar(body.charAt(start)))
					--start;
			}
			

			shingles[i] = body.substring(start, end);
		}
		return shingles;
	}

	public int registerShingle(String s){
		if(shingleRef.containsKey(s)){
			return shingleRef.get(s);
		}
		shingleRef.put(s, nextShingleID);
		return ++nextShingleID;
	}
	public boolean wordBoundryChar(char c) {
		return (c == ' ' || c == '.' || c == '"' || c == '\t' || c == '\n'
				|| c == '?' || c == '!');

	}
}
