package com.militopia.managers;

import com.militopia.data.TurnSnapshot;
import com.militopia.map.MapGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class TurnHistoryManagerTest {

    private TurnHistoryManager manager;

    @BeforeEach
    public void setup() {
        manager = new TurnHistoryManager();
    }

    private TurnSnapshot snap(int turn) {
        return new TurnSnapshot(0, 0, 0, 0, turn, 1, 1, 1,
                Collections.emptyList(), Collections.emptyList(),
                new MapGenerator.ObjectType[0][0]);
    }

    @Test
    public void push_clearsRedoStack() {
        // Arrange: push one, undo to populate redo via undoToIndex
        manager.push(snap(1));
        manager.push(snap(2));
        manager.undoToIndex(1); // skips snap(2) → onto redo
        assertTrue(manager.canRedo());

        // Act: push a new action
        manager.push(snap(3));

        // Assert: redo stack cleared by new push
        assertFalse(manager.canRedo());
    }

    @Test
    public void undo_returnsMostRecent() {
        // Arrange
        manager.push(snap(1));
        manager.push(snap(2));
        manager.push(snap(3));

        // Act
        TurnSnapshot result = manager.undo();

        // Assert: most recently pushed (turn=3) comes back first
        assertEquals(3, result.turn);
    }

    @Test
    public void undo_returnsNullWhenEmpty() {
        assertNull(manager.undo());
    }

    @Test
    public void push_respectsMaxHistory() {
        // Act: push one more than the cap
        for (int i = 0; i < 51; i++) {
            manager.push(snap(i));
        }

        // Assert: capped at 50
        assertEquals(50, manager.size());
    }

    @Test
    public void undoToIndex_movesSkippedToRedoStack() {
        // Arrange: push A(1), B(2), C(3) — stack top = C
        manager.push(snap(1));
        manager.push(snap(2));
        manager.push(snap(3));

        // Act: skip 2 items (C and B go to redo), pop A
        TurnSnapshot result = manager.undoToIndex(2);

        // Assert: returned A
        assertEquals(1, result.turn);
        // Assert: 2 items now on redo stack
        assertEquals(2, manager.getRedoSnapshots().size());
    }

    @Test
    public void redo_afterUndoToIndex_restoresForward() {
        // Arrange: A=1, B=2, C=3; undoToIndex(2) skips C then B → redo=[B,C]
        manager.push(snap(1));
        manager.push(snap(2));
        manager.push(snap(3));
        manager.undoToIndex(2);

        // Act: redo pops most-recently-skipped (B)
        TurnSnapshot result = manager.redo();

        // Assert
        assertEquals(2, result.turn);
    }

    @Test
    public void clear_emptiesBothStacks() {
        // Arrange: populate both stacks
        manager.push(snap(1));
        manager.push(snap(2));
        manager.push(snap(3));
        manager.undoToIndex(1); // snap(3) onto redo

        // Act
        manager.clear();

        // Assert
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());
        assertEquals(0, manager.size());
    }

    @Test
    public void canUndo_canRedo_reflectState() {
        // Empty state
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());

        // After push
        manager.push(snap(1));
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());

        // After undoToIndex moves item to redo
        manager.push(snap(2));
        manager.undoToIndex(1); // snap(2) → redo
        assertTrue(manager.canRedo());
    }
}
