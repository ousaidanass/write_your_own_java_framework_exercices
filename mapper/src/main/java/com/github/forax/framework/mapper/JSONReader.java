package com.github.forax.framework.mapper;

import jdk.jshell.execution.Util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JSONReader {
    private record BeanData(Constructor<?> constructor, Map<String, PropertyDescriptor> propertyMap) {
        PropertyDescriptor findProperty(String key) {
            var property = propertyMap.get(key);
            if (property == null) {
                throw new IllegalStateException("unknown key " + key + " for bean " + constructor.getDeclaringClass().getName());
            }
            return property;
        }
    }

    private static final ClassValue<BeanData> BEAN_DATA_CLASS_VALUE = new ClassValue<>() {
        @Override
        protected BeanData computeValue(Class<?> type) {
            var beanInfo = Utils.beanInfo(type);
            var map = Arrays.stream(beanInfo.getPropertyDescriptors())
                    .filter(property -> !property.getName().equals("class"))
                    .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));
            var constructor = Utils.defaultConstructor(type);
            return new BeanData(constructor, map);
        }
    };

    public record ObjectBuilder<T>(Function<? super String, ? extends Type> typeProvider,
                                   Supplier<? extends T> supplier,
                                   Populater<? super T> populater,
                                   Function<? super T, ?> finisher) {
        public interface Populater<T> {
            void populate(T instance, String key, Object value);
        }

        public static ObjectBuilder<Object> bean(Class<?> beanClass) {
            var beanData = BEAN_DATA_CLASS_VALUE.get(beanClass);
            var constructor = beanData.constructor;
            return new ObjectBuilder<>(
                    key -> beanData.findProperty(key).getWriteMethod().getGenericParameterTypes()[0],
                    () -> Utils.newInstance(constructor),
                    (instance, key, value) -> {
                        var setter = beanData.findProperty(key).getWriteMethod();
                        Utils.invokeMethod(instance, setter, value);
                    },
                    Function.identity()
            );
        }
        public static ObjectBuilder<List<Object>> list(Type componentType) {
            Objects.requireNonNull(componentType);
            return new ObjectBuilder<>(
                    key -> componentType,
                    ArrayList::new,
                    (List<Object> instance, String key, Object value) -> instance.add(value),
                    List::copyOf
            );
        }

        public static ObjectBuilder<Object[]> record(Class<?> recordClass) {
            var components = recordClass.getRecordComponents();
            var map = IntStream.range(0, components.length)
                    .boxed()
                    .collect(Collectors.toMap(i -> components[i].getName(), Function.identity()));
            var constructor = Utils.canonicalConstructor(recordClass, components);
            return new ObjectBuilder<>(
                    key -> components[map.get(key)].getGenericType(),
                    () -> new Object[components.length],
                    (array, key, value) -> array[map.get(key)] = value,
                    array -> Utils.newInstance(constructor, array)
            );
        }
    }

    @FunctionalInterface
    public interface TypeMatcher {
        Optional<ObjectBuilder<?>> match(Type type);
    }

    public interface TypeRefrence<T> {

    }
    private final ArrayList<TypeMatcher> typeMatchers = new ArrayList<>();

    public void addTypeMatcher(TypeMatcher typeMatcher){
        Objects.requireNonNull(typeMatcher);
        typeMatchers.add(typeMatcher);
    }

    private ObjectBuilder<?> findObjectBuilder(Type type) {
        return typeMatchers.reversed().stream()
                .flatMap(typeMatcher -> typeMatcher.match(type).stream())
                .findFirst()
                .orElseGet(() -> ObjectBuilder.bean(Utils.erase(type)));
    }

  /*
  // Q3
  private record Context(BeanData beanData, Object result) {

  }*/

    /*// Q4
    private record Context(ObjectBuilder<Object> objectBuilder, Object result) {}

     */
    private record Context<T>(ObjectBuilder<T> objectBuilder, T result) {
        static <T> Context<T> createContext(ObjectBuilder<T> objectBuilder) {
            var instance = objectBuilder.supplier.get();
            return new Context<>(objectBuilder, instance);
        }

        void populate(String key, Object value) {
            objectBuilder.populater.populate(result, key, value);
        }

        Object finish() {
            return objectBuilder.finisher.apply(result);
        }
    }

    public <T> T parseJSON(String text, Class<T> beanClass) {
        return beanClass.cast(parseJSON(text, (Type) beanClass));
    }

  /* Q6
  public <T> T parseJSON(String text, TypeRefrence<T> typeRefrence) {
    return parseJSON(text, giveMeTheTypeOfTheTypeReference(typeRefrence));
  }
   */

    // Before: public <T> T parseJSON(String text, Class<T> beanClass)
    public Object parseJSON(String text, Type expectedType) {
        Objects.requireNonNull(text);
        //Objects.requireNonNull(beanClass);
        Objects.requireNonNull(expectedType);
        var stack = new ArrayDeque<Context<?>>();
        var visitor = new ToyJSONParser.JSONVisitor() {
            // Q1
            //private BeanData beanData;
            private Object result;

      /*
      // Q1
      @Override
      public void value(String key, Object value) {
        // call the corresponding setter on result
        var setter = beanData.findProperty(key).getWriteMethod();
        Utils.invokeMethod(result, setter, value);
      }
      */

      /*
      // Q3
      @Override
      public void value(String key, Object value) {
        // call the corresponding setter on result
        var currentContext = stack.peek();
        var setter = currentContext.beanData.findProperty(key).getWriteMethod();
        Utils.invokeMethod(currentContext.result, setter, value);
      }
       */

            @Override
            public void value(String key, Object value) {
                // call the corresponding setter on result
                var currentContext = stack.peek();
                //currentContext.objectBuilder.populater.populate(currentContext.result, key, value);
                currentContext.populate(key, value);
            }

      /*
      Q3
      @Override
      public void startObject(String key) {
        var currentContext = stack.peek();
        var beanType = currentContext == null ? beanClass: currentContext.beanData.findProperty(key).getPropertyType();
        var beanData = BEAN_DATA_CLASS_VALUE.get(beanType);
        var instance = Utils.newInstance(beanData.constructor);
        var newContext = new Context(beanData, instance);
        stack.push(newContext);
      }
       */

            @Override
            public void startObject(String key) {
                var currentContext = stack.peek();
                var type = currentContext == null ? // before beanClass
                        expectedType:
                        currentContext.objectBuilder.typeProvider.apply(key);
                //var objectBuilder = ObjectBuilder.bean(Utils.erase(beanType));
                var objectBuilder = findObjectBuilder(type);
                //var instance = objectBuilder.supplier.get();
                //var newContext = new Context(objectBuilder, instance);
                //stack.push(newContext);
                stack.push(Context.createContext(objectBuilder));
            }

      /*
      Q3
      @Override
      public void endObject(String key) {
        var previousContext = stack.pop();
        var result = previousContext.result;
        if (stack.isEmpty()) {
          this.result = result;
        } else {
          var currentContext = stack.peek();
          var setter = currentContext.beanData.findProperty(key).getWriteMethod();
          Utils.invokeMethod(currentContext.result, setter, result);
        }
      }
       */

            @Override
            public void endObject(String key) {
                var previousContext = stack.pop();
                //var result = previousContext.result;
                var result = previousContext.finish();
                if (stack.isEmpty()) {
                    this.result = result;
                } else {
                    //var currentContext = stack.peek();
                    //currentContext.objectBuilder.populater.populate(currentContext.result, key, result);
                    var currentContext = stack.peek();
                    currentContext.populate(key, result);
                }
            }

            @Override
            public void startArray(String key) {
                startObject(key);
            }


            @Override
            public void endArray(String key) {
                endObject(key);
            }
        };
        ToyJSONParser.parse(text, visitor);
        return visitor.result;
    }
}
