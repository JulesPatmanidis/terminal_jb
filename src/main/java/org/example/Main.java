package org.example;

public class Main {
    static void main() {
        TerminalBuffer terminalBuffer = new TerminalBuffer(20, 5, 1000);

        terminalBuffer.clearScreen();
        terminalBuffer.insertText("HelloHelloHelloWoops");
        terminalBuffer.debugPrintScreenAndScrollback();
        terminalBuffer.moveCursor(-5, 0);
        terminalBuffer.clearScreen();
        terminalBuffer.insertText("Hello");
        terminalBuffer.writeText("World\nWorldWorld\nWorldWorld\nWorld");
        terminalBuffer.fillLine('a');
        terminalBuffer.writeText("OK\noh no");
        terminalBuffer.debugPrintScreenAndScrollback();
        char c = terminalBuffer.getScrollbackCharAt(3,0);
        System.out.println(terminalBuffer.getScreenAndScrollbackContent());

    }
}
