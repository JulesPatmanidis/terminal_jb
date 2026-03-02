package org.example;

public class Cell {
    public String foreground;
    public String background;
    public String styles;
    public char character;

    public Cell(String foreground, String background, String styles, char character) {
        this.foreground = foreground;
        this.background = background;
        this.styles = styles;
        this.character = character;
    }
}
