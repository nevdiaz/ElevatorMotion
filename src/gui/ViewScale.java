package gui;

import api.ShaftSpec;

/** Turns feet along the shaft into pixels in the drawing, for one shaft specification. */
public final class ViewScale {

    public static final double pixelsPerFoot = 20;
    public static final double cabinSize = 8;
    public static final double headroomAboveTopFloor = 12;   //ft of shaft drawn above the top floor
    public static final double pitBelowFloorOne = 4;         //ft of shaft drawn below floor 1

    private final ShaftSpec spec;
    private final double floorOneYValue;
    private final double shaftHeight;

    public ViewScale(ShaftSpec spec) {
        this.spec = spec;
        double travel = spec.floorPosition(spec.floors());   //feet between floor 1 and the top floor
        this.floorOneYValue = (travel + headroomAboveTopFloor) * pixelsPerFoot;
        this.shaftHeight = (headroomAboveTopFloor + travel + pitBelowFloorOne) * pixelsPerFoot;
    }

    public int floorCount() {
        return spec.floors();
    }

    /** Feet past floor 1 and the top floor where the safety levers sit. */
    public double leverMargin() {
        return spec.leverMargin();
    }

    /** Height of the whole drawn shaft in pixels. */
    public double shaftHeight() {
        return shaftHeight;
    }

    public double floorToFeet(int floorNumber) {
        return spec.floorPosition(floorNumber);
    }

    public double feetToWorldYValue(double feet) {
        return floorOneYValue - feet * pixelsPerFoot;
    }
}