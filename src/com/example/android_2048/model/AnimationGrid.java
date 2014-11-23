package com.example.android_2048.model;

import java.util.ArrayList;

/**
 * Created by xiefuheng on 14-11-22.
 */
public class AnimationGrid {
    public ArrayList<AnimationCell>[][] field;
    int activeAnimations = 0;
    boolean moreFrame = false;
    public ArrayList<AnimationCell> globalAnimation = new ArrayList<AnimationCell>();

    public AnimationGrid(int width, int height) {
        this.field = new ArrayList[width][height];

        for (int i=0; i<width; i++) {
            for (int j=0; j<height; j++) {
                field[i][j] = new ArrayList<AnimationCell>();
            }
        }
    }

    public void startAniamtion(int x, int y, int animationType, long length,
                               long delay, int[] extras) {
        AnimationCell animationAdd = new AnimationCell(x, y, animationType, length, delay, extras);
        if (x == -1 && y == -1) {
            globalAnimation.add(animationAdd);
        } else {
            field[x][y].add(animationAdd);
        }
        activeAnimations += 1;
    }

    public boolean isAnimationActive() {
        if (activeAnimations != 0) {
            moreFrame = true;
            return true;
        } else if (moreFrame) {
            moreFrame = false;
            return true;
        } else {
            return false;
        }
    }

    public void cancelAnimations() {
        for (ArrayList<AnimationCell>[] array : field) {
            for (ArrayList<AnimationCell> list : array) {
                list.clear();
            }
        }
        globalAnimation.clear();
        activeAnimations = 0;
    }

    public ArrayList<AnimationCell> getAnimation(int i, int j) {
        return field[i][j];
    }

    public void tickAll(long timeElapsed) {
        ArrayList<AnimationCell> cancelledAnimations = new ArrayList<AnimationCell>();
        for (AnimationCell animation : globalAnimation) {
            animation.tick(timeElapsed);
            if (animation.animationDone()) {
                cancelledAnimations.add(animation);
                activeAnimations = activeAnimations - 1;
            }
        }

        for (ArrayList<AnimationCell>[] array : field) {
            for (ArrayList<AnimationCell> list : array) {
                for (AnimationCell animation : list) {
                    animation.tick(timeElapsed);
                    if (animation.animationDone()) {
                        cancelledAnimations.add(animation);
                        activeAnimations = activeAnimations - 1;
                    }
                }
            }
        }

        for (AnimationCell animation : cancelledAnimations) {
            cancelAnimation(animation);
        }
    }

    private void cancelAnimation(AnimationCell animation) {
        if (animation.getX() == -1 && animation.getY() == -1) {
            globalAnimation.remove(animation);
        } else {
            field[animation.getX()][animation.getY()].remove(animation);
        }
    }
}
