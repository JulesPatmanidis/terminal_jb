package org.example;

import java.util.Objects;

public class Line {

    private final Cell[] cells;

    public Line(int width, TextAttributes defaultAttributes) {
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            this.cells[i] = new Cell(defaultAttributes, ' ', true);
        }
    }

    public int length() {
        return cells.length;
    }

    public Cell getCell(int index) {
        return cells[index];
    }

    void setCell(int index, Cell cell) {
        cells[index] = Objects.requireNonNull(cell, "cell");
    }
}
