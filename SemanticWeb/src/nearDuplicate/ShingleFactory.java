package nearDuplicate;

import java.util.Random;

/**
 * @author Jordan
 * 
 */
public class ShingleFactory {

	ShingleBuildingMethod method;
	static final int shinglesPerDocument = 20;

	ShingleFactory(ShingleBuildingMethod method) {
		this.method = method;
	}

	public String[] shingleDocument(String body, int k) {
		String[] shingles = new String[shinglesPerDocument];
		switch (method) {
		case STARTATSTOPWORDS:
			break;
		case RANDOMSTART:
			return produceRandomShingles(body,k);
		}

		return shingles;
	}

	/**
	 * @param body
	 *            the document to be shingled
	 * @param k
	 *            the number of characters per shingle
	 * @return the array of constructed shingles
	 */
	public String[] produceRandomShingles(String body, int k) {
		String[] shingles = new String[shinglesPerDocument];
		Random rand = new Random();
		int start;
		
		
		

		for (int i = 0; i < shinglesPerDocument; ++i) {
			start = rand.nextInt(body.length() - k) ;
			
			//rewind 
			while(start > 0 && !wordBoundryChar(body.charAt(start-1)))
				--start;
			shingles[i] = body.substring(start, start + k);
		}
		return shingles;
	}
	
	public boolean wordBoundryChar(char c){
		return (c == ' ' || c == '-' || c == '.' || c == '\'' || c == '"' || c == '\t' || c == '\n' || c == '?' || c == '!' );
		
	}
}
