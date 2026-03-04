package org.example;

import java.util.Set;

public record TextAttributes(TextFGColor foreground, TextBGColor background, Set<TextStyle> styles) {
    public TextAttributes {
        styles = styles == null ? Set.of() : Set.copyOf(styles);
    }
}
