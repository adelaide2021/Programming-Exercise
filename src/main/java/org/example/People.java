package org.example;

import java.util.List;

public class People {
    private String name;
    private List<Training> completions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Training> getCompletions() {
        return completions;
    }

    public void setCompletions(List<Training> completions) {
        this.completions = completions;
    }
}
