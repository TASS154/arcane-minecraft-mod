package com.arcane.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightServiceTest {
  @Test
  void speedAndGlideRoundTripThroughTheAmplifier() {
    for (double speed : new double[]{0.3, 0.6, 1.5, 3.0}) {
      for (double glide : new double[]{0.0, 0.5, 1.0}) {
        int amp = FlightService.encode(speed, glide);
        assertTrue(amp >= 0 && amp <= 255, "cabe no byte do efeito: " + amp);
        assertEquals(speed, FlightService.speedOf(amp), 0.06);
        assertEquals(glide, FlightService.glideOf(amp), 1.0 / 14 + 1e-9);
      }
    }
  }

  @Test
  void noGlideKeepsTheOldEncodingOfTheSpeed() {
    assertEquals(15, FlightService.encode(1.5, 0.0));
    assertEquals(0.0, FlightService.glideOf(15), 1e-9);
  }

  @Test
  void outOfRangeValuesAreClamped() {
    assertEquals(30, FlightService.encode(99.0, 0.0) & 31);
    assertEquals(3, FlightService.encode(-5.0, 0.0) & 31);
    assertEquals(1.0, FlightService.glideOf(FlightService.encode(1.0, 9.0)), 1e-9);
  }
}
