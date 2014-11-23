package com.example.android_2048.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.example.android_2048.R;
import com.example.android_2048.controller.InputListener;
import com.example.android_2048.controller.MainGame;
import com.example.android_2048.model.AnimationCell;
import com.example.android_2048.model.Tile;

import java.util.ArrayList;

/**
 * Created by fuxie on 2014/9/30  14:14.
 */
public class GameView extends View {
    private Paint paint = new Paint();
    public MainGame game;
    public boolean hasSaveState = false;
    public final int numCellType = 18;
    public boolean continueButtonEnabled = false;

    private int cellSize = 0;
    private float textSize = 0;
    private float cellTextSize = 0;
    private int gridWidth = 0;
    public int startingX;
    public int startingY;
    public int endingX;
    public int endingY;
    private int textPaddingSize;
    private int iconPaddingSize;

    private Drawable backgroundRectangle;
    private BitmapDrawable[] bitmapCell = new BitmapDrawable[numCellType];

    private Drawable lightUpRectangle;
    private Drawable fadeRectangle;
    private Bitmap background = null;
    private BitmapDrawable loseGameOverlay;
    private BitmapDrawable winGameContinueOverlay;
    private BitmapDrawable winGameFinalOverlay;

    //Text variables
    private int sYAll;
    private int titleStartYAll;
    private int bodyStartYAll;
    private int eYAll;
    private int titleWidthHighScore;
    private int titleWidthScore;

    //Icon variables
    public int sYIcons;
    public int sXNewGame;
    public int sXUndo;
    public int iconSize;

    //Timing
    long lastFPSTime = System.nanoTime();
    long currentTime = System.nanoTime();

    //Text
    float titleTextSize;
    float bodyTextSize;
    float headerTextSize;
    float instructionsTextSize;
    float gameOverTextSize;

    //Misc
    public boolean refreshLastTime = true;

    //Intenal Constants
    public static final int BASE_ANIMATION_TIME = 100000000;
    static final float MERGING_ACCELERATION = (float) -0.5;
    static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;

    public GameView(Context context) {
        super(context);

        Resources resources = context.getResources();
        game = new MainGame(context, this);
        try {
            backgroundRectangle = resources.getDrawable(R.drawable.background_rectangle);
            lightUpRectangle = resources.getDrawable(R.drawable.light_up_rectangle);
            fadeRectangle = resources.getDrawable(R.drawable.fade_rectangle);
            this.setBackgroundColor(resources.getColor(R.color.background));
            Typeface typeface = Typeface.createFromAsset(resources.getAssets(), "ClearSans-Bold.ttf");
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
        } catch (Exception e) {
            System.out.println("Error getting assets");
        }
        setOnTouchListener(new InputListener(this));
        game.newGame();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(background, 0, 0, paint);

        drawScoreText(canvas);

        if (!game.isActive() && !game.aGrid.isAnimationActive()) {
            drawNewGameButton(canvas, true);
        }

        drawCells(canvas);

        if (!game.isActive()) {
            drawEndGameState(canvas);
        }

        if (!game.canContinue()) {
            drawEndlessText(canvas);
        }

        //Refresh the screen if there is still an animation running
        if (game.aGrid.isAnimationActive()) {
            invalidate(startingX, startingY, endingX, endingY);
            tick();

        } else if (!game.isActive() && refreshLastTime) {   //Refresh one last time on game end.
            invalidate();
            refreshLastTime = false;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        getLayout(w, h);
        createBitmapCells();
        createBackgroundBitmap(w, h);
        createOverlays();
    }

    private void createOverlays() {
        Resources resources = getResources();
        //Initalize overlays
        Bitmap bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, true);
        winGameContinueOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, false);
        winGameFinalOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, false, false);
        loseGameOverlay = new BitmapDrawable(resources, bitmap);
    }

    private void createEndGameStates(Canvas canvas, boolean win, boolean showButton) {
        int width = endingX - startingX;
        int length = endingY - startingY;
        int middleX = width / 2;
        int middleY = length / 2;
        if (win) {
            lightUpRectangle.setAlpha(127);
            drawDrawable(canvas, lightUpRectangle, 0, 0, width, length);
            lightUpRectangle.setAlpha(255);
            paint.setColor(getResources().getColor(R.color.text_white));
            paint.setAlpha(255);
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            int textBottom = middleY - centerText();
            canvas.drawText(getResources().getString(R.string.you_win), middleX, textBottom, paint);
            paint.setTextSize(bodyTextSize);
            String text = showButton ? getResources().getString(R.string.go_on) :
                    getResources().getString(R.string.for_now);
            canvas.drawText(text, middleX, textBottom + textPaddingSize * 2 - centerText() * 2, paint);
        } else {
            fadeRectangle.setAlpha(127);
            drawDrawable(canvas, fadeRectangle, 0, 0, width, length);
            fadeRectangle.setAlpha(255);
            paint.setColor(getResources().getColor(R.color.text_black));
            paint.setAlpha(255);
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.game_over), middleX, middleY - centerText(), paint);
        }
    }

    private void createBackgroundBitmap(int width, int height) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        drawHeader(canvas);
        drawNewGameButton(canvas, false);
        drawUndoButton(canvas);
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
        drawInstructions(canvas);
    }

    private void drawUndoButton(Canvas canvas) {

        drawDrawable(canvas,
                backgroundRectangle,
                sXUndo,
                sYIcons, sXUndo + iconSize,
                sYIcons + iconSize
        );

        drawDrawable(canvas,
                getResources().getDrawable(R.drawable.ic_action_undo),
                sXUndo + iconPaddingSize,
                sYIcons + iconPaddingSize,
                sXUndo + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize
        );
    }
    private void drawHeader(Canvas canvas) {

        //Drawing the header
        paint.setTextSize(headerTextSize);
        paint.setColor(getResources().getColor(R.color.text_black));
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        int headerStartY = sYAll - textShiftY;
        canvas.drawText(getResources().getString(R.string.header), startingX, headerStartY, paint);
    }

    private void drawInstructions(Canvas canvas) {

        //Drawing the instructions
        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        canvas.drawText(getResources().getString(R.string.instructions),
                startingX, endingY - textShiftY + textPaddingSize, paint);
    }

    private void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY);
    }

    //Renders the set of 16 background squares.
    private void drawBackgroundGrid(Canvas canvas) {
        Resources resources = getResources();
        Drawable backgroundCell = resources.getDrawable(R.drawable.cell_rectangle);
        // Outputting the game grid
        for (int i = 0; i < game.numSquaresX; i++) {
            for (int j = 0; j < game.numSquaresY; j++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * i;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * j;
                int eY = sY + cellSize;

                drawDrawable(canvas, backgroundCell, sX, sY, eX, eY);
            }
        }
    }


    private void createBitmapCells() {
        Resources resources = getResources();
        int[] cellRectangleIds = getCellRectangleIds();
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 1; i < bitmapCell.length; i++) {
            int value = (int) Math.pow(2, i);
            paint.setTextSize(cellTextSize);
            float tempTextSize = cellTextSize * cellSize * 0.9f / Math.max(cellSize * 0.9f, paint.measureText(String.valueOf(value)));
            paint.setTextSize(tempTextSize);
            Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawDrawable(canvas, resources.getDrawable(cellRectangleIds[i]), 0, 0, cellSize, cellSize);
            drawCellText(canvas, value, 0, 0);
            bitmapCell[i] = new BitmapDrawable(resources, bitmap);
        }
    }

    private void drawCellText(Canvas canvas, int value, int sX, int sY) {
        int textShiftY = centerText();
        if (value >= 8) {
            paint.setColor(getResources().getColor(R.color.text_white));
        } else {
            paint.setColor(getResources().getColor(R.color.text_black));
        }
        canvas.drawText("" + value, sX + cellSize / 2, sY + cellSize / 2 - textShiftY, paint);
    }

    private void getLayout(int width, int height) {
        cellSize = Math.min(width / (game.numSquaresX + 1), height / (game.numSquaresY + 3));
        gridWidth = cellSize / 7;
        int screenMiddleX = width / 2;
        int screenMiddleY = height / 2;
        int boardMiddleX = screenMiddleX;
        int boardMiddleY = screenMiddleY + cellSize / 2;
        iconSize = cellSize / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize);
        textSize = cellSize * cellSize / Math.max(cellSize, paint.measureText("0000"));
        cellTextSize = textSize;
        titleTextSize = textSize / 3;
        bodyTextSize = (int) (textSize / 1.5);
        instructionsTextSize = (int) (textSize / 1.5);
        headerTextSize = textSize * 2;
        gameOverTextSize = textSize * 2;
        textPaddingSize = (int) (textSize / 3);
        iconPaddingSize = (int) (textSize / 5);

        //Grid Dimensions
        double halfNumSquaresX = game.numSquaresX / 2d;
        double halfNumSquaresY = game.numSquaresY / 2d;

        startingX = (int) (boardMiddleX - (cellSize + gridWidth) * halfNumSquaresX - gridWidth / 2);
        endingX = (int) (boardMiddleX + (cellSize + gridWidth) * halfNumSquaresX + gridWidth / 2);
        startingY = (int) (boardMiddleY - (cellSize + gridWidth) * halfNumSquaresY - gridWidth / 2);
        endingY = (int) (boardMiddleY + (cellSize + gridWidth) * halfNumSquaresY + gridWidth / 2);

        paint.setTextSize(titleTextSize);

        int textShiftYAll = centerText();
        //static variables
        sYAll = (int) (startingY - cellSize * 1.5);
        titleStartYAll = (int) (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll);
        bodyStartYAll = (int) (titleStartYAll + textPaddingSize + titleTextSize / 2 + bodyTextSize / 2);

        titleWidthHighScore = (int) (paint.measureText(getResources().getString(R.string.high_score)));
        titleWidthScore = (int) (paint.measureText(getResources().getString(R.string.score)));
        paint.setTextSize(bodyTextSize);
        textShiftYAll = centerText();
        eYAll = (int) (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize);

        sYIcons = (startingY + eYAll) / 2 - iconSize / 2;
        sXNewGame = (endingX - iconSize);
        sXUndo = sXNewGame - iconSize * 3 / 2 - iconPaddingSize;
        resyncTime();
    }

    private void tick() {
        currentTime = System.nanoTime();
        game.aGrid.tickAll(currentTime - lastFPSTime);
        lastFPSTime = currentTime;
    }

    private void drawEndlessText(Canvas canvas) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(bodyTextSize);
        paint.setColor(getResources().getColor(R.color.text_black));
        canvas.drawText(getResources().getString(R.string.endless), startingX, sYIcons - centerText() * 2, paint);
    }

    private int centerText() {
        return (int) ((paint.descent() + paint.ascent()) / 2);
    }

    private void drawEndGameState(Canvas canvas) {
        double alphaChange = 1;
        continueButtonEnabled = false;
        for (AnimationCell animation : game.aGrid.globalAnimation) {
            if (animation.getAnimationType() == MainGame.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.getPercentageDone();
            }
        }

        BitmapDrawable displayOverlay = null;
        if (game.gameWon()) {
            if (game.canContinue()) {
                continueButtonEnabled = true;
                displayOverlay = winGameContinueOverlay;
            } else {
                displayOverlay = winGameFinalOverlay;
            }
        } else if (game.gameLost()) {
            displayOverlay = loseGameOverlay;
        }

        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY);
            displayOverlay.setAlpha((int)(255 * alphaChange));
            displayOverlay.draw(canvas);
        }
    }

    private void drawNewGameButton(Canvas canvas, boolean lightUp) {
        if (lightUp) {
            drawDrawable(canvas,
                    lightUpRectangle,
                    sXNewGame,
                    sYIcons,
                    sXNewGame + iconSize,
                    sYIcons + iconSize
            );
        } else {
            drawDrawable(canvas,
                    backgroundRectangle,
                    sXNewGame,
                    sYIcons, sXNewGame + iconSize,
                    sYIcons + iconSize
            );
        }

        drawDrawable(canvas,
                getResources().getDrawable(R.drawable.ic_action_refresh),
                sXNewGame + iconPaddingSize,
                sYIcons + iconPaddingSize,
                sXNewGame + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize
        );
    }

    private void drawDrawable(Canvas canvas, Drawable drawable, int startingX, int startingY, int endingX, int endingY) {
        drawable.setBounds(startingX, startingY, endingX, endingY);
        drawable.draw(canvas);
    }

    private void drawCells(Canvas canvas) {
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);

        for (int i=0; i<game.numSquaresX; i++) {
            for (int j=0; j<game.numSquaresY; j++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * i;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * j;
                int eY = sY + cellSize;

                Tile currentTile = game.grid.getCellContent(i, j);
                if (currentTile != null) {
                    int value = currentTile.getValue();
                    int index = log2(value);

                    ArrayList<AnimationCell> aArray = game.aGrid.getAnimation(i, j);
                    boolean animated = false;
                    for (int k = aArray.size() - 1; k>=0; k--) {
                        AnimationCell aCell = aArray.get(k);
                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) {
                            animated = true;
                        }

                        if (!aCell.isActive()) {
                            continue;
                        }

                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) { // Spawning animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
                            bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MERGE_ANIMATION) { // Merging Animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (1 + INITIAL_VELOCITY * percentDone
                                    + MERGING_ACCELERATION * percentDone * percentDone / 2);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);
                            bitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MOVE_ANIMATION) {  // Moving animation
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.extras[0];
                            int previousY = aCell.extras[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1) * 1.0);
                            int dY = (int) ((currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1) * 1.0);
                            bitmapCell[tempIndex].setBounds(sX + dX, sY + dY, eX + dX, eY + dY);
                            bitmapCell[tempIndex].draw(canvas);
                        }
                        animated = true;
                    }

                    if (!animated) {
                        bitmapCell[index].setBounds(sX, sY, eX, eY);
                        bitmapCell[index].draw(canvas);
                    }
                }
            }
        }
    }

    private void drawScoreText(Canvas canvas) {

    }

    public void resyncTime() {
        lastFPSTime = System.nanoTime();
    }

    private static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }


    public int[] getCellRectangleIds() {
        int[] cellRectangleIds = new int[numCellType];
        cellRectangleIds[0] = R.drawable.cell_rectangle;
        cellRectangleIds[1] = R.drawable.cell_rectangle_2;
        cellRectangleIds[2] = R.drawable.cell_rectangle_4;
        cellRectangleIds[3] = R.drawable.cell_rectangle_8;
        cellRectangleIds[4] = R.drawable.cell_rectangle_16;
        cellRectangleIds[5] = R.drawable.cell_rectangle_32;
        cellRectangleIds[6] = R.drawable.cell_rectangle_64;
        cellRectangleIds[7] = R.drawable.cell_rectangle_128;
        cellRectangleIds[8] = R.drawable.cell_rectangle_256;
        cellRectangleIds[9] = R.drawable.cell_rectangle_512;
        cellRectangleIds[10] = R.drawable.cell_rectangle_1024;
        cellRectangleIds[11] = R.drawable.cell_rectangle_2048;
        for (int i = 12; i < cellRectangleIds.length; i++) {
            cellRectangleIds[i] = R.drawable.cell_rectangle_4096;
        }
        return cellRectangleIds;
    }
}
