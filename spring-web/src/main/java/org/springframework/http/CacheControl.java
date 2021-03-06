/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.http;

import java.util.concurrent.TimeUnit;

import org.springframework.util.StringUtils;

/**
 * A builder for creating "Cache-Control" HTTP response headers.
 *
 * <p>Adding Cache-Control directives to HTTP responses can significantly improve the client experience when interacting
 * with a web application. This builder creates opinionated "Cache-Control" headers with response directives only, with
 * several use cases in mind.
 *
 * <ul>
 *     <li>Caching HTTP responses with {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS)}
 *     will result in {@code Cache-Control: "max-age=3600"}</li>
 *     <li>Preventing cache with {@code CacheControl cc = CacheControl.noStore()}
 *     will result in {@code Cache-Control: "no-store"}</li>
 *     <li>Advanced cases like {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().cachePublic()}
 *     will result in {@code Cache-Control: "max-age=3600, no-transform, public"}</li>
 * </ul>
 *
 * <p>Note that to be efficient, Cache-Control headers should be written along HTTP validators such as
 * "Last-Modifed" or "ETag" headers.
 *
 * @author Brian Clozel
 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2">rfc7234 section 5.2.2</a>
 * @see <a href="https://developers.google.com/web/fundamentals/performance/optimizing-content-efficiency/http-caching">
 *     HTTP caching - Google developers reference</a>
 * @see <a href="https://www.mnot.net/cache_docs/">Mark Nottingham's cache documentation</a>
 * @since 4.2
 */
public class CacheControl {

	private boolean mustRevalidate;

	private boolean noCache;

	private boolean noStore;

	private boolean noTransform;

	private boolean cachePublic;

	private boolean cachePrivate;

	private boolean proxyRevalidate;

	private long maxAge;

	private long sMaxAge;

	/**
	 * Create a CacheControl instance with default values,
	 * i.e. that will produce an empty "Cache-Control" header value.
	 */
	protected CacheControl() {
		this.mustRevalidate = false;
		this.noCache = false;
		this.noStore = false;
		this.noTransform = false;
		this.cachePublic = false;
		this.cachePrivate = false;
		this.proxyRevalidate = false;
		this.maxAge = -1;
		this.sMaxAge = -1;
	}

	/**
	 * Add a "max-age=" directive.
	 *
	 * <p>This directive is well suited for publicly caching resources, knowing that they won't change within
	 * the configured amount of time. Additional directives can be also used, in case resources shouldn't be
	 * cached ({@link #cachePrivate()}) or transformed ({@link #noTransform()}) by shared caches.
	 *
	 * <p>In order to prevent caches to reuse the cached response even when it has become stale
	 * (i.e. the "max-age" delay is passed), the "must-revalidate" directive should be set ({@link #mustRevalidate()}
	 *
	 * @param maxAge the maximum time the response should be cached
	 * @param unit the time unit of the {@code maxAge} argument
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
	 */
	public static CacheControl maxAge(long maxAge, TimeUnit unit) {
		CacheControl cc = new CacheControl();
		cc.maxAge = unit.toSeconds(maxAge);
		return cc;
	}

	/**
	 * Add a "no-store" directive
	 *
	 * <p>This directive is well suited for preventing caches (browsers and proxies) to cache the content of responses.
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.3">rfc7234 section 5.2.2.3</a>
	 */
	public static CacheControl noStore() {
		CacheControl cc = new CacheControl();
		cc.noStore = true;
		return cc;
	}

	/**
	 * Add a "no-cache" directive.
	 *
	 * <p>This directive is well suited for telling caches that the response can be reused only if the client
	 * revalidates it with the server. This directive won't disable cache altogether and may result with
	 * clients sending conditional requests (with "ETag", "If-Modified-Since" headers) and the server responding
	 * with "304 - Not Modified" status.
	 *
	 * <p>In order to disable caching and minimize requests/responses exchanges, the {@link #noStore()} directive
	 * should be used.
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">rfc7234 section 5.2.2.2</a>
	 */
	public static CacheControl noCache() {
		CacheControl cc = new CacheControl();
		cc.noCache = true;
		return cc;
	}

	/**
	 * Return an empty directive.
	 *
	 * <p>This is well suited for using other optional directives without "no-cache", "no-store" or "max-age".
	 *
	 * @return {@code this}, to facilitate method chaining
	 */
	public static CacheControl empty() {
		CacheControl cc = new CacheControl();
		return cc;
	}

	/**
	 * Add a "must-revalidate" directive
	 *
	 * <p>This directive indicates that once it has become stale, a cache MUST NOT use the response
	 * to satisfy subsequent requests without successful validation on the origin server.
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.1">rfc7234 section 5.2.2.1</a>
	 */
	public CacheControl mustRevalidate() {
		this.mustRevalidate = true;
		return this;
	}

	/**
	 * Add a "no-transform" directive
	 *
	 * <p>This directive indicates that intermediaries (caches and others) should not transform the response content.
	 * This can be useful to force caches and CDNs not to automatically gzip or optimize the response content.
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.4">rfc7234 section 5.2.2.4</a>
	 */
	public CacheControl noTransform() {
		this.noTransform = true;
		return this;
	}

	/**
	 * Add a "public" directive
	 *
	 * <p>This directive indicates that any cache MAY store the response, even if the response
	 * would normally be non-cacheable or cacheable only within a private cache.
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">rfc7234 section 5.2.2.5</a>
	 */
	public CacheControl cachePublic() {
		this.cachePublic = true;
		return this;
	}

	/**
	 * Add a "private" directive
	 *
	 * <p>This directive indicates that the response message is intended for a single user
	 * and MUST NOT be stored by a shared cache.
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.6">rfc7234 section 5.2.2.6</a>
	 */
	public CacheControl cachePrivate() {
		this.cachePrivate = true;
		return this;
	}

	/**
	 * Add a "proxy-revalidate" directive
	 *
	 * <p>This directive has the same meaning as the "must-revalidate" directive,
	 * except that it does not apply to private caches (i.e. browsers, HTTP clients)
	 *
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.7">rfc7234 section 5.2.2.7</a>
	 */
	public CacheControl proxyRevalidate() {
		this.proxyRevalidate = true;
		return this;
	}

	/**
	 * Add a "s-maxage" directive
	 *
	 * <p>This directive indicates that, in shared caches, the maximum age specified by this directive
	 * overrides the maximum age specified by other directives.
	 *
	 * @param sMaxAge the maximum time the response should be cached
	 * @param unit the time unit of the {@code sMaxAge} argument
	 * @return {@code this}, to facilitate method chaining
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
	 */
	public CacheControl sMaxAge(long sMaxAge, TimeUnit unit) {
		this.sMaxAge = unit.toSeconds(sMaxAge);
		return this;
	}

	/**
	 * Return the "Cache-Control" header value
	 *
	 * @return null if no directive was added, the header value otherwise
	 */
	public String getHeaderValue() {
		StringBuilder ccValue = new StringBuilder();
		if (this.maxAge != -1) {
			appendDirective(ccValue, "max-age=" + Long.toString(maxAge));
		}
		if (this.noCache) {
			appendDirective(ccValue, "no-cache");
		}
		if (this.noStore) {
			appendDirective(ccValue, "no-store");
		}
		if (this.mustRevalidate) {
			appendDirective(ccValue, "must-revalidate");
		}
		if (this.noTransform) {
			appendDirective(ccValue, "no-transform");
		}
		if (this.cachePublic) {
			appendDirective(ccValue, "public");
		}
		if (this.cachePrivate) {
			appendDirective(ccValue, "private");
		}
		if (this.proxyRevalidate) {
			appendDirective(ccValue, "proxy-revalidate");
		}
		if (this.sMaxAge != -1) {
			appendDirective(ccValue, "s-maxage=" + Long.toString(this.sMaxAge));
		}
		String ccHeaderValue = ccValue.toString();
		if (StringUtils.hasText(ccHeaderValue)) {
			return ccHeaderValue;
		}
		return null;
	}

	private void appendDirective(StringBuilder b, String value) {
		if (b.length() > 0) {
			b.append(", ");
		}
		b.append(value);
	}

}
