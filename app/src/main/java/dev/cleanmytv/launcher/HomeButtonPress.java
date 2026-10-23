package dev.cleanmytv.launcher;

/** Key policy separated from Android so forwarding and cancelled presses can be tested. */
final class HomeButtonPress {
    enum Result { PASS, CONSUME, OPEN_HOME }
    private boolean pressed;
    private long downTime;

    Result handle(boolean home, boolean down, boolean up, int repeats,
                  long eventDownTime, boolean cancelled) {
        if (!home || (!down && !up)) return Result.PASS;
        if (down) {
            if (repeats == 0) {
                pressed = true;
                downTime = eventDownTime;
            }
            return Result.CONSUME;
        }
        boolean open = pressed && downTime == eventDownTime && !cancelled;
        reset();
        return open ? Result.OPEN_HOME : Result.CONSUME;
    }

    void reset() { pressed = false; }
}
