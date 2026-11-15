package gui;

import api.CabinAssemblyAPI;

import java.util.function.IntSupplier;

/** This will allow the GUI to access everything it needs for one cabin. */
public record CabinView(String name, CabinAssemblyAPI cabin, IntSupplier nudgingFloor) {
}
