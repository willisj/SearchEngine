package nearDuplicate;

public class NearDuplicateExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ShingleMatrix matrix = new ShingleMatrix(8);
		matrix.addPage("11111111");
		matrix.addPage("10101010");
		matrix.addPage("01010101");
		matrix.addPage("00000000");

		System.out.println(matrix.comparePages(0, 2));

		ShingleFactory factory = new ShingleFactory(
				ShingleBuildingMethod.RANDOMSTART);

		int[] shingles = factory
				.shingleDocument(
						"Arriving with the Iranian supreme leader's blessing to show \"heroic flexibility\" "
								+ "in global diplomacy, and having built up to his U.N. General Assembly appearance "
								+ "with weeks of conciliatory gestures, tweets and media engagement, Iranian President "
								+ "Hassan Rouhani carried the prospect of a sudden breakthrough in the 34-year U.S.-Iran "
								+ "impasse in his right hand. But despite fevered global attention, no dramatic handshake "
								+ "with President Obama ever happened.", 9);

		System.out.print("Shingle Ids: ");
		for (int s : shingles)
			System.out.print(s + " ");
		System.out.print("\n");

	}

}
