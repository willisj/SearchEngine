package crawler;

import java.net.MalformedURLException;
import java.net.URL;

public class PageExample {

	public static void main(String[] args) {
		Page p;
		try {
			p = new Page(new URL("http://www.cs.uwindsor.ca"),"", -1);
			p.requestPage();
			System.out.println(p.diagString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
