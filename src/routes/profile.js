const express = require('express');
const { getDb } = require('../db');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

router.get('/me', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const db = getDb();
  try {
    await req.setUserContext(db);
    const result = await db.query(`SELECT u.username, u.email, p.avatar_url, p.bio, p.status FROM users u JOIN user_profiles p ON u.id = p.user_id WHERE u.id = $1`, [userId]);
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed' });
  }
});

router.post('/update', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const { bio, status, avatarUrl } = req.body;
  const db = getDb();
  try {
    await req.setUserContext(db);
    await db.query(`UPDATE user_profiles SET bio = COALESCE($1, bio), status = COALESCE($2, status), avatar_url = COALESCE($3, avatar_url), updated_at = CURRENT_TIMESTAMP WHERE user_id = $4`, [bio, status, avatarUrl, userId]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed' });
  }
});

router.get('/search', authMiddleware, async (req, res) => {
  const { q } = req.query;
  if (!q || typeof q !== 'string' || q.length > 50) {
    return res.status(400).json({ error: 'Invalid query' });
  }
  const db = getDb();
  try {
    await req.setUserContext(db);
    const sanitized = q.replace(/[%_]/g, '');
    const result = await db.query(`SELECT u.id, u.username, p.avatar_url, p.status FROM users u JOIN user_profiles p ON u.id = p.user_id WHERE u.username ILIKE $1 LIMIT 20`, [`%${sanitized}%`]);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Failed' });
  }
});

module.exports = router;
