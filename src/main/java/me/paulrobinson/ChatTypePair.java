package me.paulrobinson;

import net.kyori.adventure.text.Component;

public record ChatTypePair(BoundedQueue<Component> damage, BoundedQueue<Component> regular) {
}
