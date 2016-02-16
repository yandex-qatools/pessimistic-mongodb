package ru.qatools.mongodb;

import java.io.Serializable;

public class User implements Serializable {
    public String firstName;
    public Address address;
    public String lastName;

    public static User sample() {
        final User user = new User();
        user.firstName = "Vasya";
        user.lastName = "Petrov";
        user.address = new Address();
        user.address.location = "Tokyo City";
        user.address.name = "Tokyo";
        return user;
    }

    public static class Address {
        public String location;
        public String name;
    }
}