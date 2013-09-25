package controller;

import java.net.MalformedURLException;
import java.util.Vector;

import utilities.util;
import crawler.Crawler;
import crawler.Page;
import nearDuplicate.ShingleFactory;

public class Controller {

	public static void main(String[] args) {
		util.getTimer().start(); // start the timer for the logger
		
		//** CONSTANTS **//
		
		final String CRAWLER_SEED_URL = "http://www1.uwindsor.ca/cs";
		final int MAX_CRAWL_DEPTH = 1;
		final int SHINGLE_K_CONST = 9;
		
		//** END CONSTANTS **//
				
		Crawler c;
		Vector<Page> pages;
		ShingleFactory shingleFact = new ShingleFactory(nearDuplicate.ShingleBuildingMethod.RANDOMSTART);
		
		try {
			c = new Crawler(CRAWLER_SEED_URL);
			pages = c.crawl(MAX_CRAWL_DEPTH);
		} catch (MalformedURLException e) {
			util.writeLog("Problem with the crawler seed URL",true);
			return;
		}
		
		for(Page p: pages){
			System.out.print(p.getUrl().toString() + ": ");
			int[] ids = new int[0];
			if(p.getRawSource() != null)
				ids = shingleFact.shingleDocument(p.getRawSource(), SHINGLE_K_CONST);
			for(int i: ids)
				System.out.print(i+" ");
			System.out.print("\n");
		}
	}

}
