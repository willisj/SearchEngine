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
		
		System.out.println(matrix.comparePages(0,2));

	}



}
