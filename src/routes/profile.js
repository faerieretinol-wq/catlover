const express = require('express');
const { getDb } = require('../db');
const router = express.Router();

// Get own profile
router.get('/me', async (req, res) => {
  const userId = req.user.userId;
  const db = getDb();
  try {
    const result = await db.query(
      `SELECT u.username, u.email, p.avatar_url, p.bio, p.status 
       FROM users u 
       JOIN user_profiles p ON u.id = p.user_id 
       WHERE u.id = $1`,
      [userId]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch profile' });
  }
});

// Update profile
router.post('/update', async (req, res) => {
  const userId = req.user.userId;
  const { bio, status, avatarUrl } = req.body;
  const db = getDb();
  try {
    await db.query(
      `UPDATE user_profiles 
       SET bio = COALESCE($1, bio), 
           status = COALESCE($2, status), 
           avatar_url = COALESCE($3, avatar_url),
           updated_at = CURRENT_TIMESTAMP
       WHERE user_id = $4`,
      [bio, status, avatarUrl, userId]
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

// Search users (profiles)
router.get('/search', async (req, res) => {
  const { q } = req.query;
  const db = getDb();
  try {
    const result = await db.query(
      `SELECT u.id, u.username, p.avatar_url, p.status 
       FROM users u 
       JOIN user_profiles p ON u.id = p.user_id 
       WHERE u.username ILIKE $1 
       LIMIT 20`,
      [`%${q}%`]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'Search failed' });
  }
});

module.exports = router;
