package net.caffeinemc.mods.sodium.client.render.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.caffeinemc.mods.sodium.client.render.vertex.buffer.BufferBuilderExtension;

import javax.annotation.Nullable;

public class VertexConsumerUtils {
    /**
     * Attempt to convert a {@link VertexConsumer} into a {@link VertexBufferWriter}. If this fails, return null
     * and log a message.
     * @param consumer the consumer to convert
     * @return a {@link VertexBufferWriter}, or null if the consumer does not support this
     */
    public static @Nullable VertexBufferWriter convertOrLog(VertexConsumer consumer) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);

        if (writer == null) {
            VertexConsumerTracker.logBadConsumer(consumer);
        }

        return writer;
    }

    // https://github.com/CaffeineMC/sodium/issues/3703
    public static VertexBufferWriter convertParticle(VertexConsumer vertexConsumer) {
        if (!(vertexConsumer instanceof BufferBuilderExtension extension)) {
            return null;
        }

        if (extension.sodium$getVertexFormat() != ParticleVertex.FORMAT) {
            return null;
        }

        return VertexBufferWriter.tryOf(vertexConsumer);
    }
}
