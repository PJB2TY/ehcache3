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

package org.ehcache.xml.service;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.core.spi.service.ServiceUtils;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.xml.CoreServiceConfigurationParser;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

class SimpleCoreServiceConfigurationParser<TEMPLATE, CACHE, IN, OUT, U extends ServiceConfiguration<?, ?>> implements CoreServiceConfigurationParser<TEMPLATE, CACHE> {

  private final Function<TEMPLATE, IN> extractor;
  private final Parser<IN, U> parser;

  private final Class<U> configType;

  private final Function<CACHE, OUT> getter;
  private final BiConsumer<CACHE, OUT> setter;
  private final Function<U, OUT> unparser;
  private final BinaryOperator<OUT> merger;

  SimpleCoreServiceConfigurationParser(Class<U> configType,
                                       Function<TEMPLATE, IN> extractor, Function<IN, U> parser,
                                       Function<CACHE, OUT> getter, BiConsumer<CACHE, OUT> setter, Function<U, OUT> unparser) {
    this(configType, extractor, (config, loader) -> parser.apply(config), getter, setter, unparser, (a, b) -> { throw new IllegalStateException(); });
  }

  SimpleCoreServiceConfigurationParser(Class<U> configType,
                                       Function<TEMPLATE, IN> extractor, Function<IN, U> parser,
                                       Function<CACHE, OUT> getter, BiConsumer<CACHE, OUT> setter, Function<U, OUT> unparser, BinaryOperator<OUT> merger) {
    this(configType, extractor, (config, loader) -> parser.apply(config), getter, setter, unparser, merger);
  }

  SimpleCoreServiceConfigurationParser(Class<U> configType,
                                       Function<TEMPLATE, IN> extractor, Parser<IN, U> parser,
                                       Function<CACHE, OUT> getter, BiConsumer<CACHE, OUT> setter, Function<U, OUT> unparser) {
    this(configType, extractor, parser, getter, setter, unparser, (a, b) -> { throw new IllegalStateException(); });
  }

  SimpleCoreServiceConfigurationParser(Class<U> configType,
                                       Function<TEMPLATE, IN> extractor, Parser<IN, U> parser,
                                       Function<CACHE, OUT> getter, BiConsumer<CACHE, OUT> setter, Function<U, OUT> unparser, BinaryOperator<OUT> merger) {
    this.configType = configType;
    this.extractor = extractor;
    this.parser = parser;

    this.getter = getter;
    this.setter = setter;
    this.unparser = unparser;
    this.merger = merger;
  }

  @Override
  public final <K, V> CacheConfigurationBuilder<K, V> parseServiceConfiguration(TEMPLATE cacheDefinition, ClassLoader cacheClassLoader, CacheConfigurationBuilder<K, V> cacheBuilder) throws ClassNotFoundException {
    IN config = extractor.apply(cacheDefinition);
    if (config != null) {
      U configuration = parser.parse(config, cacheClassLoader);
      if (configuration != null) {
        return cacheBuilder.withService(configuration);
      }
    }
    return cacheBuilder;
  }

  @Override
  public CACHE unparseServiceConfiguration(CacheConfiguration<?, ?> cacheConfiguration, CACHE cacheType) {
    U serviceConfig = ServiceUtils.findSingletonAmongst(configType, cacheConfiguration.getServiceConfigurations());
    if (serviceConfig == null) {
      return cacheType;
    } else {

      OUT foo = getter.apply(cacheType);
      if (foo == null) {
        setter.accept(cacheType, unparser.apply(serviceConfig));
      } else {
        setter.accept(cacheType, merger.apply(foo, unparser.apply(serviceConfig)));
      }
      return cacheType;
    }
  }

  @FunctionalInterface
  interface Parser<T, U> {

    U parse(T t, ClassLoader classLoader) throws ClassNotFoundException;
  }
}
