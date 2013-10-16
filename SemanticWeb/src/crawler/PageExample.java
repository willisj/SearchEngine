package crawler;

import java.net.MalformedURLException;

public class PageExample {

	public static void main(String[] args) {
		Page p;
		try {
			p = new Page("http://www.cs.uwindsor.ca","", -1);
			p.requestPage();
			System.out.println(p.diagString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
