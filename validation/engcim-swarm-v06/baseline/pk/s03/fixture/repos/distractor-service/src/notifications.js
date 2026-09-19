export function sendNotification(recipient, message) {
  return { recipient, message, channel: 'email' };
}
