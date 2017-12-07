/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.IceWorld;

import org.terasology.math.ChunkMath;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldRasterizer;

public class FloraRasterizer implements WorldRasterizer {
    Block RedFlower;

    @Override
    public void initialize() {
        RedFlower = CoreRegistry.get(BlockManager.class).getBlock("Core:RedFlower");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        FloraFacet FloraFacet = chunkRegion.getFacet(FloraFacet.class);

        for(Vector3i block : FloraFacet.getWorldRegion())  {
            chunk.setBlock(ChunkMath.calcBlockPos(block.add(0,1,0)),RedFlower);
        }
    }
}
