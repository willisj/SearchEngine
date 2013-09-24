/**
 * a Page represents a single URL which may or may not have been requested yet
 */
package crawler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.*;

import utilities.util;

/**
 * @author Jordan
 * 
 */
public class Page implements Runnable {

	private final boolean CACHE_REQUESTS = true;
	public final String CACHE_PATH = "cache/";

	/* PAGE-DATA */
	private URL url;
	private String rawSource; // The response from the web server to that URL
	private String linkLabel; // The lable on the link that lead to this result
								// (if applicable)

	// --- Post Request

	private Vector<String> children;
	private Vector<Page> inLinks;
	private Vector<Page> outLinks;

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
	Page(String url, int crawlDepth) throws MalformedURLException {
		children = new Vector<String>();
		
		inLinks = new Vector<Page>();
		outLinks = new Vector<Page>();
		
		setCrawlDepth(crawlDepth);
		try {
			this.setUrl(new URL(url));
		} catch (MalformedURLException e) { // bad url formatting
			throw e;

		}

		urlDepth = StringUtils.countMatches((CharSequence) this.url.getPath(),
				"/");
	}

	// used when threading
	public void run() {
		requestPage();
	}

	/**
	 * requests the page from the web
	 */
	/**
	 * @return
	 */
	boolean requestPage() {

		if (!CACHE_REQUESTS || !loadCache()) {

			BufferedReader in;
			String inputLine;
			StringBuffer response = new StringBuffer();

			try { // open the stream to the URL
				in = new BufferedReader(new InputStreamReader(url.openStream()));
			} catch (IOException e1) {
				util.writeLog("Failed to open stream to URL:"
						+ getUrl().toString(),true);
				return false;
			}

			try { // iterate over input stream until EOF
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine + "\n");
				}
				in.close();
			} catch (IOException e) {
				util.writeLog("Failed to read page source for URL:"
						+ getUrl().toString(),true);
				return false;
			}

			setRawSource(response.toString()); // set the page source
			storeCache();
		}

		findChildLinks();
		return true;
	}

	/**
	 * Finds all children links on the page
	 */
	public void findChildLinks() {

		// set properties for the HTML parser
		CleanerProperties prop = new CleanerProperties();
		prop.setTranslateSpecialEntities(true);
		prop.setTransResCharsToNCR(true);
		prop.setOmitComments(true);

		// create the parser
		HtmlCleaner hc = new HtmlCleaner(prop);

		// parse the page
		TagNode root = hc.clean(getRawSource());

		// find all <a> tags
		TagNode[] tags = root.getElementsByName("a", true);

		// traverse the list of a tags and grab all links
		for (int i = 0; tags != null && i < tags.length; i++) {
			String link = tags[i].getAttributeByName("href");
			if (link != null && link.length() > 0) {
				link = this.makeLinkAbsolute(link);
				if (link != "") // if validated link
					children.add(link); // add to this page's children
			}

		}
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
	public String md5(String s) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return diagString();
	}

	public void addOutLink(Page p){
		outLinks.add(p);
	}
	
	public void addInLink(Page p){
		inLinks.add(p);
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

	public Vector<String> getChildren() {
		return children;
	}

	public void setChildren(Vector<String> children) {
		this.children = children;
	}

}
