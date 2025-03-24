package com.ses.whodatidols.model;

public class Content {
    private int id;
    private String url;

    // Getters, setters, constructors
    public Content() {}

    public Content(int id, String url) {
        this.id = id;
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}