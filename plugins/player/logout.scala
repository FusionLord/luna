/*
 A plugin that adds functionality for performing specific tasks on logout.

 SUPPORTS:
  -> Logging the player out when the logout button is clicked.

 AUTHOR: lare96
*/

import io.luna.game.event.impl.ButtonClickEvent


/* If the logout button is clicked, logout the player. */
// TODO Disable the logout button during combat and 10 seconds after combat.
onargs[ButtonClickEvent](2458) { _.plr.logout }
