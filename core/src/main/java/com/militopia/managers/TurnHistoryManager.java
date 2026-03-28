package com.militopia.managers;

import com.militopia.data.TurnSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Manages undo/redo stacks of TurnSnapshots.
 * - push(): called on turn start — clears redo (new action invalidates forward history)
 * - pushRestore(): called after restoring a snapshot — preserves redo stack
 * - undoToIndex(): moves items to redo stack so they can be recovered
 * - redoToIndex(): moves items back to undo stack
 */
public class TurnHistoryManager {

    private static final int MAX_HISTORY = 50;

    private final Deque<TurnSnapshot> undoStack = new ArrayDeque<>();
    private final Deque<TurnSnapshot> redoStack = new ArrayDeque<>();

    /** Called on a real turn start. Clears redo (new action invalidates forward history). */
    public void push(TurnSnapshot snapshot) {
        undoStack.push(snapshot);
        redoStack.clear();
        trimToMax(undoStack);
    }

    /** Called after restoring a snapshot. Does NOT clear redo. */
    public void pushRestore(TurnSnapshot snapshot) {
        undoStack.push(snapshot);
        trimToMax(undoStack);
    }

    /** Pops the most recent undo snapshot. Returns null if empty. */
    public TurnSnapshot undo() {
        if (undoStack.isEmpty()) return null;
        return undoStack.pop();
    }

    /**
     * Pops to the snapshot at {@code index} (0 = most recent).
     * Skipped snapshots are pushed onto the redo stack so they can be recovered.
     */
    public TurnSnapshot undoToIndex(int index) {
        if (index < 0 || index >= undoStack.size()) return null;
        for (int i = 0; i < index; i++) {
            redoStack.push(undoStack.pop());
        }
        return undoStack.pop();
    }

    /** Pops the most recent redo snapshot. Returns null if empty. */
    public TurnSnapshot redo() {
        if (redoStack.isEmpty()) return null;
        return redoStack.pop();
    }

    /**
     * Pops to the redo snapshot at {@code index} (0 = next step forward).
     * Skipped redo snapshots are pushed back onto the undo stack.
     */
    public TurnSnapshot redoToIndex(int index) {
        if (index < 0 || index >= redoStack.size()) return null;
        for (int i = 0; i < index; i++) {
            undoStack.push(redoStack.pop());
        }
        return redoStack.pop();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public int size() { return undoStack.size(); }

    /** Returns undo snapshots, newest first. */
    public List<TurnSnapshot> getSnapshots() {
        return Collections.unmodifiableList(new ArrayList<>(undoStack));
    }

    /** Returns redo snapshots, next-step-first. */
    public List<TurnSnapshot> getRedoSnapshots() {
        return Collections.unmodifiableList(new ArrayList<>(redoStack));
    }

    /** Clears both stacks (e.g. on new game). */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private void trimToMax(Deque<TurnSnapshot> deque) {
        while (deque.size() > MAX_HISTORY) {
            deque.pollLast(); // Remove oldest entry in O(1)
        }
    }
}
