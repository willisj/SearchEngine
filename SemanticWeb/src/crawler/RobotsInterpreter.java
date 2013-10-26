package crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RobotsInterpreter {

	Map<String, Domain> domains = new HashMap<String, Domain>();

	public RobotsInterpreter() {

	}

	// the crawler can make this call and sleep the thread for this many seconds
	// in order to obey robots.txt precisely
	public long checkTimeRemaining(String domain) {
		return domains.get(domain).checkTimeRemaining();
	}

	private void setLastAccessTime(String domain) {
		
		if (domains.containsKey(domain) && domains.get(domain) != null)
			domains.get(domain).setLastAccessTime();
	}

	private void addFile(String domain, String file) {
		if (file != null)
			domains.put(domain, new Domain(file));
	}

	public String getParsedFile(String domain) {
		return domains.get(domain).toString();
	}

	public boolean checkAllowed(String URLtoCheck) throws MalformedURLException {
		URL url = new URL(URLtoCheck);
		if (!domains.containsKey(url.getAuthority())) {
			Page p;
			try {
				p = new Page(url.toString() + "/robots.txt", "", 0);
				p.requestPage();
			} catch (MalformedURLException e) {
				domains.put(url.getAuthority(), null);
				return true;
			}

			addFile(url.getAuthority(), p.getRawSource());
		} else if (domains.get(url) == null)
			return true;
		setLastAccessTime(url.getAuthority());
		
		if(domains.get(url.getAuthority()) == null)
			return true;
		return domains.get(url.getAuthority()).checkAllowed(URLtoCheck);
	}
}
