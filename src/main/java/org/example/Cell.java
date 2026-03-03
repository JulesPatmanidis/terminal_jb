package org.example;

import java.util.Objects;

public class Cell {
    private TextAttributes attributes;
    private char character;

    public Cell(TextAttributes attributes, char character) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.character = character;
    }

    public TextAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(TextAttributes attributes) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    public char getCharacter() {
        return character;
    }

    public void setCharacter(char character) {
        this.character = character;
    }
}
