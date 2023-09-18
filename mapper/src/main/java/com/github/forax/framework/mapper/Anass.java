package com.github.forax.framework.mapper;

public class Anass {
    private String name;
    private int age;

    public Anass() {
    }

    @JSONProperty("NameFromJSONProperty")
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
