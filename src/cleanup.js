const { getDb } = require('./db');
const { getIo } = require('./socket');

function startCleanupTask() {
  setInterval(async () => {
    const db = getDb();
    const io = getIo();
    try {
      // Находим истекшие сообщения
      const expired = await db.query(
        'DELETE FROM messages WHERE expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP RETURNING id, chat_id'
      );

      if (expired.rows.length > 0) {
        console.log(`🧹 Deleted ${expired.rows.length} expired messages`);
        
        // Уведомляем клиентов через сокеты
        expired.rows.forEach(msg => {
          io.emit('message_deleted', { messageId: msg.id, chatId: msg.chat_id, reason: 'expired' });
        });
      }
    } catch (err) {
      console.error('❌ Cleanup task error:', err);
    }
  }, 10000); // Проверка каждые 10 секунд
}

module.exports = { startCleanupTask };
