/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.caveworld;

import org.terasology.math.ChunkMath;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldRasterizerPlugin;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
public class CaveWorldRasterizer implements WorldRasterizerPlugin {

    private Block ground;
    private Block lava;

    @Override
    public void initialize() {
        ground = CoreRegistry.get(BlockManager.class).getBlock("Core:Stone");
        lava = CoreRegistry.get(BlockManager.class).getBlock("Core:Lava");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        HeightFacet surfaceHeightFacet = chunkRegion.getFacet(HeightFacet.class);
        AmplitudeFacet amplitudeFacet = chunkRegion.getFacet(AmplitudeFacet.class);

        for (Vector3i position : chunkRegion.getRegion()) {
            if (position.y > amplitudeFacet.getAmplitude()) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), ground);
                continue;
            }

            float surfaceOffset = surfaceHeightFacet.getWorld(position.x, position.z);
            float ceilingHeight = amplitudeFacet.getAmplitude();
            float centerHeight = 0;
            float floorHeight = -ceilingHeight + 1;

            if (Math.abs(centerHeight - position.y) >= surfaceOffset) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), ground);

            } else if (position.y <= floorHeight) {
                chunk.setBlock(ChunkMath.calcBlockPos(position), lava);
            }
        }
    }
}
