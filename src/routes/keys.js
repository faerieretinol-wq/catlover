const express = require('express');
const { getDb } = require('../db');
const router = express.Router();

// Upload public key
router.post('/identity', async (req, res) => {
  const userId = req.user.userId;
  const { publicKey } = req.body;
  const db = getDb();
  try {
    await db.query(
      'INSERT INTO user_public_keys (user_id, public_key) VALUES ($1, $2) ON CONFLICT (user_id) DO UPDATE SET public_key = $2',
      [userId, publicKey]
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to upload key' });
  }
});

// Get user's public key
router.get('/:userId', async (req, res) => {
  const { userId } = req.params;
  const db = getDb();
  try {
    const result = await db.query('SELECT public_key FROM user_public_keys WHERE user_id = $1', [userId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Key not found' });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch key' });
  }
});

module.exports = router;
