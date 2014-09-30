package com.example.android_2048.model;

/**
 * Created by fuxie on 2014/9/30  11:15.
 */
public class Grid {
    private Tile[][] field;
    private Tile[][] undoField;
    private Tile[][] bufferField;

    public Grid(int sizeX, int sizeY) {
        field = new Tile[sizeX][sizeY];
        undoField = new Tile[sizeX][sizeY];
        bufferField = new Tile[sizeX][sizeY];

        clearGrid();
        clearUndoGrid();
    }

    private void clearGrid() {
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                field[i][j] = null;
            }
        }
    }
    private void clearUndoGrid() {
        for (int i = 0; i < undoField.length; i++) {
            for (int j = 0; j < undoField[i].length; j++) {
                undoField[i][j] = null;
            }
        }
    }
}

