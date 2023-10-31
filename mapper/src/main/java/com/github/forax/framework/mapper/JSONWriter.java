package com.github.forax.framework.mapper;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JSONWriter {
    @FunctionalInterface
    private interface Generator {
        String generate(JSONWriter writer, Object bean);
    }
/* Q1)_
  public String toJSON(Object o) {
    return switch (o) {
      case null -> "null";
      case Integer i -> i.toString();
      case Boolean b -> b.toString();
      case Double d -> d.toString();
      case String s -> '"' + s + '"';
      default -> throw new IllegalArgumentException("unknown object: " + o);
    };
  }
 */

    public String toJSON(Object o) {
        return switch (o) {
            case null -> "null";
            case Integer i -> i.toString();
            case Boolean b -> b.toString();
            case Double d -> d.toString();
            case String s -> '"' + s + '"';
            default -> toJSONBean(o);
        };
    }

/*
  // Question 2
  public String toJSONBean(Object o) {
    var beanInfo = Utils.beanInfo(o.getClass());
    return Arrays.stream(beanInfo.getPropertyDescriptors())
            .filter(property -> !property.getName().equals("class"))
            .map(property -> toJSON(property.getName()) + ": " + toJSON(Utils.invokeMethod(o, property.getReadMethod())))
            .collect(Collectors.joining(", ", "{", "}"));
  }
  */

  /*
  // Question 3
  private static final ClassValue<PropertyDescriptor[]> CACHE = new ClassValue<>() {
    @Override
    protected PropertyDescriptor[] computeValue(Class<?> type) {
      var beanInfo = Utils.beanInfo(type);
      return beanInfo.getPropertyDescriptors();
    }
  };

  public String toJSONBean(Object o) {
    return Arrays.stream(CACHE.get(o.getClass()))
            .filter(property -> !property.getName().equals("class"))
            .map(property -> toJSON(property.getName()) + ": " + toJSON(Utils.invokeMethod(o, property.getReadMethod())))
            .collect(Collectors.joining(", ", "{", "}"));
  }
  */
/*
    // Q4
    private static final ClassValue<List<Generator>> CACHE = new ClassValue<>() {
        @Override
        protected List<Generator> computeValue(Class<?> type) {
            var beanInfo = Utils.beanInfo(type);
            return Arrays.stream(beanInfo.getPropertyDescriptors())
                    .filter(property -> !property.getName().equals("class"))
                    .<Generator>map(property -> {
                        String name;
                        var propertyAnnotation = property.getReadMethod().getAnnotation(JSONProperty.class);
                        if (propertyAnnotation != null) {
                            name = propertyAnnotation.value();
                        } else {
                            name = property.getName();
                        }
                        Method readMethod = property.getReadMethod();
                        return (writer, o) -> writer.toJSON(name) + ": " + writer.toJSON(Utils.invokeMethod(o, readMethod));
                    })
                    .toList();
        }
    };

 */

    private static final ClassValue<List<Generator>> CACHE = new ClassValue<>() {
        @Override
        protected List<Generator> computeValue(Class<?> type) {
            var beanInfo = Utils.beanInfo(type);
            List<PropertyDescriptor> propertyDescriptors;
            if (type.isRecord()) {
                propertyDescriptors = recordProperties(type);
            } else {
                propertyDescriptors = beanProperties(type);
            }
            return Arrays.stream(beanInfo.getPropertyDescriptors())
                    .filter(property -> !property.getName().equals("class"))
                    .<Generator>map(property -> {
                        String name;
                        var propertyAnnotation = property.getReadMethod().getAnnotation(JSONProperty.class);
                        if (propertyAnnotation != null) {
                            name = propertyAnnotation.value();
                        } else {
                            name = property.getName();
                        }
                        Method readMethod = property.getReadMethod();
                        return (writer, o) -> writer.toJSON(name) + ": " + writer.toJSON(Utils.invokeMethod(o, readMethod));
                    })
                    .toList();
        }
    };

    public String toJSONBean(Object o) {
        return CACHE.get(o.getClass()).stream()
                .map(generator -> generator.generate(this, o))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static List<PropertyDescriptor> beanProperties(Class<?> type) {
        return List.of(Utils.beanInfo(type).getPropertyDescriptors());
    }

    private static List<PropertyDescriptor> recordProperties(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> {
                    try {
                        return new PropertyDescriptor(component.getName(), component.getAccessor(), null);
                    } catch (IntrospectionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    public static void main(String[] args) {
        Anass anass = new Anass();
        anass.setAge(17);
        anass.setName("YesBro");
        var jsonWriter = new JSONWriter();
        System.out.println(jsonWriter.toJSON(anass));
    }
}
