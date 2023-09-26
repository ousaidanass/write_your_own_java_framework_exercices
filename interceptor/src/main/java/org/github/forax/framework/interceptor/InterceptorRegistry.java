package org.github.forax.framework.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class InterceptorRegistry {
  // Q1)_
    private AroundAdvice advice;

    // Q2)_
    //private final HashMap<Class<? extends Annotation>, List<AroundAdvice>> annotationMap = new HashMap<>();

    // Q3)_
    private final HashMap<Class<? extends Annotation>, List<Interceptor>> interceptorMap = new HashMap<>();

    /*
    // Q1)_
  public void addAroundAdvice(Class<? extends Annotation> annotationClass, AroundAdvice aroundAdvice) {
    Objects.requireNonNull(annotationClass);
    Objects.requireNonNull(aroundAdvice);
    advice = aroundAdvice;
  }
  */
    /*
    // Q2)_
    public void addAroundAdvice(Class<? extends Annotation> annotationType, AroundAdvice aroundAdvice) {
        Objects.requireNonNull(annotationType);
        Objects.requireNonNull(aroundAdvice);
        annotationMap.computeIfAbsent(annotationType, __ -> new ArrayList<>()).add(aroundAdvice);
    }
     */

    public void addAroundAdvice(Class<? extends Annotation> annotationType, AroundAdvice aroundAdvice) {
        Objects.requireNonNull(annotationType);
        Objects.requireNonNull(aroundAdvice);
        addInterceptor(annotationType, (instance, method, args, invocation) -> {
            aroundAdvice.before(instance, method, args);
            Object result = null;
            try {
                return result = invocation.proceed(instance, method, args);
            } finally {
                aroundAdvice.after(instance, method, args, result);
            }
        });
    }

    /*
    // Q3)_
    public void addInterceptor(Class<? extends Annotation> annotationType, Interceptor interceptor) {
        Objects.requireNonNull(annotationType);
        Objects.requireNonNull(interceptor);
        interceptorMap.computeIfAbsent(annotationType, __ -> new ArrayList<>()).add(interceptor);
    }
     */

    // Q3)_
    public void addInterceptor(Class<? extends Annotation> annotationType, Interceptor interceptor) {
        Objects.requireNonNull(annotationType);
        Objects.requireNonNull(interceptor);
        interceptorMap.computeIfAbsent(annotationType, __ -> new ArrayList<>()).add(interceptor);
        cache.clear();
    }

    /*
    // Q2)_
    List<AroundAdvice> findAdvices(Method method) {
        Objects.requireNonNull(method);
        return Arrays.stream(method.getAnnotations())
                .flatMap(annotation -> annotationMap.getOrDefault(annotation.annotationType(), List.of()).stream())
                .toList();
    }
    */

    /*
    // Q3)_
    List<Interceptor> findInterceptors(Method method) {
        Objects.requireNonNull(method);
        return Arrays.stream(method.getAnnotations())
                .flatMap(annotation -> interceptorMap.getOrDefault(annotation.annotationType(), List.of()).stream())
                .toList();
    }
    */

    List<Interceptor> findInterceptors(Method method) {
        Objects.requireNonNull(method);
        return Stream.of(
                Arrays.stream(method.getDeclaringClass().getAnnotations()),
                Arrays.stream(method.getAnnotations()),
                Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream))
                .flatMap(s -> s)
                .flatMap(annotation -> interceptorMap.getOrDefault(annotation.annotationType(), List.of()).stream())
                .toList();
    }

    // Q4)_
    static Invocation getInvocation(List<Interceptor> interceptorList) {
        Invocation invocation = Utils::invokeMethod;
        for (var interceptor: interceptorList.reversed()) {
            var previousInvocation = invocation;
            invocation = ((instance, method, args) -> interceptor.intercept(instance, method, args, previousInvocation));
        }
        return invocation;
    }

    /*
  // Q1)_
  public <T> T createProxy(Class<T> interfaceType, T instance) {
      Objects.requireNonNull(interfaceType);
      Objects.requireNonNull(instance);
    return interfaceType.cast(Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[] {interfaceType},
            (Object __, Method method, Object[] args) -> {
            var advices = findAdvices(method);
            for (var advice: advices) {
                advice.before(instance, method, args);
            }
              Object result = null;
              try {
                result = Utils.invokeMethod(instance, method, args);
                return result;
              } finally {
                  for (var advice: advices.reversed()) {
                      advice.after(instance, method, args, result);
                  }
              }
            }));
  }
*/
    /*
    // Q4)_
    public <T> T createProxy(Class<T> interfaceType, T instance) {
        Objects.requireNonNull(interfaceType);
        Objects.requireNonNull(instance);
        return interfaceType.cast(Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[] {interfaceType},
                (Object __, Method method, Object[] args) -> {
                    var interceptors = findInterceptors(method);
                    var invocation = getInvocation(interceptors);
                    return invocation.proceed(instance, method, args);
                }));
    }
     */

    // Q6)_
    public <T> T createProxy(Class<T> interfaceType, T instance) {
        Objects.requireNonNull(interfaceType);
        Objects.requireNonNull(instance);
        return interfaceType.cast(Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[] {interfaceType},
                (Object __, Method method, Object[] args) -> {
                    var interceptors = findInterceptors(method);
                    var invocation = getInvocation(interceptors);
                    return invocation.proceed(instance, method, args);
                }));
    }

    // Q6)_
    private final HashMap<Method, Invocation> cache = new HashMap<>();

    // Q6)_
    private Invocation computeInvocation(Method method) {
        return cache.computeIfAbsent(method, m -> getInvocation(findInterceptors(m)));
    }
}
