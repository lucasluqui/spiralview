package com.threerings.projectx.effect.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.opengl.effect.BaseParticleSystem.Layer;
import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.Placer;
import com.threerings.opengl.effect.config.PlacerConfig;

@EditorTypes({
  ProjectXPlacerConfig.SnapToFlatTopHexGrid.class
})
public abstract class ProjectXPlacerConfig extends PlacerConfig
{
  /**
   * Places particles with another placer, then moves them to the center of the hexagon they
   * landed in, on a grid of flat-topped hexagons.
   */
  public static class SnapToFlatTopHexGrid extends ProjectXPlacerConfig
  {
    /** The placer whose positions are snapped to the grid. */
    @Editable
    public PlacerConfig placer;

    /** The distance from the center of a hexagon to its corners. */
    @Editable(min=0.0, step=0.001)
    public float hexSize;

    @Override
    public Placer createPlacer (Layer layer)
    {
      final Placer inner = placer.createPlacer(layer);
      final float sqrt3 = FloatMath.sqrt(3f);
      return new Placer() {
        public void place (Particle particle) {
          inner.place(particle);
          Vector3f position = particle.getPosition();
          // the axial coordinates of the point, rounded to those of the nearest hexagon
          float q = position.x / (hexSize * 1.5f);
          float r = (position.y * sqrt3 - position.x) / (hexSize * 3f);
          float s = -q - r;
          float rq = FloatMath.round(q), rr = FloatMath.round(r), rs = FloatMath.round(s);
          float dq = Math.abs(rq - q), dr = Math.abs(rr - r), ds = Math.abs(rs - s);
          if (dq > dr && dq > ds) {
            rq = -rr - rs;
          } else if (ds > dr) {
            rs = -rq - rr;
          } else {
            rr = -rq - rs;
          }
          position.x = hexSize * 1.5f * rq;
          position.y = hexSize * sqrt3 * (rr + rq / 2f);
        }
      };
    }
  }
}
