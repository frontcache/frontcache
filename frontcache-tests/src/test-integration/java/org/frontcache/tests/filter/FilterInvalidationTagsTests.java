package org.frontcache.tests.filter;

import org.frontcache.tests.base.InvalidationTagsTests;

public class FilterInvalidationTagsTests extends InvalidationTagsTests {

	@Override
	public String getFrontCacheBaseURL() {
		return getFilterBaseURL();
	}

}
