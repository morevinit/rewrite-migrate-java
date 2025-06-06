/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpdateBeanManagerMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jakarta.enterprise.cdi-api-3.0.0-M4"))
          .recipe(new UpdateBeanManagerMethods());
    }

    @DocumentExample
    @Test
    void fireEvent() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
              import java.util.Set;

              class Foo {
                  void bar(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery) {
                      beanManager.fireEvent(beforeBeanDiscovery);
                  }
              }
              """,
            """
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
              import java.util.Set;

              class Foo {
                  void bar(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery) {
                      beanManager.getEvent().fire(beforeBeanDiscovery);
                  }
              }
              """
          )
        );
    }

    @Test
    void createInjectionTarget() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.enterprise.inject.spi.AnnotatedType;
              import jakarta.enterprise.inject.spi.BeanManager;

              class Foo {
                  void bar(BeanManager beanManager) {
                      AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
                      beanManager.createInjectionTarget(producerType);
                  }
              }
              """,
            """
              import jakarta.enterprise.inject.spi.AnnotatedType;
              import jakarta.enterprise.inject.spi.BeanManager;

              class Foo {
                  void bar(BeanManager beanManager) {
                      AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
                      beanManager.getInjectionTargetFactory(producerType).createInjectionTarget(null);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/597")
    @Test
    void fireEventWithClassifiers() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.Annotation;
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
              import java.util.Set;

              class Foo {
                  void one(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery, Annotation classifier) {
                      beanManager.fireEvent(beforeBeanDiscovery, classifier);
                  }
                  void two(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery, Annotation classifier) {
                      beanManager.fireEvent(beforeBeanDiscovery, classifier, classifier);
                  }
              }
              """,
            """
              import java.lang.annotation.Annotation;
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
              import java.util.Set;

              class Foo {
                  void one(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery, Annotation classifier) {
                      beanManager.getEvent().select(classifier).fire(beforeBeanDiscovery);
                  }
                  void two(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery, Annotation classifier) {
                      beanManager.getEvent().select(classifier, classifier).fire(beforeBeanDiscovery);
                  }
              }
              """
          )
        );
    }
}
