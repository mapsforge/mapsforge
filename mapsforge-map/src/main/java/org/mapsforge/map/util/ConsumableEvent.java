package org.mapsforge.map.util;

public class ConsumableEvent {
    private boolean consumed = false;

    /**
     * For Desktop can be MouseEvent or MouseWheelEvent
     * <br>
     * For Android can be MotionEvent or List of MotionEvents
     * */
    private final Object event;

    public ConsumableEvent() {
        this(null);
    }

    public ConsumableEvent(Object event) {
        this.event = event;
    }

    public Object getEvent() {
        return event;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        consumed = true;
    }
}
