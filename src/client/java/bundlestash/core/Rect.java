package bundlestash.core;

/**
 * 整数矩形。所有布局计算与命中检测都基于同一组矩形，避免出现"画在这里、判定在别处"的问题。
 */
public record Rect(int x, int y, int width, int height) {

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    public Rect translated(int dx, int dy) {
        return new Rect(x + dx, y + dy, width, height);
    }

    public Rect expanded(int by) {
        return new Rect(x - by, y - by, width + by * 2, height + by * 2);
    }
}
