package org.example;

import java.util.Set;

public class Main {
    static void main() {
        TerminalBuffer buffer = new TerminalBuffer(12, 3, 2);

        // Plain write on first line.
        buffer.writeText("hello");

        // Styled write on next line.
        buffer.setForeground(TextFGColor.FG_GREEN);
        buffer.setBackground(TextBGColor.BG_DEFAULT);
        buffer.setStyles(Set.of(TextStyle.BOLD, TextStyle.UNDERLINE));
        buffer.writeText("\nstatus: ok");

        // Insert text in the middle of the styled line.
        buffer.setCursorPos(7, 1);
        buffer.insertText("!");

        // Force one scroll to demonstrate scrollback behavior.
        buffer.insertEmptyLine();
        buffer.setCursorPos(0, 2);
        buffer.setStyles(Set.of());
        buffer.setForeground(TextFGColor.FG_DEFAULT);
        buffer.writeText("bottom");

        System.out.println("Screen:");
        System.out.println(buffer.getScreenContent());

        System.out.println("Scrollback + Screen:");
        System.out.println(buffer.getScreenAndScrollbackContent());

        System.out.println("Cursor: " + buffer.getCursorPos());
    }
}
