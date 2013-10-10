package controller;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.lang3.time.StopWatch;

import utilities.util;
import crawler.Crawler;
import crawler.Page;
import nearDuplicate.ShingleFactory;

public class Controller {

	public static void main(String[] args) {
		util.getTimer().start(); // start the timer for the logger
		StopWatch sw = new StopWatch();

		// ** CONSTANTS **//

		final String CRAWLER_SEED_URL = "http://www1.uwindsor.ca";
		final int MAX_CRAWL_DEPTH = 1;
		final int SHINGLE_K_CONST = 4;

		// ** END CONSTANTS **//

		Crawler c;
		HashMap<Integer, Page> pages;
		ShingleFactory shingleFact = new ShingleFactory(
				nearDuplicate.ShingleBuildingMethod.RANDOMSTART);

		try {
			c = new Crawler(CRAWLER_SEED_URL);
			pages = c.crawl(MAX_CRAWL_DEPTH);
		} catch (MalformedURLException e) {
			util.writeLog("Problem with the crawler seed URL", true);
			return;
		}

		util.writeLog("Creating Shingles. K: " + SHINGLE_K_CONST
				+ " ShinglesPerDoc: " + shingleFact.shinglesPerDocument);
		sw.start();
		for (int i : pages.keySet()) {
			int[] ids = new int[0];
			Page p = pages.get(i);
			if (p.getRawSource() != null)
				ids = shingleFact.shingleDocument(p.getPageID(),
						p.getStrippedSource(), SHINGLE_K_CONST);
		}
		sw.stop();
		util.writeLog("Shingles created in " + sw.toString());
		// shingleFact.printShinglesPerPage();
		double match;

		for (int a : pages.keySet())
			for (int b : pages.keySet()) {
				if (a == b)
					continue;
				match = shingleFact.comparePages(a, b);
				if (match > 0) {
					util.writeLog((match * 100) + "% A: "
							+ pages.get(a).getUrl().toString() + "\tB: "
							+ pages.get(b).getUrl().toString());
				}

			}
	}
}
