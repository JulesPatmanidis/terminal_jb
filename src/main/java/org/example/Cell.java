package org.example;

import java.util.Objects;

public class Cell {
    private TextAttributes attributes;
    private char character;
    private boolean empty;

    public Cell(TextAttributes attributes, char character, boolean empty) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.character = character;
        this.empty = empty;
    }

    public TextAttributes getAttributes() {
        return attributes;
    }

    void setAttributes(TextAttributes attributes) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    public char getCharacter() {
        return character;
    }

    void setCharacter(char character) {
        this.character = character;
    }

    public boolean isEmpty() {
        return empty;
    }

    void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
