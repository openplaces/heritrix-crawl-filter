package com.openplaces.heritrix;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.archive.modules.fetcher.FetchStatusCodes.S_OUT_OF_SCOPE;
import org.archive.crawler.framework.Scoper;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;

public class HeritrixCrawlFilter extends Scoper {

	private static final long serialVersionUID = 1L;

	private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

	private Map<String, String> seedFiltersMap;
	private Set<String> seedSet;

	public HeritrixCrawlFilter() {
	}

	public Map<String, String> getSeedFilters() {
		return seedFiltersMap;
	}

	public void setSeedFiltersMap(Map<String, String> seedFiltersMap) {
		LOGGER.info(seedFiltersMap.toString());

		this.seedFiltersMap = seedFiltersMap;
		this.seedSet = seedFiltersMap.keySet();
	}

	public Set<String> getSeedSet() {
		return seedSet;
	}

	@Override
	protected void innerProcess(CrawlURI curi) throws InterruptedException {
	}

	@Override
	protected boolean shouldProcess(CrawlURI curi) {
		return getSeedSet().contains(curi.getPathFromSeed());
	}

	@Override
    protected ProcessResult innerProcessResult(CrawlURI curi) throws InterruptedException {
		String seedPath = curi.getPathFromSeed();
		String filterString = getSeedFilters().get(seedPath);

		if (!Pattern.matches(filterString, curi.toString())) {
			curi.setFetchStatus(S_OUT_OF_SCOPE);
			return ProcessResult.FINISH;
		}

		return ProcessResult.PROCEED;
    }
	
}