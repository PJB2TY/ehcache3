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

package org.ehcache.jsr107;

import org.ehcache.impl.copy.ReadWriteCopier;

/**
 * Default copier for JSR caches to be used for immutable types
 * even for store by value cases.
 */
class Eh107IdentityCopier<T> extends ReadWriteCopier<T> {

  public Eh107IdentityCopier() {

  }

  @Override
  public T copy(T obj) {
    return obj;
  }
}
