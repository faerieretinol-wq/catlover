const express = require('express');
const { getDb } = require('../db');
const { getIo } = require('../socket');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

router.post('/create', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const { title, memberIds, isGroup } = req.body;
  const db = getDb();
  try {
    await req.setUserContext(db);

    const members = Array.isArray(memberIds) ? memberIds : [];
    const uniqueMembers = Array.from(new Set([userId, ...members].filter(Boolean)));
    if (uniqueMembers.length < 2) {
      return res.status(400).json({ error: 'At least 2 members required' });
    }

    await db.query('BEGIN');
    const chatRes = await db.query(
      'INSERT INTO chats (title, is_group) VALUES ($1, $2) RETURNING *',
      [title || null, Boolean(isGroup)]
    );
    const chat = chatRes.rows[0];

    for (const memberId of uniqueMembers) {
      await db.query(
        'INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
        [chat.id, memberId]
      );
    }
    await db.query('COMMIT');

    res.status(201).json(chat);
  } catch (err) {
    await db.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: 'Failed to create chat' });
  }
});

router.post('/dm', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const { userId: otherUserId } = req.body;
  const db = getDb();
  try {
    await req.setUserContext(db);

    if (!otherUserId || typeof otherUserId !== 'string' || otherUserId === userId) {
      return res.status(400).json({ error: 'Invalid userId' });
    }

    const existing = await db.query(
      `SELECT c.*
       FROM chats c
       JOIN chat_members cm1 ON cm1.chat_id = c.id AND cm1.user_id = $1
       JOIN chat_members cm2 ON cm2.chat_id = c.id AND cm2.user_id = $2
       WHERE c.is_group = false`,
      [userId, otherUserId]
    );
    if (existing.rows.length > 0) return res.json(existing.rows[0]);

    await db.query('BEGIN');
    const chatRes = await db.query(
      'INSERT INTO chats (title, is_group) VALUES (NULL, false) RETURNING *',
      []
    );
    const chat = chatRes.rows[0];

    await db.query(
      'INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
      [chat.id, userId]
    );
    await db.query(
      'INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
      [chat.id, otherUserId]
    );
    await db.query('COMMIT');

    res.status(201).json(chat);
  } catch (err) {
    await db.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: 'Failed to create dm' });
  }
});

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

router.get('/:chatId/messages', authMiddleware, async (req, res) => {
  const { chatId } = req.params;
  const userId = req.user.userId;
  const db = getDb();
  try {
    await req.setUserContext(db);
    const member = await db.query('SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2', [chatId, userId]);
    if (member.rows.length === 0) return res.status(403).json({ error: 'Unauthorized' });

    const result = await db.query(
      'SELECT * FROM messages WHERE chat_id = $1 ORDER BY created_at ASC LIMIT 200',
      [chatId]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch messages' });
  }
});

router.post('/:chatId/messages', authMiddleware, async (req, res) => {
  const { chatId } = req.params;
  const {
    body,
    encryptedKey,
    iv,
    attachmentUrl,
    ttlSeconds,
    isVideoCircle,
    attachmentPath,
    ttl
  } = req.body;
  const userId = req.user.userId;
  const db = getDb();
  const io = getIo();
  try {
    await req.setUserContext(db);

    const normalizedAttachmentUrl = attachmentUrl ?? attachmentPath ?? null;
    const normalizedTtlSeconds = typeof ttlSeconds === 'number' ? ttlSeconds : (typeof ttl === 'number' ? ttl : null);
    let expiresAt = normalizedTtlSeconds ? new Date(Date.now() + normalizedTtlSeconds * 1000) : null;
    const result = await db.query(
      'INSERT INTO messages (chat_id, sender_id, body, encrypted_key, iv, attachment_url, expires_at, "isVideoCircle") VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *',
      [chatId, userId, body, encryptedKey || null, iv || null, normalizedAttachmentUrl, expiresAt, isVideoCircle || false]
    );
    const message = result.rows[0];

    const senderRes = await db.query('SELECT username FROM users WHERE id = $1', [userId]);
    const senderName = senderRes.rows[0]?.username || 'User';

    const members = await db.query('SELECT user_id FROM chat_members WHERE chat_id = $1', [chatId]);

    members.rows.forEach(member => {
      io.to(member.user_id).emit('new_message', message);
      io.to(member.user_id).emit('message', {
        chatId,
        body: message.body,
        senderId: userId,
        senderName,
        attachmentPath: message.attachment_url || null,
        createdAt: message.created_at
      });
    });
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
