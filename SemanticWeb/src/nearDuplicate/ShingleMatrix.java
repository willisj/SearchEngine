package nearDuplicate;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ShingleMatrix {

	// Point: ([page id], [list of shingle ids])
	Map<Point, Boolean> matrix = new HashMap<Point, Boolean>();
	Vector<String> shingles = new Vector<String>();
	final int shingleCount;

	/*
	 * @shingleCount The number of shingles per page
	 */
	public ShingleMatrix(int shingleCount) {
		this.shingleCount = shingleCount;
	}

	/*
	 * Adds a page's entries to the matrix Returns the internal ID of this page
	 */
	public int addPage(String shingle) {

		int pageId = shingles.size();
		shingles.add(shingle);

		// iterate over all points
		for (int shingleIndex = 0; shingleIndex < shingleCount; ++shingleIndex)
			if (shingle.charAt(shingleIndex) == '1')
				matrix.put(new Point(pageId, shingleIndex), true);

		return pageId;
	}

	/*
	 * Given two pageIds return (0 <= x <= 1) the decimal percentage of the
	 * match ie 0.5 -> 50% match
	 */
	public double comparePages(int a, int b) {
		// Todo: use a sample rather than all the shingles

		int hits = 0;
		for (int i = 0; i < shingleCount; ++i) {
			if (matrix.containsKey(new Point(a, i))
					&& matrix.containsKey(new Point(b, i)))
				++hits;
		}
		return (double) hits / shingleCount;
	}
}
