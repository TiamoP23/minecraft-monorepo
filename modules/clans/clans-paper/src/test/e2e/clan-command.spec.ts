import { expect, test } from '@drownek/paperwright';

test('pvp status command displays current mode', async ({ player }) => {
  player.chat('/pvp status');
  await expect(player).toHaveReceivedMessage('PvP:');
});

test('pvp on command enables PvP', async ({ player }) => {
  player.chat('/pvp on');
  await expect(player).toHaveReceivedMessage('PvP ON');
});

test('pvp off command disables PvP', async ({ player }) => {
  player.chat('/pvp off');
  await expect(player).toHaveReceivedMessage('PvP OFF');
});
