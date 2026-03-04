package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

public class TerminalBuffer {

    // Setup
    private final int screenWidth;
    private final int screenHeight;
    private final int maxScrollback;

    // Attributes
    private static final TextAttributes DEFAULT_TEXT_ATTRIBUTES =
            new TextAttributes(TextFGColor.FG_DEFAULT, TextBGColor.BG_DEFAULT, Set.of());
    private TextAttributes currentTextAttributes;
    private Cursor cursor;

    /**
    * For now assume lines are fixed width
    * We split the viewable area into screen and scrollback, screen is simple array of lines,
    * scrollback is deque of lines for faster adding and removing top/bottom lines
    * */
    private final Line[] screenLines;
    private final Deque<Line> scrollback;
    private int contentEndIdx;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.maxScrollback = maxScrollback;
        this.screenLines = new Line[height];

        // Initialize cells with default attributes
        for (int i = 0; i < height; i++) {
            screenLines[i] = createEmptyLine();
        }

        scrollback = new ArrayDeque<>();

        this.cursor = new Cursor(0, 0);
        this.contentEndIdx = 0;
        this.currentTextAttributes = DEFAULT_TEXT_ATTRIBUTES;
    }

    public TextFGColor getForeground() {
        return currentTextAttributes.foreground();
    }

    public void setForeground(TextFGColor foreground) {
        this.currentTextAttributes = new TextAttributes(foreground, currentTextAttributes.background(), currentTextAttributes.styles());
    }

    public TextBGColor getBackground() {
        return currentTextAttributes.background();
    }

    public void setBackground(TextBGColor background) {
        this.currentTextAttributes = new TextAttributes(currentTextAttributes.foreground(), background, currentTextAttributes.styles());
    }

    public Set<TextStyle> getStyles() {
        return currentTextAttributes.styles();
    }

    public void setStyles(Set<TextStyle> styles) {
        if (styles == null) {
            return;
        }
        this.currentTextAttributes = new TextAttributes(
                currentTextAttributes.foreground(),
                currentTextAttributes.background(),
                styles
        );
    }

    public void setStyle(TextStyle style) {
        if (style == null) {
            return;
        }
        setStyles(Set.of(style));
    }

    public TextAttributes getAttributes() {
        return currentTextAttributes;
    }

    public void setAttributes(TextAttributes textAttributes) {
        if (textAttributes == null) {
            return;
        }
        this.currentTextAttributes = textAttributes;
    }

    public Cursor getCursorPos() {
        return cursor;
    }

    public void setCursorPos(int x, int y) {
        if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) {
            // Think if we want to throw an exception or move cursor to the nearest valid position
            IO.println("Invalid cursor position: (" + x + ", " + y + ")");
            return;
        }
        this.cursor = new Cursor(x, y);
    }

    public void moveCursor(int deltaX, int deltaY) {
        int rawX = cursor.x() + deltaX;
        int rawY = cursor.y() + deltaY;

        // Calculate new cursor position with wrapping
        int rowCarry = Math.floorDiv(rawX, screenWidth);
        int wrappedX = Math.floorMod(rawX, screenWidth);
        int wrappedY = Math.floorMod(rawY + rowCarry, screenHeight);
        //IO.println("Wrapping: (" + rawX + ", " + rawY + ") -> (" + wrappedX + ", " + wrappedY + ")");
        setCursorPos(wrappedX, wrappedY);
    }

    // Editing

    /**
     * Writes text on the current line, overriding the current content.
     * @param text The text to write
     */
    public void writeText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        ArrayList<String> parts = splitInputIfNeeded(text);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (!part.isEmpty()) {
                for (int j = 0; j < part.length(); j++) {
                    writeChar(part.charAt(j));
                }
            }

            if (shouldBreakAfterExplicitNewline(part, i, parts.size())) {
                breakLine();
            }
        }
    }

    /**
     * Inserts text on the current line, possibly wrapping the line. Characters after the inserted text are moved to the right.
     * @param text The text to insert
     */
    public void insertText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        ArrayList<String> parts = splitInputIfNeeded(text);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);

            if (!part.isEmpty()) {
                shiftTextRight(cursor.x(), cursor.y(), part.length());
                for (int j = 0; j < part.length(); j++) {
                    writeChar(part.charAt(j));
                }
            }

            if (shouldBreakAfterExplicitNewline(part, i, parts.size())) {
                breakLine();
            }
        }
    }


    public void fillLine(char character) {
        int startIdx = cursor.x();

        for (int i = startIdx; i < screenWidth; i++) {
            writeChar(character);
        }
    }

    /**
     * Inserts empty line at the bottom of the screen.
     */
    public void insertEmptyLine() {
        addLineToScrollback(screenLines[0]);
        for (int i = 0; i < screenHeight - 1; i++) {
            screenLines[i] = screenLines[i + 1];
        }
        screenLines[screenHeight - 1] = createEmptyLine();

        contentEndIdx = Math.max(0, contentEndIdx - screenWidth);
    }

    /**
     * Clears the entire screen, keep scrollback.
     */
    public void clearScreen() {
        for (int i = 0; i < screenHeight; i++) {
            for (int j = 0; j < screenWidth; j++) {
                clearCell(i, j);
            }
        }

        cursor = new Cursor(0, 0);
        contentEndIdx = 0;
    }

    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // Content Access
    public char getScreenCharAt(int x, int y) {
        if (x >= 0 && x < screenWidth && y >= 0 && y < screenHeight) {
            return screenLines[y].getCell(x).getCharacter();
        }

        return ' ';
    }

    public char getScrollbackCharAt(int x, int y) {
        int scrollbackSize = scrollback.size();
        if (x < 0 || x >= screenWidth || y < 0 || y >= scrollbackSize) {
            return ' ';
        }

        Line line = getScrollbackLineInternal(y);
        if (line == null) {
            return ' ';
        }

        Cell cell = line.getCell(x);
        if (cell == null) {
            return ' ';
        }
        return cell.getCharacter();
    }

    public TextAttributes getScreenAttributesAt(int x, int y) {
        if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) {
            return DEFAULT_TEXT_ATTRIBUTES;
        }

        Cell cell = screenLines[y].getCell(x);
        if (cell == null) {
            return DEFAULT_TEXT_ATTRIBUTES;
        }

        return cell.getAttributes();
    }

    public TextAttributes getScrollbackAttributesAt(int x, int y) {
        int scrollbackSize = scrollback.size();
        if (x < 0 || x >= screenWidth || y < 0 || y >= scrollbackSize) {
            return DEFAULT_TEXT_ATTRIBUTES;
        }
        Line line = getScrollbackLineInternal(y);
        if (line == null) {
            return DEFAULT_TEXT_ATTRIBUTES;
        }

        Cell cell = line.getCell(x);
        if (cell == null) {
            return DEFAULT_TEXT_ATTRIBUTES;
        }
        return cell.getAttributes();
    }

    /**
     * Returns a copy of the line at scrollback level y
     */
    public String getScrollbackLineAt(int y) {
        Line internal = getScrollbackLineInternal(y);
        StringBuilder result = new StringBuilder();
        if (internal != null) {
            for (int i = 0; i < internal.length(); i++) {
                Cell cell = internal.getCell(i);
                if (cell != null) {
                    result.append(cell.getCharacter());
                } else {
                    result.append(' ');
                }
            }
        }
        return result.toString();
    }

    /**
     * Returns the line at scrollback level y
     */
    private Line getScrollbackLineInternal(int y) {
        int size = scrollback.size();
        if (y < 0 || y >= size) {
            return null;
        }

        Iterator<Line> it = scrollback.iterator();
        for (int i = 0; i < y; i++) {
            it.next();
        }
        return it.next();
    }

    /**
     * Returns a copy of the screen line at level y
     */
    public String getScreenLineAt(int y) {
        if (y < 0 || y >= screenHeight) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < screenWidth; i++) {
            Cell cell = screenLines[y].getCell(i);
            if (cell != null) {
                result.append(cell.getCharacter());
            } else {
                result.append(' ');
            }
        }

        return result.toString();
    }

    public String getScreenContent() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < screenHeight; i++) {
            for (int j = 0; j < screenWidth; j++) {
                result.append(screenLines[i].getCell(j).getCharacter());
            }
            result.append('\n');
        }
        return result.toString();
    }

    public String getScreenAndScrollbackContent() {
        StringBuilder result = new StringBuilder();
        for (Line line : scrollback) {
            for (int j = 0; j < screenWidth; j++) {
                result.append(line.getCell(j).getCharacter());
            }
            result.append('\n');
        }

        for (int i = 0; i < screenHeight; i++) {
            for (int j = 0; j < screenWidth; j++) {
                result.append(screenLines[i].getCell(j).getCharacter());
            }
            result.append('\n');
        }

        return result.toString();
    }

    // Helpers

    private void addLineToScrollback(Line line) {
        scrollback.addLast(line);

        if (scrollback.size() > maxScrollback) {
            scrollback.removeFirst();
        }
    }

    private ArrayList<String> splitInputIfNeeded(String line) {
        ArrayList<String> parts = new ArrayList<>();
        int segmentStart = 0;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\n') {
                parts.add(line.substring(segmentStart, i));
                segmentStart = i + 1;
            }
        }

        // Keep the trailing segment so inputs ending with '\n' preserve the last line break.
        parts.add(line.substring(segmentStart));
        return parts;
    }

    /**
     * If a non-empty segment ended at column 0, it already wrapped to the next line.
     * In that case an explicit '\n' right after it must not add a second line break.
     */
    private boolean shouldBreakAfterExplicitNewline(String part, int partIdx, int partCount) {
        if (partIdx >= partCount - 1) {
            return false;
        }
        if (part.isEmpty()) {
            return true;
        }
        return cursor.x() != 0;
    }

    private void writeChar(char c) {
        Cell currCell = ensureCell(cursor.y(), cursor.x());
        currCell.setCharacter(c);
        applyCurrentAttributes(currCell);
        currCell.setEmpty(false);

        int cursorIdx = cursor.x() + cursor.y() * screenWidth;
        contentEndIdx = Math.max(contentEndIdx, cursorIdx + 1);

        if (cursor.x() == screenWidth - 1) {
            breakLine();
        } else {
            setCursorPos(cursor.x() + 1, cursor.y());
        }
    }

    // Adds a new line to the screen and scroll if needed
    private void breakLine() {
        if (cursor.y() == screenHeight - 1) {
            addLineToScrollback(screenLines[0]);
            for (int i = 0; i < screenHeight - 1; i++) {
                screenLines[i] = screenLines[i + 1];
            }
            screenLines[screenHeight - 1] = createEmptyLine();
        } else {
            setCursorPos(cursor.x(), cursor.y() + 1);
        }

        setCursorPos(0, cursor.y());
        int cursorIdx = cursor.x() + cursor.y() * screenWidth;
        contentEndIdx = Math.min(Math.max(contentEndIdx, cursorIdx), screenWidth * screenHeight);
    }

    private void shiftTextRight(int x, int y, int shiftAmount) {
        if (shiftAmount <= 0) {
            return;
        }

        int insertIdx = x + y * screenWidth;
        int maxCells = screenWidth * screenHeight;
        int endExclusive = Math.min(contentEndIdx, maxCells);

        if (insertIdx >= endExclusive) {
            return;
        }

        for (int sourceIdx = endExclusive - 1; sourceIdx >= insertIdx; sourceIdx--) {
            int targetIdx = sourceIdx + shiftAmount;
            if (targetIdx >= maxCells) {
                continue;
            }

            Cell source = getCellAtAbsoluteIndex(sourceIdx);
            Cell target = ensureCell(targetIdx / screenWidth, targetIdx % screenWidth);
            copyCell(source, target);
        }

        int clearEnd = Math.min(insertIdx + shiftAmount, maxCells);
        for (int idx = insertIdx; idx < clearEnd; idx++) {
            clearCell(idx / screenWidth, idx % screenWidth);
        }

        contentEndIdx = Math.min(endExclusive + shiftAmount, maxCells);
    }

    private Line createEmptyLine() {
        return new Line(screenWidth, DEFAULT_TEXT_ATTRIBUTES);
    }

    private Line copyLine(Line source) {
        Line copy = createEmptyLine();
        for (int i = 0; i < source.length(); i++) {
            Cell sourceCell = source.getCell(i);
            copy.setCell(i, copyCell(sourceCell));
        }
        return copy;
    }

    private Cell copyCell(Cell source) {
        return new Cell(source.getAttributes(), source.getCharacter(), source.isEmpty());
    }

    private Cell ensureCell(int lineIdx, int cellIdx) {
        if (screenLines[lineIdx] == null) {
            screenLines[lineIdx] = createEmptyLine();
        }
        if (screenLines[lineIdx].getCell(cellIdx) == null) {
            screenLines[lineIdx].setCell(cellIdx, new Cell(DEFAULT_TEXT_ATTRIBUTES, ' ', true));
        }
        return screenLines[lineIdx].getCell(cellIdx);
    }

    private Cell getCellAtAbsoluteIndex(int absoluteIdx) {
        int lineIdx = absoluteIdx / screenWidth;
        int cellIdx = absoluteIdx % screenWidth;
        return ensureCell(lineIdx, cellIdx);
    }

    private void clearCell(int lineIdx, int cellIdx) {
        Cell cell = ensureCell(lineIdx, cellIdx);
        cell.setAttributes(DEFAULT_TEXT_ATTRIBUTES);
        cell.setCharacter(' ');
        cell.setEmpty(true);
    }

    private void applyCurrentAttributes(Cell cell) {
        TextAttributes snapshot = new TextAttributes(
                currentTextAttributes.foreground(),
                currentTextAttributes.background(),
                currentTextAttributes.styles()
        );
        cell.setAttributes(snapshot);
    }

    private void copyCell(Cell source, Cell target) {
        target.setAttributes(source.getAttributes());
        target.setCharacter(source.getCharacter());
        target.setEmpty(source.isEmpty());
    }


    // Debug helper: print scrollback and visible screen lines.
    public void debugPrintScreenAndScrollback() {
        System.out.println("=== Scrollback (" + scrollback.size() + " lines) ===");
        int scrollbackIndex = 0;
        for (Line line : scrollback) {
            System.out.println("SB[" + scrollbackIndex + "]: " + lineToDebugString(line));
            scrollbackIndex++;
        }

        System.out.println("=== Screen (" + screenLines.length + " lines) ===");
        for (int i = 0; i < screenLines.length; i++) {
            System.out.println("SC[" + i + "]: " + lineToDebugString(screenLines[i]));
        }
    }

    private String lineToDebugString(Line line) {
        if (line == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            Cell cell = line.getCell(i);
            result.append(cell == null ? ' ' : cell.getCharacter());
        }
        return result.toString();
    }
}
