package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TerminalBufferTest {

    @Test
    void moveCursorWrapsAcrossScreen() {
        TerminalBuffer buffer = new TerminalBuffer(5, 3, 10);

        buffer.moveCursor(-1, 0);
        assertEquals(4, buffer.getCursorPos().x());
        assertEquals(2, buffer.getCursorPos().y());

        buffer.moveCursor(2, 0);
        assertEquals(1, buffer.getCursorPos().x());
        assertEquals(0, buffer.getCursorPos().y());

        buffer.moveCursor(0, -1);
        assertEquals(1, buffer.getCursorPos().x());
        assertEquals(2, buffer.getCursorPos().y());
    }

    @Test
    void writeTextHandlesNewLine() {
        TerminalBuffer buffer = new TerminalBuffer(10, 3, 10);

        buffer.writeText("hello\nworld");

        assertScreenLineEquals(buffer, 0, "hello     ");
        assertScreenLineEquals(buffer, 1, "world     ");
        assertEquals(5, buffer.getCursorPos().x());
        assertEquals(1, buffer.getCursorPos().y());
    }

    @Test
    void writeTextNewlineAfterFullLineAddsOnlyOneLineBreak() {
        TerminalBuffer buffer = new TerminalBuffer(5, 4, 10);

        buffer.writeText("12345\nX");

        assertScreenLineEquals(buffer, 0, "12345");
        assertScreenLineEquals(buffer, 1, "X    ");
        assertScreenLineEquals(buffer, 2, "     ");
        assertEquals(1, buffer.getCursorPos().x());
        assertEquals(1, buffer.getCursorPos().y());
    }

    @Test
    void insertTextShiftsTailToTheRight() {
        TerminalBuffer buffer = new TerminalBuffer(8, 2, 10);
        buffer.writeText("abcdef");
        buffer.setCursorPos(2, 0);

        buffer.insertText("XY");

        assertScreenLineEquals(buffer, 0, "abXYcdef");
        assertEquals(4, buffer.getCursorPos().x());
        assertEquals(0, buffer.getCursorPos().y());
    }

    @Test
    void fillLineFillsFromCursorToEnd() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.setCursorPos(2, 0);

        buffer.fillLine('x');

        assertScreenLineEquals(buffer, 0, "  xxx");
        assertEquals(0, buffer.getCursorPos().x());
        assertEquals(1, buffer.getCursorPos().y());
    }

    @Test
    void clearScreenResetsContentAndCursor() {
        TerminalBuffer buffer = new TerminalBuffer(4, 2, 10);
        buffer.writeText("zzzz");
        buffer.moveCursor(1, 0);

        buffer.clearScreen();

        assertScreenLineEquals(buffer, 0, "    ");
        assertScreenLineEquals(buffer, 1, "    ");
        assertEquals(0, buffer.getCursorPos().x());
        assertEquals(0, buffer.getCursorPos().y());
    }

    @Test
    void clearScreenAndScrollbackAlsoClearsScrollback() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("ABCDE");
        buffer.insertEmptyLine();
        assertEquals('A', buffer.getScrollbackCharAt(0, 0));

        buffer.clearScreenAndScrollback();

        assertEquals(' ', buffer.getScrollbackCharAt(0, 0));
        assertEquals("     \n     \n", buffer.getScreenContent());
    }

    @Test
    void attributesAreStoredAndReturned() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        TextAttributes attrs = new TextAttributes(TerminalBuffer.RED, TerminalBuffer.BG_BLUE, TerminalBuffer.BOLD);
        buffer.setAttributes(attrs);

        assertEquals(attrs, buffer.getAttributes());
        buffer.writeText("Q");
        assertEquals(attrs, buffer.getScreenAttributesAt(0, 0));
        TextAttributes defaults = new TextAttributes(TerminalBuffer.WHITE, TerminalBuffer.BG_BLACK, "");
        assertEquals(defaults, buffer.getScreenAttributesAt(-1, 0));
    }

    @Test
    void getScrollbackCharAtIsSafeForOutOfBounds() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("ABCDE");
        buffer.insertEmptyLine();

        assertEquals('A', buffer.getScrollbackCharAt(0, 0));
        assertEquals('E', buffer.getScrollbackCharAt(4, 0));
        assertEquals(' ', buffer.getScrollbackCharAt(5, 0));
        assertEquals(' ', buffer.getScrollbackCharAt(0, 1));
    }

    @Test
    void getScrollbackAttributesAtReturnsStoredAndDefaultValues() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        TextAttributes attrs = new TextAttributes(TerminalBuffer.GREEN, TerminalBuffer.BG_WHITE, TerminalBuffer.UNDERLINE);
        buffer.setAttributes(attrs);
        buffer.writeText("A\nB\nC");

        TextAttributes defaults = new TextAttributes(TerminalBuffer.WHITE, TerminalBuffer.BG_BLACK, "");
        assertEquals(attrs, buffer.getScrollbackAttributesAt(0, 0));
        assertEquals(defaults, buffer.getScrollbackAttributesAt(1, 0));
        assertEquals(defaults, buffer.getScrollbackAttributesAt(0, 1));
    }

    @Test
    void getScreenAndScrollbackContentIncludesScrollbackFirst() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("AAAAA\nBBBBB\nCC");
        assertEquals("AAAAA\nBBBBB\nCC   \n", buffer.getScreenAndScrollbackContent());
    }

    @Test
    void getScreenLineAtReturnsCopy() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("A");

        Line line = buffer.getScreenLineAt(0);
        line.setCell(0, new Cell(new TextAttributes(TerminalBuffer.RED, TerminalBuffer.BG_BLACK, ""), 'Z', false));

        assertEquals('A', buffer.getScreenCharAt(0, 0));
    }

    @Test
    void getScrollbackLineAtReturnsCopy() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("AAAAA\nBBBBB\nCC");

        Line line = buffer.getScrollbackLineAt(0);
        line.setCell(0, new Cell(new TextAttributes(TerminalBuffer.RED, TerminalBuffer.BG_BLACK, ""), 'Z', false));

        assertEquals('A', buffer.getScrollbackCharAt(0, 0));
    }

    private static void assertScreenLineEquals(TerminalBuffer buffer, int y, String expected) {
        Line line = buffer.getScreenLineAt(y);
        assertNotNull(line);
        assertEquals(expected, lineToString(line));
    }

    private static String lineToString(Line line) {
        StringBuilder value = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            value.append(line.getCell(i).getCharacter());
        }
        return value.toString();
    }
}
