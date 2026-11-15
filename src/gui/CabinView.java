package gui;

import api.CabinAssemblyAPI;

import java.util.function.IntSupplier;

public record CabinView(String name, CabinAssemblyAPI cabin, IntSupplier nudgingFloor) {
}
