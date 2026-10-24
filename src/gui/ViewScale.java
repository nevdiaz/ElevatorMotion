package gui;

public final class ViewScale {

    private ViewScale() {}

    public static final int pixelsPerFoot = 20;

    public static final int floorHeight = 14;

    public static final int floorCount = 10;

    public static final int cabinSize = 8;

    public static final int headroomAboveFloorTen = 12;

    public static final int pitBelowFloorOne = 4;

    public static final int totalTravelBetweenFloorOneAndTen = (floorCount - 1) * floorHeight; //126

    public static final int floorOneYValue =
            (totalTravelBetweenFloorOneAndTen + headroomAboveFloorTen) * pixelsPerFoot; //2760

    public static final int shaftHeight =
            ((headroomAboveFloorTen + totalTravelBetweenFloorOneAndTen + pitBelowFloorOne) * pixelsPerFoot); //2840

    public static double floorToFeet(int floorNumber) {
        return (floorNumber - 1) * floorHeight;
    }

    public static double feetToWorldYValue(double feet) {
        return floorOneYValue - feet * pixelsPerFoot;
    }


}
