/**
 * 
 */
package crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.lang3.time.StopWatch;

import utilities.util;

/**
 * @author Jordan
 * 
 */
public class Crawler extends Thread {

	final int MAX_THREADS = 10;
	final boolean STAY_IN_DOMAIN = false;

	// Error Settings
	final boolean SHOW_THREAD_COMPLETED = true;
	final boolean SHOW_NEW_DOMAIN_FOUND = true;

	final boolean PRINT_CACHE_SIZE = true;
	final boolean PRINT_SUMMARY = true;

	final private String seed; // the seed url
	private Page seedPage;

	// pages that have yet to be requested
	HashMap<String, Vector<Page>> urlPool = new HashMap<String, Vector<Page>>();

	// pages that have been requested
	Vector<String> requestedPages = new Vector<String>();

	// pages currently threaded to request pages
	Vector<Thread> running = new Vector<Thread>();

	// lookup table Thread -> Page
	HashMap<String, Page> runningRef = new HashMap<String, Page>();

	// URLs that have already been scraped
	Vector<String> seenUrls = new Vector<String>();

	private static Random rand = new Random();

	/**
	 * @param seed
	 *            the url from which to start scraping
	 * @throws MalformedURLException
	 *             invalid seed url
	 */
	public Crawler(String seed) throws MalformedURLException {
		this.seed = seed;
		seedPage = addURL(new URL(seed), 0);
		util.writeLog("Seed Domain Set: " + seedPage.getDomain());

	}

	/**
	 * @param url
	 *            the url to be added
	 * @param depth
	 *            the depth of this page from the seed (set -1 if N/A)
	 * @return true if the URL was added
	 */
	public Page addURL(URL url, int depth) {
		try {
			Page p = new Page(url, "", depth);
			if (!urlPool.containsKey(p.getUrl().getHost())) {
				util.writeLog("Domain Found: " + p.getUrl().getHost());
				urlPool.put(p.getUrl().getHost(), new Vector<Page>());
			}
			urlPool.get(p.getUrl().getHost()).add(p);
			return p;
		} catch (MalformedURLException e) {
			return null;
		}

	}

	private void addAllURLs(Vector<Page> urls) {
		for (Page p : urls) {
			if (!urlPool.containsKey(p.getUrl().getHost())) {
				urlPool.put(p.getUrl().getHost(), new Vector<Page>());
				util.writeLog("Domain Found: " + p.getUrl().getHost());
			}
			urlPool.get(p.getUrl().getHost()).add(p);
		}
	}

	private Page getRandomURL() {
		Vector<Page> v;
		Page p;

		do {
			v = urlPool.get(urlPool.keySet().toArray()[rand.nextInt(urlPool
					.keySet().size())]);
		} while (v.size() < 0 && urlPool.size() > 0);

		p = v.remove(rand.nextInt(v.size()));
		if (v.size() == 0) {
			urlPool.remove(p.getUrl().getHost());
		}

		return p;
	}

	/**
	 * 
	 * @param maxDepth
	 *            The max depth the spider will search
	 */
	public Vector<String> crawl(int maxDepth) {

		StopWatch timer = new StopWatch();
		timer.start();
		// while we have work to do
		while (urlPool.size() > 0 || running.size() > 0) {

			// if there are some running threads to check up on
			if (running.size() > 0) {

				// check the top thread to see if it's done
				for (int i = 0; i < running.size(); ++i) {
					if (running.get(i).getState() == Thread.State.TERMINATED) {
						Thread t = running.remove(i);
						try {
							// join with the thread to remove it (by collecting
							// it's
							// exit code)
							t.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} finally {
							Page p = runningRef.get(t.getName());

							if (SHOW_THREAD_COMPLETED)
								util.writeLog("Thread "
										+ (requestedPages.size() + 1)
										+ " Completed: " + p.getUrl());

							// if we haven't reached the max depth yet process
							// the
							// children
							if (p.getCrawlDepth() < maxDepth)
								addAllURLs(p.makePagesFromChildren(seenUrls));

							// move the page from the runningRef to the
							// requested
							// pages
							runningRef.remove(p);
							// requestedPages.put(p.getPageID(), p);

							if (p.save())
								requestedPages.add(p.getUrl().toString());
							p = null;
							if (requestedPages.size() % 500 == 0) {
								util.writeLog("Requested: "
										+ requestedPages.size() + "\tPool: "
										+ urlPool.size());
								util.writeLog("Running Garbage Collection.");
								System.gc();
							}
						}
						break;
					}
				}
			}
			// if there are pages waiting to be requested
			// and we're under the MAX_THREADS limit
			if (urlPool.size() > 0 && running.size() <= MAX_THREADS) {
				// while and urls still exist
				Thread t = getNextUrlFromPool();
				t.start();
			} else { // wait for more urls
				// TODO: what should we be doing while the urlpool is empty?
			}
		}

		// calculate and display the running time
		timer.stop();
		if (PRINT_SUMMARY) {
			util.writeLog("Crawl Completed in " + timer.toString());
			util.writeLog(requestedPages.size() + " pages retreived");
		}

		// display the current cache size
		if (PRINT_CACHE_SIZE && requestedPages.size() > 0)
			util.writeLog("Cache Size: "
					+ (utilities.util.folderSize(seedPage.CACHE_PATH) / 1024)
					/ 1024 + "MB");

		return requestedPages;
	}

	/**
	 * Creates a thread from the url on the front of the pool
	 * 
	 * @return the created thread
	 */
	private Thread getNextUrlFromPool() {
		Page p = getRandomURL(); // take the new link off the pool
		Thread t = new Thread(p); // create the thread
		running.add(t); // add the thread to the list of running threads
		runningRef.put(t.getName(), p); // create the reference in the lookup
										// table
		return t; // return the thread to be started
	}

	public String getSeed() {
		return seed;
	}
}
