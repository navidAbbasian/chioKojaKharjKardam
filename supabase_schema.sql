-- =============================================================
-- ChioKojaKharjKardam — Supabase Schema
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor)
-- Safe to re-run: all statements are idempotent.
-- =============================================================

-- ---------------------------------------------------------------
-- DROP EXISTING POLICIES (so re-runs don't fail on duplicates)
-- ---------------------------------------------------------------
DO $$ BEGIN
  -- families
  DROP POLICY IF EXISTS "family_select"       ON public.families;
  DROP POLICY IF EXISTS "family_insert"       ON public.families;
  DROP POLICY IF EXISTS "family_update"       ON public.families;
  -- profiles
  DROP POLICY IF EXISTS "profiles_select"     ON public.profiles;
  DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
  DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
  DROP POLICY IF EXISTS "owner_remove_member" ON public.profiles;
  -- bank_cards
  DROP POLICY IF EXISTS "bc_sel" ON public.bank_cards;
  DROP POLICY IF EXISTS "bc_ins" ON public.bank_cards;
  DROP POLICY IF EXISTS "bc_upd" ON public.bank_cards;
  DROP POLICY IF EXISTS "bc_del" ON public.bank_cards;
  -- categories
  DROP POLICY IF EXISTS "cat_sel" ON public.categories;
  DROP POLICY IF EXISTS "cat_ins" ON public.categories;
  DROP POLICY IF EXISTS "cat_upd" ON public.categories;
  DROP POLICY IF EXISTS "cat_del" ON public.categories;
  -- tags
  DROP POLICY IF EXISTS "tag_sel" ON public.tags;
  DROP POLICY IF EXISTS "tag_ins" ON public.tags;
  DROP POLICY IF EXISTS "tag_upd" ON public.tags;
  DROP POLICY IF EXISTS "tag_del" ON public.tags;
  -- transactions
  DROP POLICY IF EXISTS "tx_sel" ON public.transactions;
  DROP POLICY IF EXISTS "tx_ins" ON public.transactions;
  DROP POLICY IF EXISTS "tx_upd" ON public.transactions;
  DROP POLICY IF EXISTS "tx_del" ON public.transactions;
  -- transaction_tags
  DROP POLICY IF EXISTS "tt_sel" ON public.transaction_tags;
  DROP POLICY IF EXISTS "tt_ins" ON public.transaction_tags;
  DROP POLICY IF EXISTS "tt_del" ON public.transaction_tags;
  -- bills
  DROP POLICY IF EXISTS "bill_sel" ON public.bills;
  DROP POLICY IF EXISTS "bill_ins" ON public.bills;
  DROP POLICY IF EXISTS "bill_upd" ON public.bills;
  DROP POLICY IF EXISTS "bill_del" ON public.bills;
  -- transfers
  DROP POLICY IF EXISTS "tr_sel" ON public.transfers;
  DROP POLICY IF EXISTS "tr_ins" ON public.transfers;
  DROP POLICY IF EXISTS "tr_upd" ON public.transfers;
  DROP POLICY IF EXISTS "tr_del" ON public.transfers;
EXCEPTION WHEN others THEN NULL;
END $$;

-- ---------------------------------------------------------------
-- TABLES
-- ---------------------------------------------------------------

-- Families (created first — profiles reference it)
CREATE TABLE IF NOT EXISTS public.families (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         TEXT NOT NULL,
    invite_code  CHAR(6) UNIQUE NOT NULL,
    created_by   UUID REFERENCES auth.users (id),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
-- Ensure created_by exists (in case table was created from an older/partial schema run)
ALTER TABLE public.families ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users (id);

-- Profiles (1-to-1 with auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id           UUID PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    full_name    TEXT NOT NULL,
    email        TEXT NOT NULL,
    family_id    UUID REFERENCES public.families (id) ON DELETE SET NULL,
    is_owner     BOOLEAN DEFAULT FALSE,
    avatar_color TEXT DEFAULT '#3D7A5F',
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS family_id    UUID REFERENCES public.families (id) ON DELETE SET NULL;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_owner     BOOLEAN DEFAULT FALSE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_color TEXT DEFAULT '#3D7A5F';

-- Bank Cards
CREATE TABLE IF NOT EXISTS public.bank_cards (
    id               BIGSERIAL PRIMARY KEY,
    family_id        UUID    NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    member_id        UUID    NOT NULL REFERENCES auth.users (id),
    bank_name        TEXT,
    card_number      TEXT,
    card_holder_name TEXT    NOT NULL,
    balance          BIGINT  DEFAULT 0,
    initial_balance  BIGINT  DEFAULT 0,
    color            TEXT    DEFAULT '#DC2626',
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Categories
CREATE TABLE IF NOT EXISTS public.categories (
    id        BIGSERIAL PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    name      TEXT NOT NULL,
    icon      TEXT,
    color     TEXT,
    type      INT  DEFAULT 2,   -- 0=expense 1=income 2=both
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tags
CREATE TABLE IF NOT EXISTS public.tags (
    id        BIGSERIAL PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    name      TEXT NOT NULL,
    color     TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Transactions
CREATE TABLE IF NOT EXISTS public.transactions (
    id          BIGSERIAL PRIMARY KEY,
    family_id   UUID   NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    card_id     BIGINT REFERENCES public.bank_cards (id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES public.categories (id) ON DELETE SET NULL,
    amount      BIGINT NOT NULL,
    type        INT    NOT NULL,  -- 0=expense 1=income 2=transfer
    to_card_id  BIGINT REFERENCES public.bank_cards (id),
    description TEXT,
    date        BIGINT NOT NULL,  -- Unix ms
    created_at  BIGINT NOT NULL,
    created_by  UUID   REFERENCES auth.users (id)
);
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS to_card_id BIGINT REFERENCES public.bank_cards (id);
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users (id);

-- Transaction ↔ Tag junction
CREATE TABLE IF NOT EXISTS public.transaction_tags (
    transaction_id BIGINT NOT NULL REFERENCES public.transactions (id) ON DELETE CASCADE,
    tag_id         BIGINT NOT NULL REFERENCES public.tags (id) ON DELETE CASCADE,
    PRIMARY KEY (transaction_id, tag_id)
);

-- Bills
CREATE TABLE IF NOT EXISTS public.bills (
    id             BIGSERIAL PRIMARY KEY,
    family_id      UUID    NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    title          TEXT    NOT NULL,
    amount         BIGINT  NOT NULL,
    due_date       BIGINT  NOT NULL,
    is_recurring   BOOLEAN DEFAULT FALSE,
    recurring_type INT     DEFAULT 0,  -- 0=none 1=monthly 2=yearly
    is_paid        BOOLEAN DEFAULT FALSE,
    card_id        BIGINT  REFERENCES public.bank_cards (id) ON DELETE SET NULL,
    notify_before  INT     DEFAULT 0,
    created_at     BIGINT  NOT NULL,
    created_by     UUID    REFERENCES auth.users (id)
);
ALTER TABLE public.bills ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users (id);

-- Transfers (card-to-card)
CREATE TABLE IF NOT EXISTS public.transfers (
    id           BIGSERIAL PRIMARY KEY,
    family_id    UUID   NOT NULL REFERENCES public.families (id) ON DELETE CASCADE,
    from_card_id BIGINT REFERENCES public.bank_cards (id) ON DELETE SET NULL,
    to_card_id   BIGINT REFERENCES public.bank_cards (id) ON DELETE SET NULL,
    amount       BIGINT NOT NULL,
    description  TEXT,
    date         BIGINT NOT NULL,
    created_at   BIGINT NOT NULL,
    created_by   UUID   REFERENCES auth.users (id)
);
ALTER TABLE public.transfers ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users (id);

-- ---------------------------------------------------------------
-- ROW LEVEL SECURITY
-- ---------------------------------------------------------------

ALTER TABLE public.families        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bank_cards      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tags            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transaction_tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bills           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transfers       ENABLE ROW LEVEL SECURITY;

-- Helper: returns current user's family_id
CREATE OR REPLACE FUNCTION public.my_family_id()
RETURNS UUID LANGUAGE SQL SECURITY DEFINER STABLE AS $$
    SELECT family_id FROM public.profiles WHERE id = auth.uid();
$$;

-- Helper: returns true if current user is family owner
CREATE OR REPLACE FUNCTION public.i_am_owner()
RETURNS BOOLEAN LANGUAGE SQL SECURITY DEFINER STABLE AS $$
    SELECT COALESCE(is_owner, FALSE) FROM public.profiles WHERE id = auth.uid();
$$;

-- RPC: look up a family by invite_code (bypasses RLS — safe because invite_code is the shared secret)
-- Called by Android JOIN flow: POST /rest/v1/rpc/lookup_family_by_invite_code
CREATE OR REPLACE FUNCTION public.lookup_family_by_invite_code(p_code TEXT)
RETURNS TABLE(id UUID, name TEXT, invite_code CHAR(6), created_by UUID)
LANGUAGE SQL SECURITY DEFINER STABLE AS $$
    SELECT id, name, invite_code, created_by
    FROM public.families
    WHERE families.invite_code = p_code;
$$;

-- ---------------------------------------------------------------
-- POLICIES
-- ---------------------------------------------------------------

-- Families
-- SELECT: member of the family  OR  the user who just created it
--   (the "OR created_by" branch covers the moment right after INSERT,
--    before the profile.family_id has been updated)
CREATE POLICY "family_select" ON public.families FOR SELECT
    USING (id = public.my_family_id() OR created_by = auth.uid());
-- INSERT: authenticated user sets themselves as creator
CREATE POLICY "family_insert" ON public.families FOR INSERT
    WITH CHECK (auth.role() = 'authenticated' AND created_by = auth.uid());
-- UPDATE: only the original creator
CREATE POLICY "family_update" ON public.families FOR UPDATE USING (created_by = auth.uid());

-- Profiles
CREATE POLICY "profiles_select" ON public.profiles FOR SELECT
    USING (family_id = public.my_family_id() OR id = auth.uid());
CREATE POLICY "profiles_insert_own" ON public.profiles FOR INSERT WITH CHECK (id = auth.uid());
CREATE POLICY "profiles_update_own" ON public.profiles FOR UPDATE USING (id = auth.uid());
-- Owner can kick members (set their family_id to null)
CREATE POLICY "owner_remove_member" ON public.profiles FOR UPDATE
    USING (public.i_am_owner() AND family_id = public.my_family_id());

-- Macro for family-scoped tables
-- bank_cards
CREATE POLICY "bc_sel" ON public.bank_cards FOR SELECT USING (family_id = public.my_family_id());
CREATE POLICY "bc_ins" ON public.bank_cards FOR INSERT WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "bc_upd" ON public.bank_cards FOR UPDATE USING (family_id = public.my_family_id());
CREATE POLICY "bc_del" ON public.bank_cards FOR DELETE USING (family_id = public.my_family_id());

-- categories
CREATE POLICY "cat_sel" ON public.categories FOR SELECT USING (family_id = public.my_family_id());
CREATE POLICY "cat_ins" ON public.categories FOR INSERT WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "cat_upd" ON public.categories FOR UPDATE USING (family_id = public.my_family_id());
CREATE POLICY "cat_del" ON public.categories FOR DELETE USING (family_id = public.my_family_id());

-- tags
CREATE POLICY "tag_sel" ON public.tags FOR SELECT USING (family_id = public.my_family_id());
CREATE POLICY "tag_ins" ON public.tags FOR INSERT WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "tag_upd" ON public.tags FOR UPDATE USING (family_id = public.my_family_id());
CREATE POLICY "tag_del" ON public.tags FOR DELETE USING (family_id = public.my_family_id());

-- transactions
CREATE POLICY "tx_sel" ON public.transactions FOR SELECT USING (family_id = public.my_family_id());
CREATE POLICY "tx_ins" ON public.transactions FOR INSERT WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "tx_upd" ON public.transactions FOR UPDATE USING (family_id = public.my_family_id());
CREATE POLICY "tx_del" ON public.transactions FOR DELETE USING (family_id = public.my_family_id());

-- transaction_tags (via parent transaction)
CREATE POLICY "tt_sel" ON public.transaction_tags FOR SELECT
    USING (EXISTS (SELECT 1 FROM public.transactions t WHERE t.id = transaction_id AND t.family_id = public.my_family_id()));
CREATE POLICY "tt_ins" ON public.transaction_tags FOR INSERT
    WITH CHECK (EXISTS (SELECT 1 FROM public.transactions t WHERE t.id = transaction_id AND t.family_id = public.my_family_id()));
CREATE POLICY "tt_del" ON public.transaction_tags FOR DELETE
    USING (EXISTS (SELECT 1 FROM public.transactions t WHERE t.id = transaction_id AND t.family_id = public.my_family_id()));

-- bills
CREATE POLICY "bill_sel" ON public.bills FOR SELECT USING (family_id = public.my_family_id());
CREATE POLICY "bill_ins" ON public.bills FOR INSERT WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "bill_upd" ON public.bills FOR UPDATE USING (family_id = public.my_family_id());
CREATE POLICY "bill_del" ON public.bills FOR DELETE USING (family_id = public.my_family_id());

-- transfers
CREATE POLICY "tr_sel" ON public.transfers FOR SELECT USING (family_id = public.my_family_id());
CREATE POLICY "tr_ins" ON public.transfers FOR INSERT WITH CHECK (family_id = public.my_family_id());
CREATE POLICY "tr_upd" ON public.transfers FOR UPDATE USING (family_id = public.my_family_id());
CREATE POLICY "tr_del" ON public.transfers FOR DELETE USING (family_id = public.my_family_id());

-- ---------------------------------------------------------------
-- TRIGGER: auto-create profile row when user signs up
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name, email)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data ->> 'full_name', ''),
        NEW.email
    );
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ---------------------------------------------------------------
-- REALTIME: enable for all family-scoped tables
-- ---------------------------------------------------------------
ALTER PUBLICATION supabase_realtime ADD TABLE public.bank_cards;
ALTER PUBLICATION supabase_realtime ADD TABLE public.categories;
ALTER PUBLICATION supabase_realtime ADD TABLE public.tags;
ALTER PUBLICATION supabase_realtime ADD TABLE public.transactions;
ALTER PUBLICATION supabase_realtime ADD TABLE public.transaction_tags;
ALTER PUBLICATION supabase_realtime ADD TABLE public.bills;
ALTER PUBLICATION supabase_realtime ADD TABLE public.transfers;
ALTER PUBLICATION supabase_realtime ADD TABLE public.profiles;

