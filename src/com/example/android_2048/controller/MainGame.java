package com.example.android_2048.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.example.android_2048.model.AnimationGrid;
import com.example.android_2048.model.Cell;
import com.example.android_2048.model.Grid;
import com.example.android_2048.model.Tile;
import com.example.android_2048.view.GameView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xiefuheng on 14-11-23.
 */
public class MainGame {
    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public static final int FADE_GLOBAL_ANIMATION = 0;

    public static final long MOVE_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME;
    public static final long SPAWN_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME;
    public static final long NOTIFICATION_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME * 5;
    public static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final String HIGH_SCORE = "high score";

    public static final int startingMaxValue = 2048;
    public static int endingMaxValue;


    //Odd state = game is not active
    //Even state = game is active
    //Win state = active state + 1
    public static final int GAME_WIN = 1;
    public static final int GAME_LOST = -1;
    public static final int GAME_NORMAL = 0;
    public static final int GAME_NORMAL_WON = 1;
    public static final int GAME_ENDLESS = 2;
    public static final int GAME_ENDLESS_WON = 3;

    public Grid grid = null;
    public AnimationGrid aGrid;
    public final int numSquaresX = 4;
    public final int numSquaresY = 4;
    final int startTiles = 2;

    public int gameState = 0;
    public boolean canUndo;

    public long score = 0;
    public long highScore = 0;

    public long lastScore = 0;
    public int lastGameState = 0;

    private long bufferScore = 0;
    private int bufferGameState = 0;

    private Context mContext;

    private GameView mView;

    public MainGame(Context mContext, GameView mView) {
        this.mContext = mContext;
        this.mView = mView;
        endingMaxValue = (int) Math.pow(2, mView.numCellType - 1);
    }

    public void newGame() {
        if (grid == null) {
            grid = new Grid(numSquaresX, numSquaresY);
        } else {
            prepareUndoState();
            saveUndoState();
            grid.clearGrid();
        }

        aGrid = new AnimationGrid(numSquaresX, numSquaresY);
        highScore = getHighScore();
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }

        score = 0;
        gameState = GAME_NORMAL;
        addStartTiles();
        mView.refreshLastTime = true;
        mView.resyncTime();
        mView.invalidate();
    }

    private void addStartTiles() {
        for (int i=0; i<startTiles; i++) {
            this.addRandomTile();
        }
    }

    private void addRandomTile() {
        if (grid.isCellsAvailable()) {
            int value = Math.random() < 0.9 ? 2 : 4;
            Tile tile = new Tile(grid.randomAvailableCell(), value);
            spawnTile(tile);
        }
    }

    private void spawnTile(Tile tile) {
        grid.insertTile(tile);
        aGrid.startAniamtion(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);
    }

    private void recordHighScore() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(HIGH_SCORE, highScore);
        editor.commit();
    }

    private void saveUndoState() {
        grid.saveTiles();
        canUndo = true;
        lastScore = bufferScore;
        lastGameState = bufferGameState;
    }

    private void prepareUndoState() {
        grid.prepareSaveTiles();
        bufferScore = score;
        bufferGameState = gameState;
    }

    public boolean gameWon() {
        return (gameState > 0 && gameState % 2 != 0);
    }

    public boolean gameLost() {
        return (gameState == GAME_LOST);
    }

    public boolean isActive() {
        return !(gameWon() || gameLost());
    }

    public boolean canContinue() {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON);
    }

    /**
     * move with given direction.
     *
     * @param direction 0: up, 1: right, 2: down, 3: left.
     */
    public void move(int direction) {
        aGrid.cancelAnimations();
        if (!isActive())
            return;

        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;

        prepareTiles();

        for (int i : traversalsX) {
            for (int j : traversalsY) {
                Cell cell = new Cell(i, j);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() &&
                            tile.getMergedFrom() == null) {
                        Tile merged = new Tile(positions[1], tile.getValue()*2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        tile.updatePosition(positions[1]);
                        int[] extras = {i, j};
                        aGrid.startAniamtion(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras);
                        aGrid.startAniamtion(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        score = score + merged.getValue();
                        highScore = Math.max(score, highScore);

                        if (merged.getValue() >= winValue() && !gameWon()) {
                            gameState = gameState + GAME_WIN;
                            endGame();
                        }
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {i, j, 0};
                        aGrid.startAniamtion(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras);
                    }

                    if (!positionsEqual(cell, tile)) {
                        moved = true;
                    }
                }
            }
        }

        if (moved) {
            saveUndoState();
            addRandomTile();
            checkLose();
        }

        mView.resyncTime();
        mView.invalidate();
    }

    private void checkLose() {
        if (!movesAvailable() && !gameWon()) {
            gameState = GAME_LOST;
            endGame();
        }
    }

    private boolean movesAvailable() {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;

        for (int i=0; i<numSquaresX; i++) {
            for (int j=0; j<numSquaresY; j++) {
                tile = grid.getCellContent(new Cell(i, j));

                if (tile != null) {
                    for (int direction =0; direction<4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(i + vector.getX(), j + vector.getY());
                        Tile other = grid.getCellContent(cell);

                        if (other != null && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean positionsEqual(Cell cell, Tile tile) {
        return cell.getX() == tile.getX() && cell.getY() == tile.getY();
    }

    private void moveTile(Tile tile, Cell cell) {
        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void endGame() {
        aGrid.startAniamtion(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME,null);
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
    }

    private int winValue() {
        if (!canContinue()) {
            return endingMaxValue;
        } else {
            return startingMaxValue;
        }
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(), previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));
        Cell[] answer = {previous, nextCell};
        return answer;
    }

    private void prepareTiles() {
        for (Tile[] tiles : grid.field) {
            for (Tile tile : tiles) {
                if (grid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<Integer>();
        for (int i=0; i<numSquaresX; i++) {
            traversals.add(i);
        }

        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }
        return traversals;
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<Integer>();
        for (int i=0; i<numSquaresX; i++) {
            traversals.add(i);
        }

        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }
        return traversals;
    }

    private Cell getVector(int direction) {
        Cell[] map = {
                new Cell(0, -1),     //up
                new Cell(1, 0),      //right
                new Cell(0, 1),      //down
                new Cell(-1, 0)      //left
        };
        return map[direction];
    }

    public void revertUndoState() {
        if (canUndo) {
            canUndo = false;
            aGrid.cancelAnimations();
            grid.revertTiles();
            score = lastScore;
            gameState = lastGameState;
            mView.refreshLastTime = true;
            mView.invalidate();
        }

    }

    public void setEndlessMode() {
        gameState = GAME_ENDLESS;
        mView.invalidate();
        mView.refreshLastTime = true;
    }

    public long getHighScore() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return preferences.getLong(HIGH_SCORE, -1);
    }
}
