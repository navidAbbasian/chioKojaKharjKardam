-- =============================================================
-- ChioKojaKharjKardam — Supabase Schema Migration v2
-- Run this in Supabase SQL Editor AFTER supabase_schema.sql
--
-- Changes:
-- 1. bank_cards: UNIQUE(family_id, card_number)
-- 2. categories: UNIQUE(family_id, name) + owner-only INSERT policy
-- 3. tags: UNIQUE(family_id, name)
-- 4. transactions: id BIGSERIAL → UUID
-- 5. Add updated_at column to key tables
-- =============================================================

-- ---------------------------------------------------------------
-- 1. bank_cards — full 16-digit card_number, unique per family
-- ---------------------------------------------------------------
ALTER TABLE public.bank_cards
    DROP CONSTRAINT IF EXISTS bank_cards_family_card_number_unique;

ALTER TABLE public.bank_cards
    ADD CONSTRAINT bank_cards_family_card_number_unique
    UNIQUE (family_id, card_number);

-- Add updated_at
ALTER TABLE public.bank_cards
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ---------------------------------------------------------------
-- 2. categories — unique name per family + owner-only insert
-- ---------------------------------------------------------------
ALTER TABLE public.categories
    DROP CONSTRAINT IF EXISTS categories_family_name_unique;

ALTER TABLE public.categories
    ADD CONSTRAINT categories_family_name_unique
    UNIQUE (family_id, name);

-- Add updated_at
ALTER TABLE public.categories
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- Replace INSERT policy: only family owner can create categories
DROP POLICY IF EXISTS "cat_ins" ON public.categories;
CREATE POLICY "cat_ins" ON public.categories FOR INSERT
    WITH CHECK (family_id = public.my_family_id() AND public.i_am_owner());

-- Also restrict UPDATE and DELETE to owner
DROP POLICY IF EXISTS "cat_upd" ON public.categories;
CREATE POLICY "cat_upd" ON public.categories FOR UPDATE
    USING (family_id = public.my_family_id() AND public.i_am_owner());

DROP POLICY IF EXISTS "cat_del" ON public.categories;
CREATE POLICY "cat_del" ON public.categories FOR DELETE
    USING (family_id = public.my_family_id() AND public.i_am_owner());

-- ---------------------------------------------------------------
-- 3. tags — unique name per family
-- ---------------------------------------------------------------
ALTER TABLE public.tags
    DROP CONSTRAINT IF EXISTS tags_family_name_unique;

ALTER TABLE public.tags
    ADD CONSTRAINT tags_family_name_unique
    UNIQUE (family_id, name);

-- Add updated_at
ALTER TABLE public.tags
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ---------------------------------------------------------------
-- 4. transactions — migrate id from BIGSERIAL to UUID
-- ---------------------------------------------------------------

-- Step 4a: Add a UUID column to existing transactions table
ALTER TABLE public.transactions
    ADD COLUMN IF NOT EXISTS uuid_id UUID DEFAULT gen_random_uuid();

-- Populate UUID for existing rows
UPDATE public.transactions SET uuid_id = gen_random_uuid() WHERE uuid_id IS NULL;

-- Step 4b: Add uuid_id column to transaction_tags referencing the new UUID
-- We need to rebuild transaction_tags to reference UUID instead of BIGINT

-- Drop old transaction_tags
DROP TABLE IF EXISTS public.transaction_tags CASCADE;

-- Step 4c: Rename old transactions, create new UUID-keyed table
ALTER TABLE public.transactions RENAME TO transactions_old;

-- Drop policies on old table (they reference the old table name)
DROP POLICY IF EXISTS "tx_sel" ON public.transactions_old;
DROP POLICY IF EXISTS "tx_ins" ON public.transactions_old;
DROP POLICY IF EXISTS "tx_upd" ON public.transactions_old;
DROP POLICY IF EXISTS "tx_del" ON public.transactions_old;

-- Create new transactions table with UUID PK
CREATE TABLE public.transactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id   UUID   NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    card_id     BIGINT REFERENCES public.bank_cards (id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES public.categories (id) ON DELETE SET NULL,
    amount      BIGINT NOT NULL,
    type        INT    NOT NULL,  -- 0=expense 1=income 2=transfer
    to_card_id  BIGINT REFERENCES public.bank_cards (id),
    description TEXT,
    date        BIGINT NOT NULL,  -- Unix ms
    created_at  BIGINT NOT NULL,
    created_by  UUID   REFERENCES auth.users (id),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Migrate data: use uuid_id from old table as new id
INSERT INTO public.transactions (id, family_id, card_id, category_id, amount, type,
                                  to_card_id, description, date, created_at, created_by)
SELECT uuid_id, family_id, card_id, category_id, amount, type,
       to_card_id, description, date, created_at, created_by
FROM public.transactions_old;

-- Recreate transaction_tags with UUID FK
CREATE TABLE public.transaction_tags (
    transaction_id UUID NOT NULL REFERENCES public.transactions (id) ON DELETE CASCADE,
    tag_id         BIGINT NOT NULL REFERENCES public.tags (id) ON DELETE CASCADE,
    PRIMARY KEY (transaction_id, tag_id)
);

-- Drop old table
DROP TABLE public.transactions_old CASCADE;

-- ---------------------------------------------------------------
-- Re-enable RLS and policies for transactions + transaction_tags
-- ---------------------------------------------------------------
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transaction_tags ENABLE ROW LEVEL SECURITY;

CREATE POLICY "tx_sel" ON public.transactions FOR SELECT
    USING (family_id = public.my_family_id());
CREATE POLICY "tx_ins" ON public.transactions FOR INSERT
    WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "tx_upd" ON public.transactions FOR UPDATE
    USING (family_id = public.my_family_id());
CREATE POLICY "tx_del" ON public.transactions FOR DELETE
    USING (family_id = public.my_family_id());

CREATE POLICY "tt_sel" ON public.transaction_tags FOR SELECT
    USING (EXISTS (SELECT 1 FROM public.transactions t WHERE t.id = transaction_id AND t.family_id = public.my_family_id()));
CREATE POLICY "tt_ins" ON public.transaction_tags FOR INSERT
    WITH CHECK (EXISTS (SELECT 1 FROM public.transactions t WHERE t.id = transaction_id AND t.family_id = public.my_family_id()));
CREATE POLICY "tt_del" ON public.transaction_tags FOR DELETE
    USING (EXISTS (SELECT 1 FROM public.transactions t WHERE t.id = transaction_id AND t.family_id = public.my_family_id()));

-- ---------------------------------------------------------------
-- Create indexes for performance
-- ---------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_family_id ON public.transactions (family_id);
CREATE INDEX IF NOT EXISTS idx_transactions_card_id ON public.transactions (card_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON public.transactions (date);
CREATE INDEX IF NOT EXISTS idx_bank_cards_family_id ON public.bank_cards (family_id);
CREATE INDEX IF NOT EXISTS idx_categories_family_id ON public.categories (family_id);
CREATE INDEX IF NOT EXISTS idx_tags_family_id ON public.tags (family_id);

-- ---------------------------------------------------------------
-- Auto-update updated_at trigger
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS set_updated_at ON public.bank_cards;
CREATE TRIGGER set_updated_at BEFORE UPDATE ON public.bank_cards
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

DROP TRIGGER IF EXISTS set_updated_at ON public.categories;
CREATE TRIGGER set_updated_at BEFORE UPDATE ON public.categories
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

DROP TRIGGER IF EXISTS set_updated_at ON public.tags;
CREATE TRIGGER set_updated_at BEFORE UPDATE ON public.tags
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

DROP TRIGGER IF EXISTS set_updated_at ON public.transactions;
CREATE TRIGGER set_updated_at BEFORE UPDATE ON public.transactions
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

