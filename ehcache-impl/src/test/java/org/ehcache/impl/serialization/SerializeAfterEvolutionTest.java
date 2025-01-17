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

package org.ehcache.impl.serialization;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;

import org.ehcache.core.spi.store.TransientStateRepository;
import org.ehcache.spi.serialization.StatefulSerializer;
import org.junit.Test;

import static org.ehcache.impl.serialization.SerializerTestUtilities.createClassNameRewritingLoader;
import static org.ehcache.impl.serialization.SerializerTestUtilities.newClassName;
import static org.ehcache.impl.serialization.SerializerTestUtilities.popTccl;
import static org.ehcache.impl.serialization.SerializerTestUtilities.pushTccl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SerializeAfterEvolutionTest {

  @Test
  public void test() throws Exception {
    @SuppressWarnings("unchecked")
    StatefulSerializer<Serializable> s = new CompactJavaSerializer<>(null);
    s.init(new TransientStateRepository());

    ClassLoader loaderA = createClassNameRewritingLoader(A_old.class);
    Serializable a = (Serializable) loaderA.loadClass(newClassName(A_old.class)).newInstance();
    ByteBuffer encodedA = s.serialize(a);

    ClassLoader loaderB = createClassNameRewritingLoader(A_new.class);
    pushTccl(loaderB);
    try {
      Serializable outA = s.read(encodedA);
      assertThat((Integer) outA.getClass().getField("integer").get(outA), is(42));

      Serializable b = (Serializable) loaderB.loadClass(newClassName(A_new.class)).newInstance();
      Serializable outB = s.read(s.serialize(b));
      assertThat((Integer) outB.getClass().getField("integer").get(outB), is(42));
    } finally {
      popTccl();
    }
  }

  public static class A_old implements Serializable {

    private static final long serialVersionUID = 1L;

    public Integer integer;

    public A_old() {
      integer = 42;
    }
  }

  public static class A_new implements Serializable {

    private static final long serialVersionUID = 1L;

    public Date date;
    public Integer integer;

    public A_new() {
      date = new Date(42L);
      integer = 42;
    }
  }
}
