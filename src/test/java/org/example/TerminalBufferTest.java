package org.example;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

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
    void moveCursorWrapsAcrossMultipleRowsAndColumns() {
        TerminalBuffer buffer = new TerminalBuffer(5, 3, 10);

        buffer.moveCursor(13, 7);

        assertEquals(3, buffer.getCursorPos().x());
        assertEquals(0, buffer.getCursorPos().y());
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
    void writeTextHandlesConsecutiveNewlines() {
        TerminalBuffer buffer = new TerminalBuffer(5, 4, 10);

        buffer.writeText("A\n\nB");

        assertScreenLineEquals(buffer, 0, "A    ");
        assertScreenLineEquals(buffer, 1, "     ");
        assertScreenLineEquals(buffer, 2, "B    ");
        assertEquals(1, buffer.getCursorPos().x());
        assertEquals(2, buffer.getCursorPos().y());
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
    void insertEmptyLineAlwaysScrollsScreenRegardlessOfContentEnd() {
        TerminalBuffer buffer = new TerminalBuffer(5, 3, 10);
        buffer.writeText("AB");
        buffer.setCursorPos(2, 0);

        buffer.insertEmptyLine();

        assertEquals('A', buffer.getScrollbackCharAt(0, 0));
        assertEquals('B', buffer.getScrollbackCharAt(1, 0));
        assertEquals("     \n     \n     \n", buffer.getScreenContent());
        assertEquals(2, buffer.getCursorPos().x());
        assertEquals(0, buffer.getCursorPos().y());
    }

    @Test
    void scrollbackRespectsConfiguredMaximumSize() {
        TerminalBuffer buffer = new TerminalBuffer(5, 1, 2);

        buffer.setCursorPos(0, 0);
        buffer.writeText("1111");
        buffer.insertEmptyLine();
        buffer.setCursorPos(0, 0);
        buffer.writeText("2222");
        buffer.insertEmptyLine();
        buffer.setCursorPos(0, 0);
        buffer.writeText("3333");
        buffer.insertEmptyLine();

        assertEquals("2222 ", buffer.getScrollbackLineAt(0));
        assertEquals("3333 ", buffer.getScrollbackLineAt(1));
        assertEquals("", buffer.getScrollbackLineAt(2));
    }

    @Test
    void attributesAreStoredAndReturned() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        TextAttributes attrs = new TextAttributes(TextFGColor.FG_RED, TextBGColor.BG_BLUE, Set.of(TextStyle.BOLD));
        buffer.setAttributes(attrs);

        assertEquals(attrs, buffer.getAttributes());
        buffer.writeText("Q");
        assertEquals(attrs, buffer.getScreenAttributesAt(0, 0));
        TextAttributes defaults = new TextAttributes(TextFGColor.FG_DEFAULT, TextBGColor.BG_DEFAULT, Set.of());
        assertEquals(defaults, buffer.getScreenAttributesAt(-1, 0));
    }

    @Test
    void writeTextAppliesForegroundBackgroundAndStyleToWrittenCell() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.setForeground(TextFGColor.FG_GREEN);
        buffer.setBackground(TextBGColor.BG_RED);
        buffer.setStyles(Set.of(TextStyle.UNDERLINE));

        buffer.writeText("X");

        TextAttributes expected = new TextAttributes(TextFGColor.FG_GREEN, TextBGColor.BG_RED, Set.of(TextStyle.UNDERLINE));
        assertEquals(expected, buffer.getScreenAttributesAt(0, 0));
    }

    @Test
    void setStylesUsesDefensiveCopyAndIgnoresNullInput() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        HashSet<TextStyle> mutableStyles = new HashSet<>();
        mutableStyles.add(TextStyle.BOLD);

        buffer.setStyles(mutableStyles);
        mutableStyles.add(TextStyle.UNDERLINE);

        assertEquals(Set.of(TextStyle.BOLD), buffer.getStyles());

        buffer.setStyles(null);
        assertEquals(Set.of(TextStyle.BOLD), buffer.getStyles());
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
        TextAttributes attrs = new TextAttributes(TextFGColor.FG_GREEN, TextBGColor.BG_WHITE, Set.of(TextStyle.UNDERLINE));
        buffer.setAttributes(attrs);
        buffer.writeText("A\nB\nC");

        TextAttributes defaults = new TextAttributes(TextFGColor.FG_DEFAULT, TextBGColor.BG_DEFAULT, Set.of());
        assertEquals(attrs, buffer.getScrollbackAttributesAt(0, 0));
        assertEquals(defaults, buffer.getScrollbackAttributesAt(1, 0));
        assertEquals(defaults, buffer.getScrollbackAttributesAt(0, 1));
    }

    @Test
    void getScrollbackLineAtReturnsExpectedContentAndHandlesBounds() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("AAAAA\nBBBBB\nCC");

        assertEquals("AAAAA", buffer.getScrollbackLineAt(0));
        assertEquals("", buffer.getScrollbackLineAt(1));
        assertEquals("", buffer.getScrollbackLineAt(-1));
    }

    @Test
    void getScreenAndScrollbackContentIncludesScrollbackFirst() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);
        buffer.writeText("AAAAA\nBBBBB\nCC");
        assertEquals("AAAAA\nBBBBB\nCC   \n", buffer.getScreenAndScrollbackContent());
    }

    @Test
    void getScreenLineAtReturnsNullForOutOfBounds() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2, 10);

        assertEquals(null, buffer.getScreenLineAt(-1));
        assertEquals(null, buffer.getScreenLineAt(2));
    }

    private static void assertScreenLineEquals(TerminalBuffer buffer, int y, String expected) {
        String line = buffer.getScreenLineAt(y);
        assertNotNull(line);
        assertEquals(expected, line);
    }

    private static String lineToString(Line line) {
        StringBuilder value = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            value.append(line.getCell(i).getCharacter());
        }
        return value.toString();
    }
}
