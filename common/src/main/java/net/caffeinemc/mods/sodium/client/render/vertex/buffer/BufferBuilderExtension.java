package net.caffeinemc.mods.sodium.client.render.vertex.buffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

public interface BufferBuilderExtension extends VertexBufferWriter {
    void sodium$duplicateVertex();
    VertexFormat sodium$getVertexFormat();
}
