package crawler;

import java.net.MalformedURLException;
import utilities.util;

public class CrawlerExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		util.getTimer().start();
		Crawler c;
		try {
			c = new Crawler("http://www1.uwindsor.ca/cs/");
			//c.addURL("http://www.google.ca",-1);
			//c.addURL("http://www.godaddy.com",-1);
			//c.addURL("http://www.reddit.com",-1);
			c.crawl(3);			
		} catch (MalformedURLException e) {}
		

		

	}

}
