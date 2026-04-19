-- Migration v3: Add initial_balance to bank_cards for balance recalculation
-- Run this on your Supabase SQL editor

ALTER TABLE public.bank_cards ADD COLUMN IF NOT EXISTS initial_balance BIGINT DEFAULT 0;

-- Set initial_balance = balance for existing cards (approximation)
UPDATE public.bank_cards SET initial_balance = balance WHERE initial_balance = 0;

