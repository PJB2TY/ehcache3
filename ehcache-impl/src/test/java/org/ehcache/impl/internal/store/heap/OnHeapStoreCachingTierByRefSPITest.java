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

package org.ehcache.impl.internal.store.heap;

import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.store.StoreConfigurationImpl;
import org.ehcache.core.events.NullStoreEventDispatcher;
import org.ehcache.impl.internal.sizeof.NoopSizeOfEngine;
import org.ehcache.impl.internal.store.heap.holders.SimpleOnHeapValueHolder;
import org.ehcache.core.spi.time.SystemTimeSource;
import org.ehcache.internal.tier.CachingTierFactory;
import org.ehcache.internal.tier.CachingTierSPITest;
import org.ehcache.spi.service.ServiceProvider;
import org.ehcache.core.spi.store.Store;
import org.ehcache.core.spi.store.tiering.CachingTier;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;
import org.junit.Before;

import static org.ehcache.config.builders.ResourcePoolsBuilder.newResourcePoolsBuilder;
import static org.ehcache.core.spi.ServiceLocator.dependencySet;

/**
 * This factory instantiates a CachingTier
 *
 * @author Aurelien Broszniowski
 */
public class OnHeapStoreCachingTierByRefSPITest extends CachingTierSPITest<String, String> {

  private CachingTierFactory<String, String> cachingTierFactory;

  @Override
  protected CachingTierFactory<String, String> getCachingTierFactory() {
    return cachingTierFactory;
  }

  @Before
  public void setUp() {
    cachingTierFactory = new CachingTierFactory<String, String>() {

      @Override
      public CachingTier<String, String> newCachingTier() {
        return newCachingTier(null);
      }

      @Override
      public CachingTier<String, String> newCachingTier(long capacity) {
        return newCachingTier((Long) capacity);
      }

      private CachingTier<String, String> newCachingTier(Long capacity) {
        Store.Configuration<String, String> config = new StoreConfigurationImpl<>(getKeyType(), getValueType(), null,
          ClassLoader.getSystemClassLoader(), ExpiryPolicyBuilder.noExpiration(), buildResourcePools(capacity), 0, null, null);

        return new OnHeapStore<>(config, SystemTimeSource.INSTANCE,
          new NoopSizeOfEngine(), NullStoreEventDispatcher.nullStoreEventDispatcher(), new DefaultStatisticsService());
      }

      @Override
      public Store.ValueHolder<String> newValueHolder(final String value) {
        return new SimpleOnHeapValueHolder<>(value, SystemTimeSource.INSTANCE.getTimeMillis(), false);
      }

      @Override
      public Store.Provider newProvider() {
        return new OnHeapStore.Provider();
      }

      private ResourcePools buildResourcePools(Comparable<Long> capacityConstraint) {
        if (capacityConstraint == null) {
          return newResourcePoolsBuilder().heap(Long.MAX_VALUE, EntryUnit.ENTRIES).build();
        } else {
          return newResourcePoolsBuilder().heap((Long)capacityConstraint, EntryUnit.ENTRIES).build();
        }
      }

      @Override
      public Class<String> getKeyType() {
        return String.class;
      }

      @Override
      public Class<String> getValueType() {
        return String.class;
      }

      @Override
      public ServiceConfiguration<?, ?>[] getServiceConfigurations() {
        return new ServiceConfiguration<?, ?>[0];
      }

      @Override
      public String createKey(long seed) {
        return "" + seed;
      }

      @Override
      public String createValue(long seed) {
        return "" + seed;
      }

      @Override
      public void disposeOf(CachingTier<String, String> tier) {
      }

      @Override
      public ServiceProvider<Service> getServiceProvider() {
        return dependencySet().build();
      }

    };
  }

}
