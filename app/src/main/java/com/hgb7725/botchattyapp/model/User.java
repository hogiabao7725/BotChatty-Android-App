package com.hgb7725.botchattyapp.model;

import java.io.Serializable;

public class User implements Serializable {
//    private static final long serialVersionUID = 1L;

    private String name;
    private String image;
    private String email;
    private String token;

    public User() {};

    public User(String name, String image, String email, String token) {
        this.name = name;
        this.image = image;
        this.email = email;
        this.token = token;
    }

    // Getters
    public String getName() { return name; }
    public String getImage() { return image; }
    public String getEmail() { return email; }
    public String getToken() { return token; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setImage(String image) { this.image = image; }
    public void setEmail(String email) { this.email = email; }
    public void setToken(String token) { this.token = token; }
}

