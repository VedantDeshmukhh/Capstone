package com.resolvex.model;

/** A complaint category (Electrical, IT...) with keywords used to suggest it automatically. */
public class Category {

    private String name;
    private String keywords;   // comma separated, example: fan,light,switch

    public Category(String name, String keywords) {
        this.name = name;
        this.keywords = keywords;
    }

    public String getName() { return name; }
    public String getKeywords() { return keywords; }

    public String toLine() {
        return name + "|" + keywords;
    }

    public static Category fromLine(String line) {
        String[] p = line.split("\\|", -1);
        return new Category(p[0], p[1]);
    }
}
