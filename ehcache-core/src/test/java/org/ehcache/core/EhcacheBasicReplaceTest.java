/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehcache.core;

import java.util.Collections;
import java.util.EnumSet;

import org.ehcache.Status;
import org.ehcache.core.statistics.CacheOperationOutcomes;
import org.ehcache.core.store.SimpleTestStore;
import org.ehcache.spi.resilience.StoreAccessException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Abhilash
 *
 */
public class EhcacheBasicReplaceTest extends EhcacheBasicCrudBase {

  @Test
  public void testReplaceNullNull() {
    final Ehcache<String, String> ehcache = this.getEhcache();

    try {
      ehcache.replace(null, null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testReplaceKeyNull() {
    final Ehcache<String, String> ehcache = this.getEhcache();

    try {
      ehcache.replace("key", null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testReplaceNullValue() {
    final Ehcache<String, String> ehcache = this.getEhcache();

    try {
      ehcache.replace(null, "value");
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  /**
   * Tests the effect of a {@link Ehcache#replace(Object, Object)} for
   * <ul>
   *   <li>key not present in {@code Store}</li>
   * </ul>
   */
  @Test
  public void testReplaceNoStoreEntry() throws Exception {
    final SimpleTestStore fakeStore = new SimpleTestStore(Collections.<String, String>emptyMap());
    this.store = spy(fakeStore);

    final Ehcache<String, String> ehcache = this.getEhcache();

    assertNull(ehcache.replace("key", "value"));
    verify(this.store).replace(eq("key"), eq("value"));
    verifyNoInteractions(this.resilienceStrategy);
    assertThat(fakeStore.getEntryMap().containsKey("key"), is(false));
    validateStats(ehcache, EnumSet.of(CacheOperationOutcomes.ReplaceOutcome.MISS_NOT_PRESENT));
  }

  /**
   * Tests the effect of a {@link Ehcache#replace(Object, Object)} for
   * <ul>
   *   <li>key not present in {@code Store}</li>
   *   <li>{@code Store.replace} throws</li>
   * </ul>
   */
  @Test
  public void testReplaceNoStoreEntryStoreAccessException() throws Exception {
    final SimpleTestStore fakeStore = new SimpleTestStore(Collections.<String, String>emptyMap());
    this.store = spy(fakeStore);
    doThrow(new StoreAccessException("")).when(this.store).replace(eq("key"), eq("value"));

    final Ehcache<String, String> ehcache = this.getEhcache();

    ehcache.replace("key", "value");
    verify(this.store).replace(eq("key"), eq("value"));
    verify(this.resilienceStrategy).replaceFailure(eq("key"), eq("value"), any(StoreAccessException.class));
    validateStats(ehcache, EnumSet.of(CacheOperationOutcomes.ReplaceOutcome.FAILURE));
  }

  /**
   * Tests the effect of a {@link Ehcache#replace(Object, Object)} for
   * <ul>
   *   <li>key present in {@code Store}</li>
   * </ul>
   */
  @Test
  public void testReplaceHasStoreEntry() throws Exception {
    final SimpleTestStore fakeStore = new SimpleTestStore(Collections.singletonMap("key", "oldValue"));
    this.store = spy(fakeStore);

    final Ehcache<String, String> ehcache = this.getEhcache();

    assertThat(ehcache.replace("key", "value"), is(equalTo("oldValue")));
    verify(this.store).replace(eq("key"), eq("value"));
    verifyNoInteractions(this.resilienceStrategy);
    assertThat(fakeStore.getEntryMap().get("key"), is(equalTo("value")));
    validateStats(ehcache, EnumSet.of(CacheOperationOutcomes.ReplaceOutcome.HIT));
  }

  /**
   * Tests the effect of a {@link Ehcache#replace(Object, Object)} for
   * <ul>
   *   <li>key present in {@code Store}</li>
   *   <li>{@code Store.replace} throws</li>
   * </ul>
   */
  @Test
  public void testReplaceHasStoreEntryStoreAccessException() throws Exception {
    final SimpleTestStore fakeStore = new SimpleTestStore(Collections.singletonMap("key", "oldValue"));
    this.store = spy(fakeStore);
    doThrow(new StoreAccessException("")).when(this.store).replace(eq("key"), eq("value"));

    final Ehcache<String, String> ehcache = this.getEhcache();

    ehcache.replace("key", "value");
    verify(this.store).replace(eq("key"), eq("value"));
    verify(this.resilienceStrategy).replaceFailure(eq("key"), eq("value"), any(StoreAccessException.class));
    validateStats(ehcache, EnumSet.of(CacheOperationOutcomes.ReplaceOutcome.FAILURE));
  }

  /**
   * Gets an initialized {@link Ehcache Ehcache} instance
   *
   * @return a new {@code Ehcache} instance
   */
  private Ehcache<String, String> getEhcache() {
    final Ehcache<String, String> ehcache
        = new Ehcache<>(CACHE_CONFIGURATION, this.store, resilienceStrategy, cacheEventDispatcher);
    ehcache.init();
    assertThat("cache not initialized", ehcache.getStatus(), is(Status.AVAILABLE));
    return ehcache;
  }
}
