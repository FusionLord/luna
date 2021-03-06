package io.luna.net.msg.out;

import io.luna.game.model.mob.Player;
import io.luna.net.codec.ByteMessage;
import io.luna.net.msg.MessageWriter;

/**
 * A {@link MessageWriter} that opens an interface.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class InterfaceMessageWriter extends MessageWriter {

    /**
     * The interface identifier.
     */
    private final int id;

    /**
     * Creates a new {@link InterfaceMessageWriter}.
     *
     * @param id The interface identifier.
     */
    public InterfaceMessageWriter(int id) {
        this.id = id;
    }

    @Override
    public ByteMessage write(Player player) {
        ByteMessage msg = ByteMessage.message(97);
        msg.putShort(id);
        return msg;
    }
}
