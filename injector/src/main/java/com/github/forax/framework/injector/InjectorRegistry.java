package com.github.forax.framework.injector;

import jdk.jshell.execution.Util;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

public final class InjectorRegistry {
    //private final HashMap<Class<?>, Object> registry = new HashMap<>();
    private final HashMap<Class<?>, Supplier<?>> registry = new HashMap<>();

    /*
    Q1°
    public void registerInstance(Class<?> type, Object instance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(instance);
        var existing = registry.putIfAbsent(type, instance);
        if (existing != null) {
            throw new IllegalStateException("there is an already registred recipe for " + type.getName());
        }
    }

    public Object lookupInstance(Class<?> type) {
        var instance = registry.get(type);
        if (instance == null) {
            throw new IllegalArgumentException("there is no receipe for " + type.getName());
        }
        return instance;
    }
     */

    /*
    // Q2)_
    public <T> void registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(instance);
        var existing = registry.putIfAbsent(type, instance);
        if (existing != null) {
            throw new IllegalStateException("there is an already registred recipe for " + type.getName());
        }
    }
     */

    //Q3
    public <T> void registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(instance);
        registerProvider(type, () -> instance);
    }



    /*
    Q2)
    public <T> T lookupInstance(Class<T> type) {
        var instance = registry.get(type);
        if (instance == null) {
            throw new IllegalStateException("there is no receipe for " + type.getName());
        }
        return type.cast(instance);
    }
     */


    //Q3
    public <T> T lookupInstance(Class<T> type) {
        var supplier = registry.get(type);
        if (supplier == null) {
            throw new IllegalStateException("there is no receipe for " + type.getName());
        }
        return type.cast(supplier.get());
    }



    public <T> void registerProvider(Class<T> type, Supplier<T> supplier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(supplier);
        var existing = registry.putIfAbsent(type, supplier);
        if (existing != null) {
            throw new IllegalStateException("there is an already registred recipe for " + type.getName());
        }
    }

    static List<PropertyDescriptor> findInjectableProperties(Class<?> type) {
        Objects.requireNonNull(type);
        var beanInfo = Utils.beanInfo(type);
        return Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(property -> {
                    var setter = property.getWriteMethod();
                    return setter != null && setter.isAnnotationPresent(Inject.class);
                })
                .toList();
    }

    /*
    Q5)
    public <T> void registerProviderClass(Class<T> type, Class<? extends T> providerClass) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(providerClass);
        var constructor = Utils.defaultConstructor(providerClass);
        var properties = findInjectableProperties(providerClass);
        registerProvider(type, () -> {
            var instance = Utils.newInstance(constructor);
            for (var property : properties) {
                Utils.invokeMethod(instance, property.getWriteMethod(), lookupInstance(property.getPropertyType()));
            }
            return instance;
        });
    }
    */

    private Optional<Constructor<?>> findInjectableConstructor(Class<?> providerClass) {
        var constructors = Arrays.stream(providerClass.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();
        return switch (constructors.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(constructors.get(0));
            default -> throw new IllegalStateException("too many injectable constructors" + providerClass.getName());
        };
    }

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> providerClass) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(providerClass);
        var constructor = findInjectableConstructor(providerClass).orElseGet(() -> Utils.defaultConstructor(providerClass));
        var properties = findInjectableProperties(providerClass);
        registerProvider(type, () -> {
            var arguments = Arrays.stream(constructor.getParameterTypes())
                    .map(this::lookupInstance).toArray();
            var instance = Utils.newInstance(constructor, arguments);
            for (var property : properties) {
                Utils.invokeMethod(instance, property.getWriteMethod(), lookupInstance(property.getPropertyType()));
            }
            return providerClass.cast(instance);
        });
    }

    public void registerProviderClass(Class<?> providerClass) {
        Objects.requireNonNull(providerClass);
        registerProviderClassImpl(providerClass);
    }

    private <T> void registerProviderClassImpl(Class<T> providerClass) {
        registerProviderClass(providerClass, providerClass);
    }
}