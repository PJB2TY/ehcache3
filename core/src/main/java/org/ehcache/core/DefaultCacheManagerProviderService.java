/*
 * Copyright Terracotta, Inc.
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

import org.ehcache.core.spi.cache.InternalCacheManager;
import org.ehcache.core.spi.service.CacheManagerProviderService;
import org.ehcache.spi.ServiceProvider;

/**
 * @author Mathieu Carbou
 */
public class DefaultCacheManagerProviderService implements CacheManagerProviderService {

  private final InternalCacheManager cacheManager;

  public DefaultCacheManagerProviderService(InternalCacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Override
  public InternalCacheManager getCacheManager() {
    return cacheManager;
  }

  @Override
  public void start(ServiceProvider serviceProvider) {

  }

  @Override
  public void stop() {

  }
}