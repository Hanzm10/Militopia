package com.militopia.managers;

import com.militopia.data.TurnSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages a bounded stack of TurnSnapshots for undo functionality.
 * Push a snapshot at the start of every turn; call undo() to pop and restore.
 */
public class TurnHistoryManager {

    private static final int MAX_HISTORY = 10;

    private final Deque<TurnSnapshot> stack = new ArrayDeque<>();

    /** Save a snapshot at the start of a turn. Caps at MAX_HISTORY. */
    public void push(TurnSnapshot snapshot) {
        stack.push(snapshot);
        while (stack.size() > MAX_HISTORY) {
            // Remove the oldest (bottom) entry
            TurnSnapshot[] arr = stack.toArray(new TurnSnapshot[0]);
            stack.clear();
            for (int i = arr.length - 2; i >= 0; i--) {
                stack.push(arr[i]);
            }
        }
    }

    /**
     * Pops the most recent snapshot (the current turn's start state).
     * Returns null if there is nothing to undo.
     */
    public TurnSnapshot undo() {
        if (stack.isEmpty())
            return null;
        return stack.pop();
    }

    /** True if there is at least one snapshot to undo to. */
    public boolean canUndo() {
        return !stack.isEmpty();
    }

    /** Clears all history (e.g. on new game). */
    public void clear() {
        stack.clear();
    }
}
