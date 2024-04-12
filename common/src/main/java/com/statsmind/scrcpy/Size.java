package com.statsmind.scrcpy;

import android.graphics.Rect;
import lombok.Data;

import java.util.Objects;

@Data
public final class Size {
    private final int width;
    private final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size rotate() {
        return new Size(height, width);
    }

    public Rect toRect() {
        return new Rect(0, 0, width, height);
    }
}
