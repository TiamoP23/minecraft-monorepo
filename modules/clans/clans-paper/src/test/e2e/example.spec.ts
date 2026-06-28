import {expect, test} from '@drownek/paperwright';

test('help displays message', async ({ player, server }) => {
  player.chat('/help');
  await expect(player).toHaveReceivedMessage('Help');
});