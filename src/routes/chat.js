const express = require('express');
const { getDb } = require('../db');
const { getIo } = require('../socket');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

router.get('/', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const db = getDb();
  try {
    await req.setUserContext(db);
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

router.post('/:chatId/messages', authMiddleware, async (req, res) => {
  const { chatId } = req.params;
  const { body, encryptedKey, iv, attachmentUrl, ttlSeconds, isVideoCircle } = req.body;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();
  try {
    await req.setUserContext(db);
    let expiresAt = ttlSeconds ? new Date(Date.now() + ttlSeconds * 1000) : null;
    const result = await db.query(
      'INSERT INTO messages (chat_id, sender_id, body, encrypted_key, iv, attachment_url, expires_at, "isVideoCircle") VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *',
      [chatId, userId, body, encryptedKey, iv, attachmentUrl, expiresAt, isVideoCircle || false]
    );
    const message = result.rows[0];
    const members = await db.query('SELECT user_id FROM chat_members WHERE chat_id = $1', [chatId]);
    members.rows.forEach(member => io.to(member.user_id).emit('new_message', message));
    res.status(201).json(message);
  } catch (err) {
    res.status(500).json({ error: 'Failed to send message' });
  }
});

router.delete('/messages/:messageId', authMiddleware, async (req, res) => {
  const { messageId } = req.params;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();
  try {
    await req.setUserContext(db);
    const msgRes = await db.query('SELECT chat_id, sender_id FROM messages WHERE id = $1', [messageId]);
    if (msgRes.rows.length === 0) return res.status(404).json({ error: 'Message not found' });
    if (msgRes.rows[0].sender_id !== userId) return res.status(403).json({ error: 'Unauthorized' });
    await db.query('DELETE FROM messages WHERE id = $1', [messageId]);
    const members = await db.query('SELECT user_id FROM chat_members WHERE chat_id = $1', [msgRes.rows[0].chat_id]);
    members.rows.forEach(m => io.to(m.user_id).emit('message_deleted', { messageId, chatId: msgRes.rows[0].chat_id }));
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete message' });
  }
});

router.post('/:chatId/read', authMiddleware, async (req, res) => {
  const { chatId } = req.params;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();
  try {
    await req.setUserContext(db);
    await db.query('UPDATE messages SET is_read = true WHERE chat_id = $1 AND sender_id != $2 AND is_read = false', [chatId, userId]);
    const result = await db.query('SELECT DISTINCT sender_id FROM messages WHERE chat_id = $1 AND sender_id != $2', [chatId, userId]);
    result.rows.forEach(row => io.to(row.sender_id).emit('messages_read', { chatId, readerId: userId }));
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to mark messages as read' });
  }
});

module.exports = router;
