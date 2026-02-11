const { Pool } = require('pg');
const dns = require('node:dns');
require('dotenv').config();

let pool;

async function ensurePool() {
  if (pool) return pool;

  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL is missing');
  }

  console.log('🔌 Connection attempt to DB...');

  pool = new Pool({
    connectionString: connectionString,
    ssl: {
      rejectUnauthorized: false
    }
  });

  return pool;
}

async function dbInit() {
  const p = await ensurePool();
  const client = await p.connect();
  try {
    console.log('🚀 Initializing PostgreSQL Schema...');
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        email TEXT UNIQUE NOT NULL,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS user_profiles (
        user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
        avatar_url TEXT,
        bio TEXT,
        status TEXT,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS chats (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        title TEXT,
        is_group BOOLEAN DEFAULT false,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS chat_members (
        chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
        user_id UUID REFERENCES users(id) ON DELETE CASCADE,
        joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (chat_id, user_id)
      );

      CREATE TABLE IF NOT EXISTS messages (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
        sender_id UUID REFERENCES users(id) ON DELETE CASCADE,
        body TEXT NOT NULL,
        encrypted_key TEXT,
        iv TEXT,
        attachment_url TEXT,
        is_read BOOLEAN DEFAULT false,
        isVideoCircle BOOLEAN DEFAULT false,
        expires_at TIMESTAMP WITH TIME ZONE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS user_public_keys (
        user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
        public_key TEXT NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS stories (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID REFERENCES users(id) ON DELETE CASCADE,
        image_url TEXT NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS follows (
        follower_id UUID REFERENCES users(id) ON DELETE CASCADE,
        following_id UUID REFERENCES users(id) ON DELETE CASCADE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (follower_id, following_id)
      );

      CREATE TABLE IF NOT EXISTS user_2fa (
        user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
        secret TEXT NOT NULL,
        enabled BOOLEAN DEFAULT false,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );

      -- Enable Row Level Security
      ALTER TABLE users ENABLE ROW LEVEL SECURITY;
      ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
      ALTER TABLE chats ENABLE ROW LEVEL SECURITY;
      ALTER TABLE chat_members ENABLE ROW LEVEL SECURITY;
      ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
      ALTER TABLE user_public_keys ENABLE ROW LEVEL SECURITY;
      ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
      ALTER TABLE follows ENABLE ROW LEVEL SECURITY;
      ALTER TABLE user_2fa ENABLE ROW LEVEL SECURITY;

      -- RLS Policies for users table
      DROP POLICY IF EXISTS users_select_policy ON users;
      CREATE POLICY users_select_policy ON users FOR SELECT USING (true);
      
      DROP POLICY IF EXISTS users_insert_policy ON users;
      CREATE POLICY users_insert_policy ON users FOR INSERT WITH CHECK (true);
      
      DROP POLICY IF EXISTS users_update_policy ON users;
      CREATE POLICY users_update_policy ON users FOR UPDATE USING (id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS users_delete_policy ON users;
      CREATE POLICY users_delete_policy ON users FOR DELETE USING (id = current_setting('app.current_user_id', true)::uuid);

      -- RLS Policies for user_profiles
      DROP POLICY IF EXISTS profiles_select_policy ON user_profiles;
      CREATE POLICY profiles_select_policy ON user_profiles FOR SELECT USING (true);
      
      DROP POLICY IF EXISTS profiles_insert_policy ON user_profiles;
      CREATE POLICY profiles_insert_policy ON user_profiles FOR INSERT WITH CHECK (true);
      
      DROP POLICY IF EXISTS profiles_update_policy ON user_profiles;
      CREATE POLICY profiles_update_policy ON user_profiles FOR UPDATE USING (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS profiles_delete_policy ON user_profiles;
      CREATE POLICY profiles_delete_policy ON user_profiles FOR DELETE USING (user_id = current_setting('app.current_user_id', true)::uuid);

      -- RLS Policies for messages
      DROP POLICY IF EXISTS messages_select_policy ON messages;
      CREATE POLICY messages_select_policy ON messages FOR SELECT USING (
        sender_id = current_setting('app.current_user_id', true)::uuid OR
        chat_id IN (SELECT chat_id FROM chat_members WHERE user_id = current_setting('app.current_user_id', true)::uuid)
      );
      
      DROP POLICY IF EXISTS messages_insert_policy ON messages;
      CREATE POLICY messages_insert_policy ON messages FOR INSERT WITH CHECK (
        sender_id = current_setting('app.current_user_id', true)::uuid AND
        chat_id IN (SELECT chat_id FROM chat_members WHERE user_id = current_setting('app.current_user_id', true)::uuid)
      );
      
      DROP POLICY IF EXISTS messages_update_policy ON messages;
      CREATE POLICY messages_update_policy ON messages FOR UPDATE USING (sender_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS messages_delete_policy ON messages;
      CREATE POLICY messages_delete_policy ON messages FOR DELETE USING (sender_id = current_setting('app.current_user_id', true)::uuid);

      -- RLS Policies for chat_members
      DROP POLICY IF EXISTS chat_members_select_policy ON chat_members;
      CREATE POLICY chat_members_select_policy ON chat_members FOR SELECT USING (
        user_id = current_setting('app.current_user_id', true)::uuid OR
        chat_id IN (SELECT chat_id FROM chat_members WHERE user_id = current_setting('app.current_user_id', true)::uuid)
      );
      
      DROP POLICY IF EXISTS chat_members_insert_policy ON chat_members;
      CREATE POLICY chat_members_insert_policy ON chat_members FOR INSERT WITH CHECK (true);
      
      DROP POLICY IF EXISTS chat_members_delete_policy ON chat_members;
      CREATE POLICY chat_members_delete_policy ON chat_members FOR DELETE USING (
        user_id = current_setting('app.current_user_id', true)::uuid
      );

      -- RLS Policies for user_2fa
      DROP POLICY IF EXISTS user_2fa_select_policy ON user_2fa;
      CREATE POLICY user_2fa_select_policy ON user_2fa FOR SELECT USING (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS user_2fa_insert_policy ON user_2fa;
      CREATE POLICY user_2fa_insert_policy ON user_2fa FOR INSERT WITH CHECK (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS user_2fa_update_policy ON user_2fa;
      CREATE POLICY user_2fa_update_policy ON user_2fa FOR UPDATE USING (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS user_2fa_delete_policy ON user_2fa;
      CREATE POLICY user_2fa_delete_policy ON user_2fa FOR DELETE USING (user_id = current_setting('app.current_user_id', true)::uuid);

      -- RLS Policies for chats
      DROP POLICY IF EXISTS chats_select_policy ON chats;
      CREATE POLICY chats_select_policy ON chats FOR SELECT USING (
        id IN (SELECT chat_id FROM chat_members WHERE user_id = current_setting('app.current_user_id', true)::uuid)
      );
      
      DROP POLICY IF EXISTS chats_insert_policy ON chats;
      CREATE POLICY chats_insert_policy ON chats FOR INSERT WITH CHECK (true);
      
      DROP POLICY IF EXISTS chats_update_policy ON chats;
      CREATE POLICY chats_update_policy ON chats FOR UPDATE USING (
        id IN (SELECT chat_id FROM chat_members WHERE user_id = current_setting('app.current_user_id', true)::uuid)
      );
      
      DROP POLICY IF EXISTS chats_delete_policy ON chats;
      CREATE POLICY chats_delete_policy ON chats FOR DELETE USING (
        id IN (SELECT chat_id FROM chat_members WHERE user_id = current_setting('app.current_user_id', true)::uuid)
      );

      -- RLS Policies for user_public_keys
      DROP POLICY IF EXISTS public_keys_select_policy ON user_public_keys;
      CREATE POLICY public_keys_select_policy ON user_public_keys FOR SELECT USING (true);
      
      DROP POLICY IF EXISTS public_keys_insert_policy ON user_public_keys;
      CREATE POLICY public_keys_insert_policy ON user_public_keys FOR INSERT WITH CHECK (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS public_keys_update_policy ON user_public_keys;
      CREATE POLICY public_keys_update_policy ON user_public_keys FOR UPDATE USING (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS public_keys_delete_policy ON user_public_keys;
      CREATE POLICY public_keys_delete_policy ON user_public_keys FOR DELETE USING (user_id = current_setting('app.current_user_id', true)::uuid);

      -- RLS Policies for stories
      DROP POLICY IF EXISTS stories_select_policy ON stories;
      CREATE POLICY stories_select_policy ON stories FOR SELECT USING (
        user_id = current_setting('app.current_user_id', true)::uuid OR
        user_id IN (SELECT following_id FROM follows WHERE follower_id = current_setting('app.current_user_id', true)::uuid)
      );
      
      DROP POLICY IF EXISTS stories_insert_policy ON stories;
      CREATE POLICY stories_insert_policy ON stories FOR INSERT WITH CHECK (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS stories_update_policy ON stories;
      CREATE POLICY stories_update_policy ON stories FOR UPDATE USING (user_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS stories_delete_policy ON stories;
      CREATE POLICY stories_delete_policy ON stories FOR DELETE USING (user_id = current_setting('app.current_user_id', true)::uuid);

      -- RLS Policies for follows
      DROP POLICY IF EXISTS follows_select_policy ON follows;
      CREATE POLICY follows_select_policy ON follows FOR SELECT USING (
        follower_id = current_setting('app.current_user_id', true)::uuid OR
        following_id = current_setting('app.current_user_id', true)::uuid
      );
      
      DROP POLICY IF EXISTS follows_insert_policy ON follows;
      CREATE POLICY follows_insert_policy ON follows FOR INSERT WITH CHECK (follower_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS follows_update_policy ON follows;
      CREATE POLICY follows_update_policy ON follows FOR UPDATE USING (follower_id = current_setting('app.current_user_id', true)::uuid);
      
      DROP POLICY IF EXISTS follows_delete_policy ON follows;
      CREATE POLICY follows_delete_policy ON follows FOR DELETE USING (follower_id = current_setting('app.current_user_id', true)::uuid);
    `);
    console.log('✅ Schema initialized with RLS enabled');
  } finally {
    client.release();
  }
}

const getDb = () => {
  if (!pool) {
    throw new Error('DB pool is not initialized yet. Call dbInit() first.');
  }
  return pool;
};

module.exports = { dbInit, getDb };
