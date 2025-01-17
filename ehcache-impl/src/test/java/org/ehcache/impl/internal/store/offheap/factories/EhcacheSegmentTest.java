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

package org.ehcache.impl.internal.store.offheap.factories;

import org.ehcache.config.Eviction;
import org.ehcache.config.EvictionAdvisor;
import org.ehcache.impl.internal.store.offheap.SwitchableEvictionAdvisor;
import org.ehcache.impl.internal.store.offheap.HeuristicConfiguration;
import org.ehcache.impl.internal.store.offheap.portability.SerializerPortability;
import org.ehcache.impl.internal.spi.serialization.DefaultSerializationProvider;
import org.ehcache.spi.serialization.SerializationProvider;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.UnsupportedTypeException;
import org.junit.Test;
import org.terracotta.offheapstore.paging.PageSource;
import org.terracotta.offheapstore.paging.UpfrontAllocatingPageSource;
import org.terracotta.offheapstore.storage.OffHeapBufferStorageEngine;
import org.terracotta.offheapstore.storage.PointerSize;
import org.terracotta.offheapstore.storage.portability.Portability;
import org.terracotta.offheapstore.util.Factory;

import static org.ehcache.impl.internal.store.offheap.OffHeapStoreUtils.getBufferSource;
import static org.ehcache.impl.internal.spi.TestServiceProvider.providerContaining;
import static org.ehcache.test.MockitoUtil.uncheckedGenericMock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EhcacheSegmentTest {

  @SuppressWarnings("unchecked")
  private EhcacheSegmentFactory.EhcacheSegment<String, String> createTestSegment() {
    return createTestSegmentWithAdvisorAndListener(Eviction.noAdvice(), mock(EhcacheSegmentFactory.EhcacheSegment.EvictionListener.class));
  }

  @SuppressWarnings("unchecked")
  private EhcacheSegmentFactory.EhcacheSegment<String, String> createTestSegmentWithAdvisor(EvictionAdvisor<? super String, ? super String> evictionPredicate) {
    return createTestSegmentWithAdvisorAndListener(evictionPredicate, mock(EhcacheSegmentFactory.EhcacheSegment.EvictionListener.class));
  }

  private EhcacheSegmentFactory.EhcacheSegment<String, String> createTestSegmentWithListener(EhcacheSegmentFactory.EhcacheSegment.EvictionListener<String, String> evictionListener) {
    return createTestSegmentWithAdvisorAndListener(Eviction.noAdvice(), evictionListener);
  }

  private EhcacheSegmentFactory.EhcacheSegment<String, String> createTestSegmentWithAdvisorAndListener(final EvictionAdvisor<? super String, ? super String> evictionPredicate, EhcacheSegmentFactory.EhcacheSegment.EvictionListener<String, String> evictionListener) {
    try {
      HeuristicConfiguration configuration = new HeuristicConfiguration(1024 * 1024);
      SerializationProvider serializationProvider = new DefaultSerializationProvider(null);
      serializationProvider.start(providerContaining());
      PageSource pageSource = new UpfrontAllocatingPageSource(getBufferSource(), configuration.getMaximumSize(), configuration.getMaximumChunkSize(), configuration.getMinimumChunkSize());
      Serializer<String> keySerializer = serializationProvider.createKeySerializer(String.class, EhcacheSegmentTest.class.getClassLoader());
      Serializer<String> valueSerializer = serializationProvider.createValueSerializer(String.class, EhcacheSegmentTest.class.getClassLoader());
      Portability<String> keyPortability = new SerializerPortability<>(keySerializer);
      Portability<String> elementPortability = new SerializerPortability<>(valueSerializer);
      Factory<OffHeapBufferStorageEngine<String, String>> storageEngineFactory = OffHeapBufferStorageEngine.createFactory(PointerSize.INT, pageSource, configuration.getInitialSegmentTableSize(), keyPortability, elementPortability, false, true);
      SwitchableEvictionAdvisor<String, String> wrappedEvictionAdvisor = new SwitchableEvictionAdvisor<String, String>() {

        private volatile boolean enabled = true;

        @Override
        public boolean adviseAgainstEviction(String key, String value) {
          return evictionPredicate.adviseAgainstEviction(key, value);
        }

        @Override
        public boolean isSwitchedOn() {
          return enabled;
        }

        @Override
        public void setSwitchedOn(boolean switchedOn) {
          this.enabled = switchedOn;
        }
      };
      return new EhcacheSegmentFactory.EhcacheSegment<>(pageSource, storageEngineFactory.newInstance(), 1, wrappedEvictionAdvisor, evictionListener);
    } catch (UnsupportedTypeException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testPutAdvisedAgainstEvictionComputesMetadata() {
    EhcacheSegmentFactory.EhcacheSegment<String, String> segment = createTestSegmentWithAdvisor((key, value) -> {
      return "please-do-not-evict-me".equals(key);
    });
    try {
      segment.put("please-do-not-evict-me", "value");
      assertThat(segment.getMetadata("please-do-not-evict-me", EhcacheSegmentFactory.EhcacheSegment.ADVISED_AGAINST_EVICTION), is(EhcacheSegmentFactory.EhcacheSegment.ADVISED_AGAINST_EVICTION));
    } finally {
      segment.destroy();
    }
  }

  @Test
  public void testPutPinnedAdvisedAgainstComputesMetadata() {
    EhcacheSegmentFactory.EhcacheSegment<String, String> segment = createTestSegmentWithAdvisor((key, value) -> {
      return "please-do-not-evict-me".equals(key);
    });
    try {
      segment.putPinned("please-do-not-evict-me", "value");
      assertThat(segment.getMetadata("please-do-not-evict-me", EhcacheSegmentFactory.EhcacheSegment.ADVISED_AGAINST_EVICTION), is(EhcacheSegmentFactory.EhcacheSegment.ADVISED_AGAINST_EVICTION));
    } finally {
      segment.destroy();
    }
  }

  @Test
  public void testAdviceAgainstEvictionPreventsEviction() {
    EhcacheSegmentFactory.EhcacheSegment<String, String> segment = createTestSegment();
    try {
      assertThat(segment.evictable(1), is(true));
      assertThat(segment.evictable(EhcacheSegmentFactory.EhcacheSegment.ADVISED_AGAINST_EVICTION | 1), is(false));
    } finally {
      segment.destroy();
    }
  }

  @Test
  public void testEvictionFiresEvent() {
    EhcacheSegmentFactory.EhcacheSegment.EvictionListener<String, String> evictionListener = uncheckedGenericMock(EhcacheSegmentFactory.EhcacheSegment.EvictionListener.class);
    EhcacheSegmentFactory.EhcacheSegment<String, String> segment = createTestSegmentWithListener(evictionListener);
    try {
      segment.put("key", "value");
      segment.evict(segment.getEvictionIndex(), false);
      verify(evictionListener).onEviction("key", "value");
    } finally {
      segment.destroy();
    }
  }
}
