package org.example;

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

    private static class Cell {
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

    /*
    * Initial idea for cells, will be refined
    * Have static array holding all cells, width is screenWidth, height is screenHeight + maxScrollback
    * This implies that we can "scroll" the screen by changing start/end height pointers
    * When lines are added and we reach max scrollback, we need to shift every row up.
    *
    * */
    private Cell[][] cells;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.maxScrollback = maxScrollback;
        this.cells = new Cell[height + maxScrollback][width];

        // Initialize cells with default attributes
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                cells[i][j] = new Cell(BLACK, BG_BLACK, "", ' ');
            }
        }

        this.cursorPos = new CursorPos(0, 0);
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

    public void moveCursor(int x, int y) {
        setCursorPos(cursorPos.x + x, cursorPos.y + y);
    }

    // Editing
    public void writeText(String text) {
        // TODO: implement
    }

    public void insertText(String text) {
        // TODO: implement
    }

    public void fillLine(char character) {
        // TODO: implement
    }

    public void insertEmptyLine() {
        // TODO: implement
    }

    public void clearScreen() {
        // TODO: implement
    }

    public void clearScreenAndScrollback() {
        // TODO: implement
    }

    // Content Access
    public char getCharAt(int x, int y) {
        // TODO: implement
        return ' ';
    }

    public void getAttributesAt(int x, int y) {
        // TODO: implement
        // Probably need to move attributes to a separate class
    }

    public String getLineAt(int y) {
        // TODO: implement
        return "";
    }

    public String getScreenContent() {
        // TODO: implement
        return "";
    }

    public String getScreenAndScrollbackContent() {
        // TODO: implement
        return "";
    }

}
