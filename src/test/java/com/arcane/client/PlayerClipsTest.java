package com.arcane.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arcane.client.anim.PlayerClips;
import org.junit.jupiter.api.Test;

class PlayerClipsTest {
  @Test
  void everyClipIdExistsAndHasChainedKeys() {
    for (int id = 0; id <= 5; id++) {
      var clip = PlayerClips.byId(id);
      assertNotNull(clip, "clipe " + id);
      assertTrue(clip.keys().size() >= 4, clip.name() + " deve encadear varios quadros");
      assertTrue(clip.total() > 10);
    }
    assertEquals(null, PlayerClips.byId(99));
  }

  @Test
  void blendWeightFadesInAndOutAndPoseIsContinuousInside() {
    var clip = PlayerClips.CAST_FINISH;
    float[] a = new float[9], b = new float[9];
    assertEquals(0f, PlayerClips.sample(clip, -1f, a));
    assertEquals(0f, PlayerClips.sample(clip, clip.total(), a));
    float mid = PlayerClips.sample(clip, clip.total() / 2f, a);
    assertTrue(mid > 0.99f, "no meio o peso e total");
    // sem saltos: dois instantes vizinhos diferem pouco
    for (float t = 1f; t < clip.total() - 1; t += 0.5f) {
      PlayerClips.sample(clip, t, a);
      PlayerClips.sample(clip, t + 0.05f, b);
      for (int i = 0; i < 9; i++) assertTrue(Math.abs(a[i] - b[i]) < 6f, clip.name() + " salto no canal " + i + " em t=" + t);
    }
  }
}
