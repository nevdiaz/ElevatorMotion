package gui;

import api.CabinAssemblyAPI;
import api.ShaftSpec;
import java.util.function.IntSupplier;

/** Everything the GUI needs to draw one cabin. Read-only. */
public record CabinView(String name, CabinAssemblyAPI cabin, IntSupplier nudgingFloor, ShaftSpec spec) {}