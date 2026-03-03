package org.example;

public class Main {
    static void main() {
        TerminalBuffer terminalBuffer = new TerminalBuffer(20, 5, 1000);

        terminalBuffer.clearScreen();
        terminalBuffer.insertText("HelloHelloHelloWoops");
        terminalBuffer.debugPrintScreenAndScrollback();
        terminalBuffer.moveCursor(-5, 0);
        terminalBuffer.insertText("Hello");
        terminalBuffer.writeText("World\nWorld");
        terminalBuffer.debugPrintScreenAndScrollback();
    }
}
