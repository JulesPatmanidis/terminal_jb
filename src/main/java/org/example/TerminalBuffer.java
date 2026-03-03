package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;

/**
 * Basic operations
 *
 * Implement a TerminalBuffer class (or equivalent) supporting the following operations:
 *
 * Setup
 * - Configurable initial width and height
 * - Configurable scrollback maximum size (number of lines)
 *
 * Attributes
 * - Set current attributes: foreground, background and styles. These attributes should be used for further edits.
 *
 * Cursor
 * - Get/set cursor position (column, row)
 * - Move cursor: up, down, left, right by N cells
 * - Cursor must not move outside screen bounds
 *
 * Editing
 *
 * Operations that should take the current cursor position and attributes into account:
 *
 * - Write a text on a line, overriding the current content. Moves the cursor.
 * - Insert a text on a line, possibly wrapping the line. Moves the cursor.
 * - Fill a line with a character (or empty)
 *
 * Operations that do not depend on cursor position or attributes:
 * - Insert an empty line at the bottom of the screen
 * - Clear the entire screen
 * - Clear the screen and scrollback
 *
 * Content Access
 * - Get character at position (from screen and scrollback)
 * - Get attributes at position (from screen and scrollback)
 * - Get line as string (from screen and scrollback)
 * - Get entire screen content as string
 * - Get entire screen+scrollback content as string
 */
public class TerminalBuffer {

    // Regular colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright colors
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Background colors
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";

    // Bright background colors
    public static final String BG_BRIGHT_BLACK = "\u001B[100m";
    public static final String BG_BRIGHT_RED = "\u001B[101m";
    public static final String BG_BRIGHT_GREEN = "\u001B[102m";
    public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
    public static final String BG_BRIGHT_BLUE = "\u001B[104m";
    public static final String BG_BRIGHT_MAGENTA = "\u001B[105m";
    public static final String BG_BRIGHT_CYAN = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE = "\u001B[107m";

    // Text styles
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String BLINK = "\u001B[5m";
    public static final String RAPID_BLINK = "\u001B[6m";
    public static final String REVERSE = "\u001B[7m";
    public static final String HIDDEN = "\u001B[8m";
    public static final String STRIKETHROUGH = "\u001B[9m";

    // Setup
    private int screenWidth;
    private int screenHeight;
    // TODO: add implementation for maxScrollback
    private int maxScrollback;

    // Attributes
    private String foreground;
    private String background;
    private String styles;

    // Cursor TODO: move to separate class
    public class CursorPos {
        private int x;
        private int y;

        public CursorPos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private CursorPos cursorPos;

    /*
    * For now assume lines are fixed width
    * We split the viewable area into screen and scrollback, screen is simple array of lines,
    * scrollback is deque of lines for faster adding and removing top/bottom lines
    *
    *
    * */
    private Line[] screenLines;
    private Deque<Line> scrollback;
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

        this.cursorPos = new CursorPos(0, 0);
        this.contentEndIdx = 0;
        this.foreground = BLACK;
        this.background = BG_BLACK;
        this.styles = "";
    }

    public String getForeground() {
        return foreground;
    }

    public void setForeground(String foreground) {
        this.foreground = foreground;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getStyles() {
        return styles;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }

    public CursorPos getCursorPos() {
        return cursorPos;
    }

    public void setCursorPos(int x, int y) {
        if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight) {
            // Think if we want to throw an exception or move cursor to the nearest valid position
            IO.println("Invalid cursor position: (" + x + ", " + y + ")");
            return;
        }
        this.cursorPos.x = x;
        this.cursorPos.y = y;
    }

    public void moveCursor(int deltaX, int deltaY) {
        int rawX = cursorPos.x + deltaX;
        int rawY = cursorPos.y + deltaY;

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

            if (i < parts.size() - 1) {
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
                shiftTextRight(cursorPos.x, cursorPos.y, part.length());
                for (int j = 0; j < part.length(); j++) {
                    writeChar(part.charAt(j));
                }
            }

            if (i < parts.size() - 1) {
                breakLine();
            }
        }
    }


    public void fillLine(char character) {
        int startIdx = cursorPos.x;
        int endIdx = screenWidth;

        for (int i = startIdx; i < endIdx; i++) {
            writeChar(character);
        }
    }

    /**
     * Inserts empty line at the bottom of the screen.
     * TODO: Decide if we want to move the cursor to the new line.
     */
    public void insertEmptyLine() {
        addLineToScrollback(screenLines[0]);
        for (int i = 0; i < screenHeight - 1; i++) {
            screenLines[i] = screenLines[i + 1];
        }
        screenLines[screenHeight - 1] = createEmptyLine();
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

        cursorPos.x = 0;
        cursorPos.y = 0;
        contentEndIdx = 0;
    }

    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // Content Access
    public char getScreenCharAt(int x, int y) {
        if (x >= 0 && x < screenWidth && y >= 0 && y < screenHeight) {
            return screenLines[y].cells[x].character;
        }

        // TODO: Think about what will be the default char to return
        return ' ';
    }

    public char getScrollbackCharAt(int x, int y) {
        int scrollbackSize = scrollback.size();
        if (x < 0 || x >= screenWidth || y < 0 || y >= scrollbackSize) {
            return ' ';
        }

        Line line = getScrollbackLineAt(y);
        if (line == null || line.cells == null || line.cells[x] == null) {
            return ' ';
        }

        return line.cells[x].character;
    }

    public void getScreenAttributesAt(int x, int y) {
        // TODO: implement
        // Probably need to move attributes to a separate class
    }

    public Line getScrollbackLineAt(int y) {
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

    public Line getScreenLineAt(int y) {
        if (y < 0 || y >= screenHeight) {
            return null;
        }

        return screenLines[y];
    }

    public String getScreenContent() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < screenHeight; i++) {
            for (int j = 0; j < screenWidth; j++) {
                result.append(screenLines[i].cells[j].character);
            }
            result.append('\n');
        }
        return result.toString();
    }

    public String getScreenAndScrollbackContent() {
        StringBuilder result = new StringBuilder();
        for (Line line : scrollback) {
            for (int j = 0; j < screenWidth; j++) {
                result.append(line.cells[j].character);
            }
            result.append('\n');
        }

        for (int i = 0; i < screenHeight; i++) {
            for (int j = 0; j < screenWidth; j++) {
                result.append(screenLines[i].cells[j].character);
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

    private void writeChar(char c) {
        Cell currCell = ensureCell(cursorPos.y, cursorPos.x);
        currCell.character = c;
        currCell.foreground = foreground;
        currCell.background = background;
        currCell.styles = styles;

        int cursorIdx = cursorPos.x + cursorPos.y * screenWidth;
        contentEndIdx = Math.max(contentEndIdx, cursorIdx + 1);

        if (cursorPos.x == screenWidth - 1) {
            breakLine();
        } else {
            cursorPos.x++;
        }
    }

    // Adds a new line to the screen and scroll if needed
    private void breakLine() {
        if (cursorPos.y == screenHeight - 1) {
            addLineToScrollback(screenLines[0]);
            for (int i = 0; i < screenHeight - 1; i++) {
                screenLines[i] = screenLines[i + 1];
            }
            screenLines[screenHeight - 1] = createEmptyLine();
        } else {
            cursorPos.y++;
        }

        cursorPos.x = 0;
        int cursorIdx = cursorPos.x + cursorPos.y * screenWidth;
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
        Line line = new Line(screenWidth);
        for (int i = 0; i < screenWidth; i++) {
            line.cells[i] = new Cell(BLACK, BG_BLACK, "", ' ');
        }
        return line;
    }

    private Cell ensureCell(int lineIdx, int cellIdx) {
        if (screenLines[lineIdx] == null) {
            screenLines[lineIdx] = createEmptyLine();
        }
        if (screenLines[lineIdx].cells[cellIdx] == null) {
            screenLines[lineIdx].cells[cellIdx] = new Cell(BLACK, BG_BLACK, "", ' ');
        }
        return screenLines[lineIdx].cells[cellIdx];
    }

    private Cell getCellAtAbsoluteIndex(int absoluteIdx) {
        int lineIdx = absoluteIdx / screenWidth;
        int cellIdx = absoluteIdx % screenWidth;
        return ensureCell(lineIdx, cellIdx);
    }

    private void clearCell(int lineIdx, int cellIdx) {
        Cell cell = ensureCell(lineIdx, cellIdx);
        cell.foreground = BLACK;
        cell.background = BG_BLACK;
        cell.styles = "";
        cell.character = ' ';
    }

    private void copyCell(Cell source, Cell target) {
        target.foreground = source.foreground;
        target.background = source.background;
        target.styles = source.styles;
        target.character = source.character;
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
        if (line == null || line.cells == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(line.cells.length);
        for (Cell cell : line.cells) {
            result.append(cell == null ? ' ' : cell.character);
        }
        return result.toString();
    }

}
