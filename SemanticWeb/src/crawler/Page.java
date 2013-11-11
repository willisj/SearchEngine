/**
 * a Page represents a single URL which may or may not have been requested yet
 */
package crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import norbert.NoRobotClient;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.*;

import utilities.util;

/**
 * @author Jordan
 * 
 */
public class Page implements Runnable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final boolean CACHE_REQUESTS = true;
	private final boolean SKIP_ON_CACHE_MISS = false;
	private final boolean SHOW_NEW_CHILD_FOUND = false;
	private final boolean SHOW_404 = true;
	public final String CACHE_PATH = "cache/";
	public final static String STORE_PATH = "store/";
	public final static String FILE_EXT = "pgf";
	private static int nextID = 0;
	private final int pageID;
	
	// use for sql directly
	//public static TopologySQLController topology = new TopologySQLController("jdbc:mysql://localhost/topology?user=root&password=");
	
	// use to drop to file first
	public static TopologySQLController topology = new TopologySQLController(new File("sqlOutput"+System.currentTimeMillis()+".sql"));
			
	static NoRobotClient rob = new NoRobotClient("WALL-E");

	/* PAGE-DATA */

	private String title;
	private URL url;
	private String rawSource; // The response from the web server to that URL
	private String linkLabel; // The label on the link that lead to this result
								// (if applicable)

	// --- Post Request

	private HashMap<URL,String> children;

	/* META-DATA */
	private int crawlDepth; // The depth this page was found at if it was found
							// by a crawler
	private int urlDepth; // The number of path sections in the URL from the
							// base URL

	/**
	 * @param url
	 *            - the url of this page
	 * @param crawlDepth
	 *            - the depth from the seed this page was found (set to -1 for
	 *            N/A)
	 * @throws MalformedURLException
	 *             - invalid url
	 */
	public Page(URL url, String anchorText, int crawlDepth)
			throws MalformedURLException {
		pageID = nextID++;
		children = new HashMap<URL,String>();

		this.setLinkLabel(anchorText);
		setCrawlDepth(crawlDepth);

		this.setUrl(url);

		urlDepth = StringUtils.countMatches((CharSequence) this.url.getPath(),
				"/");
	}

	// used when threading
	public void run() {
		requestPage();
	}
	
	public static void startTopologyHandler(){
		Thread t = new Thread(topology);
		t.start();
	}

	/**
	 * requests the page from the web
	 */
	/**
	 * @return
	 */
	public boolean requestPage() {

		if (!CACHE_REQUESTS || !loadCache()) {
			if (SKIP_ON_CACHE_MISS)
				return false;

			BufferedReader in;
			String inputLine;
			StringBuffer response = new StringBuffer();

			try { // open the stream to the URL
				in = new BufferedReader(new InputStreamReader(url.openStream()));
			} catch (IOException e1) {
				if (SHOW_404)
					util.writeLog("Failed to open stream to URL:"
							+ getUrl().toString(), true);
				return false;
			}

			try { // iterate over input stream until EOF
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine + "\n");
				}
				in.close();
			} catch (IOException e) {

				util.writeLog("Failed to read page source for URL:"
						+ getUrl().toString(), true);
				return false;
			}
			
			setRawSource(response.toString()); // set the page source
			if (CACHE_REQUESTS)
				storeCache();
		}

		processPageSource();
		return true;
	}

	/**
	 * Finds all children links on the page
	 */
	public void processPageSource() {

		//TODO: send request to lucene here
		
		// set properties for the HTML parser
		CleanerProperties prop = new CleanerProperties();
		prop.setTranslateSpecialEntities(true);
		prop.setTransResCharsToNCR(true);
		prop.setOmitComments(true);

		// create the parser
		HtmlCleaner hc = new HtmlCleaner(prop);

		// parse the page
		TagNode root = hc.clean(getRawSource());

		// find the page title
		// TODO: make this faster, look at body/title directly 
		TagNode[] tags = root.getElementsByName("title",true);
		if(tags.length == 1){
			setTitle(tags[0].getText().toString());
		}
		
		// find all <a> tags
		tags = root.getElementsByName("a", true);

		// traverse the list of a tags and grab all links
		for (int i = 0; tags != null && i < tags.length; i++) {
			String link = tags[i].getAttributeByName("href");
			String anchorText = tags[i].getText().toString();
			if (link != null && link.length() > 0) {
				link = this.makeLinkAbsolute(link);
				URL childLink; 
				try {
					childLink = new URL(link);					
				} catch (MalformedURLException e) {
					continue;
				} 
				
				if (link != "" && (!url.getAuthority().equals(childLink.getAuthority()) || rob.isUrlAllowed(childLink))) // if validated link
						children.put(childLink,anchorText);
				
				// add to this page's children
			}

		}
		topology.createChildrenAndLink(getUrl(), getChildren().keySet());
	}

	/**
	 * @param the
	 *            relative url to make absolute
	 * @return the absolute url
	 * 
	 *         Note: Do we want to store emails?
	 */
	public String makeLinkAbsolute(String link) {

		// filter mailto links out
		if (link.startsWith("mailto:"))
			return "";

		// these are already absolute
		if (link.startsWith("http") || link.startsWith("https"))
			return link;

		// just missing the protocol
		if (link.startsWith("www.")) {
			return "http://" + link;
		}

		// this is a link that is relative to the baseurl
		if (link.startsWith("/")) {
			return getUrl().getProtocol() + "://" + getUrl().getAuthority()
					+ link;
		}

		// otherwise we assume the link is relative to the current path
		return getUrl().toString() + "/" + link;

	}

	static public Page load(String url) {
		try {
			return load(new File(getFullFilePath(url)));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static public Page load(File path) {

		Page page;
		try {

			FileInputStream fin = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fin);
			page = (Page) ois.readObject();
			ois.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return page;
	}

	// be sure to update the overloaded method to match
	public String getFilePath() {
		String path = url.getAuthority().toLowerCase();
		return path;
	}

	// -- don't separate

	// be sure to update the base method to match
	public static String getFullFilePath(String s) throws MalformedURLException {
		URL u = new URL(s);
		String path = STORE_PATH + u.getAuthority().toLowerCase() + "/"
				+ md5(u.toString()) + "." + FILE_EXT;
		return path;
	}

	public boolean save() {
		String path = getFilePath(); // no leading or trailing slash
		String filename = md5(url.toString()) + "." + FILE_EXT; // no slashes

		if (!new File(STORE_PATH + path).exists()) {
			if (!new File(STORE_PATH + path).mkdirs()) {
				util.writeLog("Unable to create directory for domain: " + path);
			}
		}

		try {
			FileOutputStream fout = new FileOutputStream(STORE_PATH + path
					+ "/" + filename);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this);
			oos.close();

		} catch (Exception ex) {
			util.writeLog("Error writing page to file: " + STORE_PATH + path
					+ "/" + filename);
			return false;
		}
		return true;
	}

	/**
	 * Store the page in the cache
	 * 
	 * @return true if the page was stored in the cache
	 */
	public boolean storeCache() {
		PrintWriter out;

		try {
			out = new PrintWriter(CACHE_PATH + md5(url.toString()));
			out.write(getRawSource());
		} catch (FileNotFoundException e) {
			return false;
		}

		out.close();
		return true;
	}

	/**
	 * Get the page from cache
	 * 
	 * @return true if successful cache hit, false otherwise
	 */
	public boolean loadCache() {

		try {
			setRawSource(utilities.util.readFile(
					CACHE_PATH + md5(url.toString()), Charset.defaultCharset()));
		} catch (IOException e) {
			return false; // file not found
		}
		return true;
	}

	/**
	 * @param s
	 *            string to be hashed
	 * @return the hash of the string
	 */
	private static String md5(String s) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");

			return utilities.util.bytesToHex(md.digest(s.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * @return the diagnostic string
	 */
	public String diagString() {
		String s = "URL: " + getUrl().toString() + "\nCrawl Depth: "
				+ getCrawlDepth() + "\nURL Depth: " + getUrlDepth()
				+ "\nSource Available: " + (getRawSource().length() > 0);
		return s;
	}

	public String getStrippedSource() {
		// set properties for the HTML parser
		CleanerProperties prop = new CleanerProperties();
		prop.setTranslateSpecialEntities(true);
		prop.setTransResCharsToNCR(true);
		prop.setOmitComments(true);

		// create the parser
		HtmlCleaner hc = new HtmlCleaner(prop);
		TagNode node = hc.clean(getRawSource());
		node = node.findElementByName("body", false);

		return node.getText().toString().replaceAll("(\\s{2,}|\\n)", " ");
	}
	
	
	/**
	 * creates pages from the children of page p
	 * 
	 * @param p
	 *            the page to take the children from
	 * @return a vector of pages which represent the children of 'p'
	 */

	// TODO: is this slowing us down
	public Vector<Page> makePagesFromChildren(Vector<String> seenUrls) {
		Vector<Page> children = new Vector<Page>();
		final int newDepth = getCrawlDepth() + 1;

		for (Entry<URL, String> entry : getChildren().entrySet()) {
			try {
				if (!seenUrls.contains(entry.getKey())) {
					seenUrls.add(entry.getKey().toString());
					if (SHOW_NEW_CHILD_FOUND)
						util.writeLog("+ " + entry.getKey().toString());
					Page newPage = new Page(entry.getKey(), entry.getValue(), newDepth);
					children.add(newPage);
					// TODO: SQL for topology here
					// / TODO: form ALL links from children, regardless of if
					// they were followed by the crawler, correct any depths.
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return children;
	}
	
	
	
	
	
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return diagString();
	}


	// * SETTERS AND GETTERS *//

	public String getDomain() {
		return getUrl().getProtocol() + "://" + getUrl().getAuthority();
	}

	/**
	 * @return the source of the page or null
	 */
	public String getRawSource() {
		return rawSource;
	}

	/**
	 * @param source
	 *            the source to set
	 */
	private void setRawSource(String source) {
		this.rawSource = source;
	}

	/**
	 * @return the crawlDepth
	 */
	public int getCrawlDepth() {
		return crawlDepth;
	}

	/**
	 * @param set
	 *            the crawlDepth
	 */
	private void setCrawlDepth(int crawlDepth) {
		this.crawlDepth = crawlDepth;
	}

	/**
	 * @return the urlDepth
	 */
	public int getUrlDepth() {
		return urlDepth;
	}

	/**
	 * @param urlDepth
	 *            the urlDepth to set
	 */
	private void setUrlDepth(int urlDepth) {
		this.urlDepth = urlDepth;
	}

	/**
	 * @return the url
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	private void setUrl(URL url) {
		this.url = url;
	}

	public HashMap<URL, String> getChildren() {
		return children;
	}

	public void setChildren(HashMap<URL,String> children) {
		this.children = children;
	}

	public int getPageID() {
		return pageID;
	}

	public String getLinkLabel() {
		return linkLabel;
	}

	public void setLinkLabel(String linkLabel) {
		this.linkLabel = linkLabel;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	

}
