package com.openplaces.heritrix;

import static org.archive.modules.fetcher.FetchStatusCodes.S_OUT_OF_SCOPE;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.archive.crawler.framework.Scoper;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;

/**
 * The crawl filter applies user-defined regular expressions on all URIs
 * matching a given seed. In other words, the user defines a HashMap with
 * keys being the seeds, and values as a regex string for which all URIs
 * coming from that seed must match to continue. Otherwise they'll be
 * marked as out-of-scope.
 *
 * The following is an example configuration snippet from crawler-beans.cxml:
 *
 * <pre>
 * {@code
 * <!-- CANDIDATE CHAIN -->
 * <bean id="heritrixCrawlFilter" class="com.openplaces.heritrix.HeritrixCrawlFilter">
 * <property name="seedFiltersMap">
 *   <map>
 *     <entry>
 *       <key><value>http://greglu.com/</value></key>
 *       <value>http://greglu.com/blog.*</value>
 *     </entry>
 *   </map>
 * </property>
 * </bean>
 * [...]
 * <!-- assembled into ordered CandidateChain bean -->
 * <bean id="candidateProcessors" class="org.archive.modules.CandidateChain">
 *  <property name="processors">
 *   <list>
 *    <!-- apply scoping rules to each individual candidate URI... -->
 *    <ref bean="candidateScoper"/>
 *    <!-- ...then prepare those ACCEPTed for enqueuing to frontier. -->
 *    <ref bean="preparer"/>
 *    <!-- here we reference the HeritrixCrawlFilter named bean -->
 *    <ref bean="heritrixCrawlFilter"/>
 *   </list>
 *  </property>
 * </bean>
 * }
 * </pre>
 *
 * In this example, we tell the crawler that for every URI that comes from the
 * "http://greglu.com/" seed, we apply the regular expression
 * "http://greglu.com/blog.*" on it and allow it to continue if it matches,
 * otherwise mark it "out-of-scope" and prevent further crawling from that page.
 *
 *
 * @author greg
 *
 */
public class HeritrixCrawlFilter extends Scoper {

	private static final long serialVersionUID = 1L;

	private Map<String, String> seedFiltersMap;

	public HeritrixCrawlFilter() {
	}

	public Map<String, String> getSeedFilters() {
		return seedFiltersMap;
	}

	public void setSeedFiltersMap(Map<String, String> seedFiltersMap) {
		this.seedFiltersMap = seedFiltersMap;
	}

	@Override
	protected void innerProcess(CrawlURI curi) throws InterruptedException {
	}

	@Override
	protected boolean shouldProcess(CrawlURI curi) {
		return !curi.isSeed();
	}

	@Override
	protected ProcessResult innerProcessResult(CrawlURI curi) throws InterruptedException {
		CrawlURI seed = curi.getFullVia();
		String seedUrl = "";

		// Set a limit on the number of times to recurse
		int count = 50;

		if (seed != null) {
			while (!seed.isSeed()) {
				if (count == 0)
					break;

				seed = seed.getFullVia();

				count--;
			}

			seedUrl = seed.toString();
		}

		String baseUri = curi.getBaseURI().toString();

		// Hard-coding the retrieval of robots.txt to always happen, and also check
		// that the base uri of the incoming link matches the seed uri
		if (!Pattern.matches(".*robots.txt.*", baseUri) && Pattern.matches(seedUrl + ".*", baseUri)
				&& getSeedFilters().containsKey(seedUrl)) {
			String filterString = getSeedFilters().get(seedUrl);

			// Now we apply the user-defined regex to the full uri
			if (!Pattern.matches(filterString, curi.toString())) {
				// If it doesn't match, mark it out-of-scope and finish the processing chain
				curi.setFetchStatus(S_OUT_OF_SCOPE);
				return ProcessResult.FINISH;
			}
		}

		// Otherwise we proceed
		return ProcessResult.PROCEED;
	}

}