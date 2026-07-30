package com.example.solstice.qol.chatheads;

import java.util.UUID;

/**
 * Mixin interface exposing a {@code @Unique} sender-UUID field bolted onto
 * {@code ChatHudLine} and {@code ChatHudLine.Visible} - lets Chat Heads dock
 * a head icon to the specific message/line it belongs to, instead of a
 * separate "recent senders" strip. Mirrors dzwdz/chat_heads' own {@code
 * HeadRenderable} mixin interface pattern (MPL-2.0, see NOTICE.md).
 *
 * <p>Deliberately lives outside {@code com.example.solstice.mixin.*} - that
 * whole package tree is declared as this project's Mixin package in
 * {@code solstice.client.mixins.json}, and Mixin throws a hard {@code
 * IllegalClassLoadError} at runtime ("is in a defined mixin package and
 * cannot be referenced directly") for any plain, non-{@code @Mixin} class
 * referenced from inside that tree - a real crash hit and fixed this
 * session, not a style preference. Any future mixin-interface helper like
 * this one must live in a normal package, never under {@code mixin.*}.</p>
 */
public interface ChatSenderCarrier {
    UUID solstice$getSenderUuid();

    void solstice$setSenderUuid(UUID uuid);
}
