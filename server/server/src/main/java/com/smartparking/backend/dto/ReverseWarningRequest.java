package com.smartparking.backend.dto;

/**
 * Reverse-parking warning event pushed by the serial bridge (serial_bridge.py)
 * to {@code POST /api/parking/reverse-warning}.
 *
 * The bridge forwards the firmware's
 * {@code {"type":"REVERSE_WARNING","slot":2,"active":true|false}} messages
 * so the web dashboard can display the reverse-manoeuvre warning state.
 *
 * Both {@code reverseWarning} and {@code active} are accepted — the firmware
 * emits {@code active} and the bridge mirrors it under {@code reverseWarning}.
 */
public class ReverseWarningRequest {

    /**
     * Campus slot number (1-based). Slot 2 is the reverse-parking space.
     */
    private Integer slotNumber;

    /**
     * Whether the reverse warning is active.
     */
    private Boolean reverseWarning;

    /**
     * Alias for {@link #reverseWarning}; the firmware emits this key.
     */
    private Boolean active;

    public ReverseWarningRequest() {
    }

    public ReverseWarningRequest(Integer slotNumber, Boolean active) {
        this.slotNumber = slotNumber;
        this.active = active;
        this.reverseWarning = active;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public Boolean getReverseWarning() {
        return reverseWarning;
    }

    public void setReverseWarning(Boolean reverseWarning) {
        this.reverseWarning = reverseWarning;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Resolve the active flag from either key, defaulting to false.
     */
    public boolean isActive() {
        if (active != null) {
            return active;
        }
        if (reverseWarning != null) {
            return reverseWarning;
        }
        return false;
    }
}