/**
 * 
 */
package crawler;

import java.net.MalformedURLException;
import java.util.Date;
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

	final int MAX_THREADS = 60;
	final int BURST_SIZE = 20;
	final boolean STAY_IN_DOMAIN = false;

	// Error Settings
	final boolean SHOW_THREAD_COMPLETED = false;
	final boolean SHOW_NEW_DOMAIN_FOUND = true;
	final boolean PRINT_CACHE_SIZE = true;
	final boolean PRINT_SUMMARY = true;

	final private String seed; // the seed url
	private Page seedPage;

	// pages that have yet to be requested
	HashMap<String, Vector<Page>> urlPool = new HashMap<String, Vector<Page>>();

	// pages that have been requested
	Vector <String> requestedPages = new Vector<String>();

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
		seedPage = addURL(seed, 0);
		util.writeLog("Seed Domain Set: " + seedPage.getDomain());
	}

	/**
	 * @param url
	 *            the url to be added
	 * @param depth
	 *            the depth of this page from the seed (set -1 if N/A)
	 * @return true if the URL was added
	 */
	public Page addURL(String url, int depth) {
		try {
			Page p = new Page(url, depth);
			if (!urlPool.containsKey(p.getUrl().getHost())){
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
			if (!urlPool.containsKey(p.getUrl().getHost()))
				urlPool.put(p.getUrl().getHost(), new Vector<Page>());
			urlPool.get(p.getUrl().getHost()).add(p);
		}
	}

	private Page getRandomURL() {
		Vector<Page> v;
		Page p;
		
		do {
			v = urlPool.get(urlPool.keySet().toArray()[rand.nextInt(urlPool.keySet().size())]);
		} while (v.size() < 0 && urlPool.size() > 0);

		p = v.remove(rand.nextInt(v.size()));
		if(v.size() == 0){
			urlPool.remove(p.getUrl().getHost());
		}
		
		return p;
	}

	/**
	 * 
	 * @param maxDepth
	 *            The max depth the spider will search
	 */
	public Vector <String> crawl(int maxDepth) {

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

						if (SHOW_THREAD_COMPLETED)
							util.writeLog("Thread "
									+ (requestedPages.size() + 1)
									+ " Completed: " + p.getUrl());

						// if we haven't reached the max depth yet process the
						// children
						if (p.getCrawlDepth() < maxDepth)
							addAllURLs(makePagesFromChildren(p));

						// move the page from the runningRef to the requested
						// pages
						runningRef.remove(p);
						//requestedPages.put(p.getPageID(), p);
						
						if(p.save())
							requestedPages.add(p.getUrl().toString());
						p = null;
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
	private Thread addNextUrlFromPool() {
		Page p = getRandomURL(); // take the new link off the pool
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

	// TODO: is this slowing us down
	private Vector<Page> makePagesFromChildren(Page p) {
		Vector<Page> children = new Vector<Page>();
		final int newDepth = p.getCrawlDepth() + 1;

		for (String s : p.getChildren()) {
			if (checkUrl(s)) {
				seenUrls.add(s);
				try {
					Page newPage = new Page(s, newDepth);
					children.add(newPage);
					newPage.addInLink(p);
					p.addOutLink(p);
					// / TODO: form ALL links from children, regardless of if
					// they were followed by the crawler, correct any depths.
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
