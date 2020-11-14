package com.fedotov.testtask;

import java.io.Serializable;

public class TagWithExpression implements Serializable {
    public String tagName;
    public String Expression;

    public TagWithExpression(String tagName, String expression) {
        this.tagName = tagName;
        Expression = expression;
    }
}
