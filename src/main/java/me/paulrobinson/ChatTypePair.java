package me.paulrobinson;

import net.kyori.adventure.text.Component;

public record ChatTypePair(CircularBuffer<Component> damage, CircularBuffer<Component> regular) {
}
