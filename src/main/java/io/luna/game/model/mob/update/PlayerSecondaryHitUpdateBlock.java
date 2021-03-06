package io.luna.game.model.mob.update;

import io.luna.game.model.mob.Hit;
import io.luna.game.model.mob.Player;
import io.luna.game.model.mob.Skill;
import io.luna.game.model.mob.update.UpdateFlagSet.UpdateFlag;
import io.luna.net.codec.ByteMessage;
import io.luna.net.codec.ByteTransform;

/**
 * A {@link PlayerUpdateBlock} implementation for the {@code SECONDARY_HIT} update block.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class PlayerSecondaryHitUpdateBlock extends PlayerUpdateBlock {

    /**
     * Creates a new {@link PlayerSecondaryHitUpdateBlock}.
     */
    public PlayerSecondaryHitUpdateBlock() {
        super(0x200, UpdateFlag.SECONDARY_HIT);
    }

    @Override
    public void write(Player mob, ByteMessage msg) {
        Hit hit = mob.getSecondaryHit().get();
        Skill hitpoints = mob.skill(Skill.HITPOINTS);

        msg.put(hit.getDamage());
        msg.put(hit.getType().getOpcode(), ByteTransform.S);
        msg.put(hitpoints.getLevel());
        msg.put(hitpoints.getStaticLevel(), ByteTransform.C);
    }
}
