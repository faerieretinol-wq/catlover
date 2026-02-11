const express = require('express');
const { getDb } = require('../db');
const { getIo } = require('../socket');
const router = express.Router();

// Получение списка чатов
router.get('/', async (req, res) => {
  const userId = req.user.userId; // Предполагается наличие auth middleware
  const db = getDb();
  try {
    const chats = await db.query(`
      SELECT c.*, 
      (SELECT body FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message,
      (SELECT created_at FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message_at
      FROM chats c
      JOIN chat_members cm ON c.id = cm.chat_id
      WHERE cm.user_id = $1
    `, [userId]);
    res.json(chats.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch chats' });
  }
});

// Отправка сообщения (поддержка E2EE, вложений и таймера)
router.post('/:chatId/messages', async (req, res) => {
  const { chatId } = req.params;
  const { body, encryptedKey, iv, attachmentUrl, ttlSeconds } = req.body;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();

  try {
    let expiresAt = null;
    if (ttlSeconds) {
      expiresAt = new Date(Date.now() + ttlSeconds * 1000);
    }

    const result = await db.query(
      'INSERT INTO messages (chat_id, sender_id, body, encrypted_key, iv, attachment_url, expires_at) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *',
      [chatId, userId, body, encryptedKey, iv, attachmentUrl, expiresAt]
    );
    const message = result.rows[0];

    // Рассылка через сокеты всем участникам чата
    const members = await db.query('SELECT user_id FROM chat_members WHERE chat_id = $1', [chatId]);
    members.rows.forEach(member => {
      io.to(member.user_id).emit('new_message', message);
    });

    res.status(201).json(message);
  } catch (err) {
    res.status(500).json({ error: 'Failed to send message' });
  }
});

// Удаление сообщения
router.delete('/messages/:messageId', async (req, res) => {
  const { messageId } = req.params;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();

  try {
    const messageResult = await db.query('SELECT chat_id, sender_id FROM messages WHERE id = $1', [messageId]);
    if (messageResult.rows.length === 0) return res.status(404).json({ error: 'Message not found' });
    
    const message = messageResult.rows[0];
    if (message.sender_id !== userId) return res.status(403).json({ error: 'Unauthorized' });

    await db.query('DELETE FROM messages WHERE id = $1', [messageId]);

    // Уведомляем участников чата об удалении
    const members = await db.query('SELECT user_id FROM chat_members WHERE chat_id = $1', [message.chat_id]);
    members.rows.forEach(member => {
      io.to(member.user_id).emit('message_deleted', { messageId, chatId: message.chat_id });
    });

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete message' });
  }
});

// Пометка сообщений как прочитанных
router.post('/:chatId/read', async (req, res) => {
  const { chatId } = req.params;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();

  try {
    await db.query(
      'UPDATE messages SET is_read = true WHERE chat_id = $1 AND sender_id != $2 AND is_read = false',
      [chatId, userId]
    );

    // Уведомляем отправителей о прочтении
    const result = await db.query('SELECT DISTINCT sender_id FROM messages WHERE chat_id = $1 AND sender_id != $2', [chatId, userId]);
    result.rows.forEach(row => {
      io.to(row.sender_id).emit('messages_read', { chatId, readerId: userId });
    });

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to mark messages as read' });
  }
});

module.exports = router;
