/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.additionalworlds.caveworld;

import org.terasology.entitySystem.Component;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.rendering.nui.properties.Range;
import org.terasology.utilities.procedural.AbstractNoise;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.PerlinNoise;
import org.terasology.utilities.procedural.SubSampledNoise;
import org.terasology.world.generation.ConfigurableFacetProvider;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generation.Requires;
import org.terasology.world.generation.facets.SurfaceHeightFacet;

@Produces(LavalakeFacet.class)
@Requires(@Facet(SurfaceHeightFacet.class))
public class LavalakeFacetProvider implements ConfigurableFacetProvider {
    private LavalakeFacetProviderConfiguration configuration = new LavalakeFacetProviderConfiguration();

    private Noise baseCaveNoise;

    @Override
    public void setSeed(long seed) {
        baseCaveNoise = new PerlinNoise(seed + 2);
    }

    @Override
    public void process(GeneratingRegion region) {
        float width = configuration.width;
        float height = configuration.height;
        float noiseLevel = configuration.rawAmount;

        // at default settings,  make caves wider than tall.
        SubSampledNoise caveNoise = new SubSampledNoise(new RidgedNoise(baseCaveNoise, 2), new Vector3f(0.03f * (1f / width), 0.09f * (1f / height), 0.03f * (1f / width)), 4);
        LavalakeFacet facet = new LavalakeFacet(region.getRegion(), region.getBorderForFacet(LavalakeFacet.class));

        // get noise in batch for performance reasons.  Getting it by individual position takes 10 times as long
        float[] caveNoiseValues = caveNoise.noise(facet.getWorldRegion());

        for (Vector3i pos : region.getRegion()) {
            float noiseValue = caveNoiseValues[facet.getWorldIndex(pos)];

            facet.setWorld(pos, noiseValue > noiseLevel);
        }

        region.setRegionFacet(LavalakeFacet.class, facet);
    }


    @Override
    public String getConfigurationName() {
        return "Caves";
    }

    @Override
    public Component getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Component configuration) {
        this.configuration = (LavalakeFacetProviderConfiguration) configuration;
    }

    private static class LavalakeFacetProviderConfiguration implements Component {
        @Range(min = 0, max = 1f, increment = 0.05f, precision = 2, description = "Width")
        public float width = 0.3f;
        @Range(min = 0, max = 1f, increment = 0.05f, precision = 2, description = "Height")
        public float height = 0.3f;
        @Range(min = -1f, max = 1, increment = 0.05f, precision = 2, description = "Raw Amount (use at own risk)")
        public float rawAmount = 0.3f;

    }

    public static class RidgedNoise extends AbstractNoise {

        /**
         * Default persistence value
         */
        public static final double DEFAULT_PERSISTENCE = 0.836281;

        /**
         * Default lacunarity value
         */
        public static final double DEFAULT_LACUNARITY = 2.1379201;

        private double lacunarity = DEFAULT_LACUNARITY;
        private double persistence = DEFAULT_PERSISTENCE;

        private int octaves;
        private float[] spectralWeights;
        //private float scale;                // 1/sum of all weights
        private final Noise other;

        /**
         * Initialize with 9 octaves - <b>this is quite expensive, but backwards compatible</b>
         *
         * @param other the noise to use as a basis
         */
        public RidgedNoise(Noise other) {
            this(other, 9);
        }

        /**
         * @param other   other the noise to use as a basis
         * @param octaves the number of octaves to use
         */
        public RidgedNoise(Noise other, int octaves) {
            this.other = other;
            setOctaves(octaves);
        }

        /**
         * Returns Ridged noise at the given position.
         *
         * @param x Position on the x-axis
         * @param y Position on the y-axis
         * @param z Position on the z-axis
         * @return The noise value in the range of the base noise function
         */
        @Override
        public float noise(float x, float y, float z) {
            float result = other.noise(x, y, z);

            float workingX = x;
            float workingY = y;
            float workingZ = z;
            for (int i = 1; i < getOctaves(); i++) {
                workingX *= Math.pow(2, i) * Math.abs(result);
                workingY *= Math.pow(2, i) * Math.abs(result);
                workingZ *= Math.pow(2, i) * Math.abs(result);
                result += Math.abs(other.noise(workingX, workingY, workingZ)) * (1f / Math.pow(2, i));

            }

            return result;// * scale;
        }

//        private static float computeScale(float[] spectralWeights) {
//            float sum = 0;
//            for (float weight : spectralWeights) {
//                sum += weight;
//            }
//            return 1.0f / sum;
//        }

        /**
         * @param octaves the number of octaves used for computation
         */
        public void setOctaves(int octaves) {
            this.octaves = octaves;
            updateWeights();
        }

        /**
         * @return the number of octaves
         */
        public int getOctaves() {
            return octaves;
        }

        /**
         * Lacunarity is what makes the frequency grow. Each octave
         * the frequency is multiplied by the lacunarity.
         *
         * @return the lacunarity
         */
        public double getLacunarity() {
            return this.lacunarity;
        }

        /**
         * Lacunarity is what makes the frequency grow. Each octave
         * the frequency is multiplied by the lacunarity.
         *
         * @param lacunarity the lacunarity
         */
        public void setLacunarity(double lacunarity) {
            this.lacunarity = lacunarity;
        }

        /**
         * Persistence is what makes the amplitude shrink.
         * More precicely the amplitude of octave i = lacunarity^(-persistence * i)
         *
         * @return the persistance
         */
        public double getPersistance() {
            return this.persistence;
        }

        /**
         * Persistence is what makes the amplitude shrink.
         * More precisely the amplitude of octave i = lacunarity^(-persistence * i)
         *
         * @param persistence the persistence to set
         */
        public void setPersistence(double persistence) {
            this.persistence = persistence;
            updateWeights();
        }

        private void updateWeights() {
            // recompute weights eagerly
            spectralWeights = new float[octaves];

            for (int i = 0; i < octaves; i++) {
                spectralWeights[i] = (float) Math.pow(lacunarity, -persistence * i);
            }

            //scale = computeScale(spectralWeights);
        }
    }

}
