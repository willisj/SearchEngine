/**
 * 
 */
package crawler;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.lang3.time.StopWatch;
import utilities.util;

/**
 * @author Jordan
 * 
 */
public class Crawler extends Thread {

	final int MAX_THREADS = 10;
	final int BURST_SIZE = 5;
	final boolean STAY_IN_DOMAIN = true;

	final private String seed; // the seed url
	private Page seedPage;

	// pages that have yet to be requested
	Vector<Page> urlPool = new Vector<Page>();

	// pages that have been requested
	Vector<Page> requestedPages = new Vector<Page>();

	// pages currently threaded to request pages
	Vector<Thread> running = new Vector<Thread>();

	// lookup table Thread -> Page
	HashMap<String, Page> runningRef = new HashMap<String, Page>();

	// URLs that have already been scraped
	Vector<String> seenUrls = new Vector<String>();

	/**
	 * @param seed
	 *            the url from which to start scraping
	 * @throws MalformedURLException
	 *             invalid seed url
	 */
	Crawler(String seed) throws MalformedURLException {
		this.seed = seed;
		seedPage = new Page(seed, 0);
		urlPool.add(seedPage);
		System.out.println("Domain Set: " + seedPage.getDomain());
	}

	/**
	 * @param url
	 *            the url to be added
	 * @param depth
	 *            the depth of this page from the seed (set -1 if N/A)
	 * @return true if the URL was added
	 */
	public boolean addURL(String url, int depth) {
		try {
			urlPool.add(new Page(url, depth));
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param maxDepth
	 *            The max depth the spider will search
	 */
	public void crawl(int maxDepth) {

		StopWatch timer = new StopWatch();
		timer.start();
		// while we have work to do
		while (urlPool.size() > 0 || running.size() > 0) {

			// if there are some running threads to check up on
			if (running.size() > 0) {

				// check the top thread to see if it's done
				if (running.get(0).getState() == Thread.State.TERMINATED) {
					Thread t = running.remove(0);
					try {
						// join with the thread to remove it (by collecting it's
						// exit code)
						t.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						Page p = runningRef.get(t.getName());

						util.writeLog("Thread "+ (requestedPages.size() + 1) +" Completed:" + p.getUrl());


						// if we haven't reached the max depth yet process the
						// children
						if (p.getCrawlDepth() < maxDepth)
							urlPool.addAll(makePagesFromChildren(p));

						// move the page from the runningRef to the requested
						// pages
						requestedPages.add(runningRef.remove(p));
					}

				}

			}

			// if there are pages waiting to be requested
			// and we're under the MAX_THREADS limit
			if (urlPool.size() > 0 && running.size() <= MAX_THREADS) {

				// the container for this burst of pages
				Vector<Thread> burst = new Vector<Thread>();

				// while we still have room in this burst
				// and urls still exist
				while (burst.size() < BURST_SIZE && urlPool.size() > 0)
					burst.add(addNextUrlFromPool());

				for (Thread t : burst)
					t.start();

			} else { // wait for more urls
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		// calculate and display the running time
		timer.stop();
		util.writeLog("Crawl Completed in " + timer.toString());

		// display the current cache size
		if (requestedPages.size() > 0)
			util.writeLog("Cache Size: "
					+ (utilities.util.folderSize(seedPage.CACHE_PATH) / 1024)
					/ 1024 + "MB");
	}

	/**
	 * Creates a thread from the url on the front of the pool
	 * 
	 * @return the created thread
	 */
	private Thread addNextUrlFromPool() {
		Page p = urlPool.remove(0); // take the new link off the pool
		Thread t = new Thread(p); // create the thread
		running.add(t); // add the thread to the list of running threads
		runningRef.put(t.getName(), p); // create the reference in the lookup
										// table
		return t; // return the thread to be started
	}

	/**
	 * creates pages from the children of page p
	 * 
	 * @param p
	 *            the page to take the children from
	 * @return a vector of pages which represent the children of 'p'
	 */
	private Vector<Page> makePagesFromChildren(Page p) {
		Vector<Page> children = new Vector<Page>();
		final int newDepth = p.getCrawlDepth() + 1;

		for (String s : p.getChildren()) {
			if (checkUrl(s)) {
				seenUrls.add(s);
				try {
					children.add(new Page(s, newDepth));
				} catch (MalformedURLException e) {
				}
			}
		}

		return children;
	}

	// checks to see if a url is appropriate to be added to the queue
	private boolean checkUrl(String s) {
		// ALGEBRA: ![url has been seen] && ( Stay in domain -> s.startswith...)
		// p->q == !p || q
		return (!seenUrls.contains(s))
				&& (!STAY_IN_DOMAIN || s.startsWith(seedPage.getDomain()));
	}
}
